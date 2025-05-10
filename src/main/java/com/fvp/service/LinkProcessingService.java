package com.fvp.service;

import com.fvp.config.ElasticsearchSyncConfig;
import com.fvp.document.LinkDocument;
import com.fvp.entity.AllCat;
import com.fvp.entity.BaseLinkCategory;
import com.fvp.entity.BaseLinkModel;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.entity.LinkModel;
import com.fvp.entity.Model;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.LinkRepository;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LinkProcessingService {

  private static final Logger logger = LoggerFactory.getLogger(LinkProcessingService.class);
  private static final Pattern WORD_PATTERN = Pattern.compile("\\w+");
  private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\s,.-]+");
  private final Random random = new Random();

  private final DataSource dataSource;
  private final LinkRepository linkRepository;
  private final AllCatRepository allCatRepository;
  private final ElasticsearchClientService elasticsearchClientService;
  private final ElasticsearchSyncConfig elasticsearchSyncConfig;
  private final JdbcTemplate jdbcTemplate;
  private final LinkCategoryService linkCategoryService;
  private final LinkModelService linkModelService;
  private final LinkCategoryShardingService shardingService;
  private final LinkModelShardingService linkModelShardingService;
  private final ModelService modelService;
  // Cache for storing categories by tenant ID
  private final Map<Integer, List<AllCat>> categoriesCache = new ConcurrentHashMap<>();


  @Autowired
  public LinkProcessingService(
      DataSource dataSource,
      LinkRepository linkRepository,
      AllCatRepository allCatRepository,
      ElasticsearchClientService elasticsearchClientService,
      ElasticsearchSyncConfig elasticsearchSyncConfig,
      JdbcTemplate jdbcTemplate,
      LinkCategoryService linkCategoryService,
      LinkModelService linkModelService,
      ModelService modelService,
      LinkCategoryShardingService shardingService,
      LinkModelShardingService linkModelShardingService) {
    this.dataSource = dataSource;
    this.linkRepository = linkRepository;
    this.allCatRepository = allCatRepository;
    this.elasticsearchClientService = elasticsearchClientService;
    this.elasticsearchSyncConfig = elasticsearchSyncConfig;
    this.jdbcTemplate = jdbcTemplate;
    this.linkCategoryService = linkCategoryService;
    this.linkModelService = linkModelService;
    this.modelService = modelService;
    this.shardingService = shardingService;
    this.linkModelShardingService = linkModelShardingService;
  }

  @PostConstruct
  public void logJdbcUrl() throws SQLException {
    String jdbcUrl = dataSource.getConnection().getMetaData().getURL();
    System.out.println("JDBC URL: " + jdbcUrl);
  }

  public void processLink(Link link) {
    if (link == null) {
      logger.warn("Cannot process null link");
      return;
    }

    long startTimeMs = System.currentTimeMillis();
    long checkExistingTimeMs = 0;
    long saveLinkTimeMs = 0;
    long processCategoriesTimeMs = 0;
    long processModelsTimeMs = 0;
    boolean realTimeIndex = false;

    try {
      // Check if link with same URL already exists
      long checkStartMs = System.currentTimeMillis();
      Link existingLink = linkRepository.findByLinkAndTenantId(link.getLink(), link.getTenantId());
      checkExistingTimeMs = System.currentTimeMillis() - checkStartMs;

      long saveStartMs = System.currentTimeMillis();
      if (existingLink != null) {
        logger.info("Found existing link with URL '{}', updating instead of creating new",
            link.getLink());

        // Update existing link with new data
        existingLink.setTitle(link.getTitle());
        existingLink.setThumbnail(link.getThumbnail());
        existingLink.setDuration(link.getDuration());
        existingLink.setQuality(link.getQuality());
        existingLink.setThumbpath(link.getThumbpath());
        existingLink.setSheetName(link.getSheetName());
        existingLink.setSource(link.getSource());
        // Don't update createdOn to preserve original creation date

        // Save the updated link using merge
        logger.info("Updating link table: 1 entry with ID {} and title '{}'", existingLink.getId(),
            existingLink.getTitle());
        link = linkRepository.saveAndFlush(existingLink); // Use saveAndFlush instead of save

        // Update Elasticsearch document

        if (realTimeIndex) {
          updateElasticsearchDocument(link);
        }
      } else {
        // Save the new link
        logger.info("Saving to link table: 1 entry with title '{}'", link.getTitle());
        link = linkRepository.saveAndFlush(link); // Use saveAndFlush instead of save

        // Create Elasticsearch document
        if (realTimeIndex) {
          updateElasticsearchDocument(link);
        }
      }
      saveLinkTimeMs = System.currentTimeMillis() - saveStartMs;

      // Process categories if provided
      if (link.getCategory() != null && !link.getCategory().trim().isEmpty()) {
        long categoriesStartMs = System.currentTimeMillis();
        processCategories(link);
        processCategoriesTimeMs = System.currentTimeMillis() - categoriesStartMs;
      }

      // Process models if provided
      if (link.getStar() != null && !link.getStar().trim().isEmpty()) {
        long modelsStartMs = System.currentTimeMillis();
        processModels(link);
        processModelsTimeMs = System.currentTimeMillis() - modelsStartMs;
      }

      long totalTimeMs = System.currentTimeMillis() - startTimeMs;
      logger.info(
          "Link processing completed in {} ms (check: {} ms, save: {} ms, categories: {} ms, models: {} ms): {}",
          totalTimeMs, checkExistingTimeMs, saveLinkTimeMs, processCategoriesTimeMs,
          processModelsTimeMs, link.getTitle());

    } catch (Exception e) {
      long errorTimeMs = System.currentTimeMillis() - startTimeMs;
      logger.error("Error processing link after {} ms: {}", errorTimeMs, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Process categories for a link
   *
   * @param link The link entity
   */
  public void processCategories(Link link) {
    Set<String> categorySet = new HashSet<>();

    List<AllCat> allCategories = getCategoriesForTenant(link.getTenantId());
    List<String> values = tokenize(link.getCategory());

    // First pass: Collect all categories to be created
    for (String token : values) {
      String category = token.trim();
      if (!category.isEmpty()) {
        // Check if the token exactly matches any category
        for (AllCat allCat : allCategories) {
          if (allCat.getName().equalsIgnoreCase(category)) {
            categorySet.add(allCat.getName());
            break;
          }
        }
        // Try tokenizing the category name for partial matches
        String[] words = category.toLowerCase().split(" ");
        for (String singleToken : words) {
          if (!singleToken.trim().isEmpty()) {
            for (AllCat allCat : allCategories) {
              if (allCat.getName().trim().toLowerCase().equalsIgnoreCase(singleToken.trim())) {
                categorySet.add(allCat.getName());
                logger.info("Found substring match: '{}' equals '{}'", category, allCat.getName());
                break;
              }
            }
          }
        }
      }
    }

    // Create LinkCategory entries for all valid categories in a single batch
    if (!categorySet.isEmpty()) {
      logger.info("Saving to sharded link_category tables: {} entries for link ID {}",
          categorySet.size(), link.getId());

      List<BaseLinkCategory> shardEntities = new ArrayList<>();
      for (String categoryName : categorySet) {
        try {
          // Create base entity
          LinkCategory linkCategory = new LinkCategory();
          linkCategory.setTenantId(link.getTenantId());
          linkCategory.setLinkId(link.getId());
          linkCategory.setCategory(categoryName);
          linkCategory.setCreatedOn(link.getCreatedOn());
          linkCategory.setRandomOrder(link.getRandomOrder());

          // Convert to shard entity
          BaseLinkCategory shardEntity = shardingService.convertToShardEntity(linkCategory);
          shardEntities.add(shardEntity);
        } catch (Exception e) {
          logger.error("Error creating shard entity for category '{}' and link ID {}: {}",
              categoryName, link.getId(), e.getMessage(), e);
        }
      }

      // Save all shard entities in a single batch
      int savedCount = 0;
      for (BaseLinkCategory shardEntity : shardEntities) {
        try {
          shardingService.save(shardEntity);
          savedCount++;
        } catch (Exception e) {
          logger.error("Error saving shard entity for link ID {}: {}",
              link.getId(), e.getMessage(), e);
        }
      }

      logger.info("Saved {} entries to sharded link_category tables for link ID {}",
          savedCount, link.getId());
    }
  }

  /**
   * Process models for a link
   *
   * @param link The link entity
   */
  public void processModels(Link link) {
    Set<String> modelSet = new HashSet<>();

    List<String> values = tokenize(link.getStar());
    // For each token from the input
    for (String token : values) {
      String model = token.trim();
      if (!model.isEmpty()) {
        try {
          Map<String, Model> modelMap = modelService.getAllModels();
          modelSet.add(model);
        } catch (Exception e) {
          logger.debug("No exact match found for model: {}", model);
        }
      }
    }

    // Save all matched models
    int savedCount = 0;
    for (String modelName : modelSet) {
      try {
        // Create base entity
        LinkModel linkModel = new LinkModel();
        linkModel.setTenantId(link.getTenantId());
        linkModel.setLinkId(link.getId());
        linkModel.setModel(modelName);
        linkModel.setCreatedOn(link.getCreatedOn());
        linkModel.setRandomOrder(link.getRandomOrder().doubleValue());

        // Convert and save to appropriate shard
        BaseLinkModel shardEntity = linkModelShardingService.convertToShardEntity(linkModel);
        linkModelShardingService.save(shardEntity);
        savedCount++;
      } catch (Exception e) {
        logger.error("Error saving model '{}' for link ID {}: {}",
            modelName, link.getId(), e.getMessage(), e);
      }
    }

    logger.info("Saved {} entries to sharded link_model tables for link ID {}",
        savedCount, link.getId());
  }

  private List<String> tokenize(String input) {
    Gson gson = new Gson();
    Type listType = new TypeToken<List<String>>() {
    }.getType();
    List<String> values = gson.fromJson(input, listType);
    return values;
  }

  /**
   * Gets all categories for a tenant, using cache when available
   *
   * @param tenantId the tenant ID
   * @return List of categories for the tenant
   */
  public List<AllCat> getCategoriesForTenant(Integer tenantId) {
    return categoriesCache.computeIfAbsent(tenantId, id -> {
      logger.info("Cache miss for tenant {}, loading categories from database", id);
      return allCatRepository.findByTenantId(id);
    });
  }

  /**
   * Refreshes the categories cache every hour
   */
  @Scheduled(fixedRate = 3600000) // 1 hour
  public void refreshCategoriesCache() {
    logger.info("Refreshing categories cache");
    categoriesCache.clear();
    // Pre-warm cache for common tenant IDs if needed
  }

  /**
   * Updates an existing Elasticsearch document for a link
   */
  public void updateElasticsearchDocument(Link link) {
    if (!elasticsearchSyncConfig.isEnabled()) {
      logger.debug("Elasticsearch sync is disabled, skipping document update for link ID {}",
          link.getId());
      return;
    }

    try {
      // Find existing document

      List<LinkDocument> existingDocs = elasticsearchClientService.searchByTitleOrText(
          link.getLink(), null);
      LinkDocument doc = null;
      if (!existingDocs.isEmpty()) {
        logger.info("Existing link found for link {} with ID {}", link.getLink(),
            existingDocs.get(0).getId());
        doc = existingDocs.get(0);
      } else {
        doc = new LinkDocument();
        doc.setId(String.valueOf(link.getId()));
      }
      doc.setTitle(link.getTitle());
      doc.setLink(link.getLink());
      doc.setThumbnail(link.getThumbnail());
      doc.setDuration(link.getDuration());
      doc.setSearchableText(generateSearchableText(link));

      // Update categories
      List<String> categoriesList = tokenize(link.getCategory());
      if (categoriesList == null) {
        categoriesList = new ArrayList<>();
      }
      doc.setCategories(categoriesList);
      doc.setModels(tokenize(link.getStar()));
      logger.info("Updating Elasticsearch document for link ID {} with {} categories",
          link.getId(), categoriesList.size());
      elasticsearchClientService.saveLinkDocument(doc);
    } catch (Exception e) {
      logger.error("Error updating Elasticsearch document for link ID {}: {}", link.getId(),
          e.getMessage(), e);
    }
  }

  /**
   * Generates searchable text for a link
   */
  private String generateSearchableText(Link link) {
    StringBuilder searchableText = new StringBuilder();
    searchableText.append(link.getTitle());

    // Add categories to searchable text
    List<String> categories = tokenize(link.getCategory());
    List<String> models = tokenize(link.getStar());
    if (categories != null && !categories.isEmpty()) {
      for (String category : categories) {
        searchableText.append(" ").append(category);
      }
    }
    if (models != null && !models.isEmpty()) {

      for (String model : models) {
        searchableText.append(" ").append(model);
      }
    }

    return searchableText.toString().trim();
  }

}