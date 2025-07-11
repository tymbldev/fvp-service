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
import com.fvp.util.Util;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
  private final ModelService modelService;
  private final Util util;
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
      Util util) {
    this.dataSource = dataSource;
    this.linkRepository = linkRepository;
    this.allCatRepository = allCatRepository;
    this.elasticsearchClientService = elasticsearchClientService;
    this.elasticsearchSyncConfig = elasticsearchSyncConfig;
    this.jdbcTemplate = jdbcTemplate;
    this.linkCategoryService = linkCategoryService;
    this.linkModelService = linkModelService;
    this.modelService = modelService;
    this.util = util;
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

    try {
      // Check if link with same URL already exists
      long checkStartMs = System.currentTimeMillis();
      Link existingLink = linkRepository.findByLink(link.getLink());
      checkExistingTimeMs = System.currentTimeMillis() - checkStartMs;

      long saveStartMs = System.currentTimeMillis();
      if (existingLink != null) {
        logger.info("Found existing link with URL '{}', updating instead of creating new",
            link.getLink());

        // Update existing link with new data
        existingLink.setTitle(link.getTitle());
        existingLink.setTrailer(link.getTrailer());
        existingLink.setTrailerPresent(link.getTrailerPresent());
        existingLink.setThumbnail(link.getThumbnail());
        existingLink.setDuration(link.getDuration());
        existingLink.setQuality(link.getQuality());
        existingLink.setThumbpath(link.getThumbpath());
        existingLink.setSheetName(link.getSheetName());
        existingLink.setSource(link.getSource());
        existingLink.setHd(link.getHd());
        // Don't update createdOn to preserve original creation date

        // Save the updated link using merge
        logger.info("Updating link table: 1 entry with ID {} and title '{}'", existingLink.getId(),
            existingLink.getTitle());
        link = linkRepository.saveAndFlush(existingLink); // Use saveAndFlush instead of save

        // Update Elasticsearch document
        updateElasticsearchDocument(link);
      } else {
        // Save the new link
        logger.info("Saving to link table: 1 entry with title '{}'", link.getTitle());
        link = linkRepository.saveAndFlush(link); // Use saveAndFlush instead of save
        updateElasticsearchDocument(link);
      }
      saveLinkTimeMs = System.currentTimeMillis() - saveStartMs;

      long totalTimeMs = System.currentTimeMillis() - startTimeMs;
      logger.info(
          "Link processing completed in {} ms (check: {} ms, save: {} ms): {}",
          totalTimeMs, checkExistingTimeMs, saveLinkTimeMs, link.getTitle());
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

  private Set<String> getCategorySet(Link link) {
    Set<String> categorySet = new HashSet<>();
    Set<String> categorySetFinal = new HashSet<>();
    if (link == null || link.getCategory() == null || link.getCategory().isEmpty()) {
      return categorySet;
    }
    List<AllCat> allCategories = getCategoriesForTenant(link.getTenantId());
    List<String> values = util.tokenize(link.getCategory());

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
    for(String categorySetItem : categorySet) {
      if (!categorySetItem.isEmpty()) {
        categorySetFinal.add(categorySetItem.toLowerCase());
      }
    }
    return categorySetFinal;
  }

  public Set<String> getModelSet(Link link) {
    Set<String> modelSet = new HashSet<>();
    Set<String> modelSetFinal = new HashSet<>();
    if (link == null || link.getCategory() == null || link.getCategory().isEmpty()) {
      return modelSet;
    }
    List<String> values = util.tokenize(link.getStar());
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
    for(String modelSetEntry : modelSet) {
      if (!modelSetEntry.isEmpty()) {
        modelSetFinal.add(modelSetEntry.toLowerCase());
      }
    }
    return modelSetFinal;
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
//@Scheduled(fixedRate = 3600000) // 1 hour
  public void refreshCategoriesCache() {
    logger.info("Refreshing categories cache");
    categoriesCache.clear();
    // Pre-warm cache for common tenant IDs if needed
  }

  /**
   * Updates an existing Elasticsearch document for a link
   */
  public void updateElasticsearchDocument(Link link) {

    try {
      // Find existing document
      List<LinkDocument> existingDocs = elasticsearchClientService.searchByLinkId(
          String.valueOf(link.getId()), null);
      LinkDocument doc = null;
      if (!existingDocs.isEmpty()) {
        logger.info("Existing link found for link ID {} with document ID {}", link.getId(),
            existingDocs.get(0).getLinkId());
        doc = existingDocs.get(0);
      } else {
        doc = new LinkDocument();
        doc.setLinkId(String.valueOf(link.getId()));
      }

      // Update all fields from Link entity
      doc.setTenantId(link.getTenantId());
      doc.setLinkTitle(link.getTitle());
      doc.setLink(link.getLink());
      doc.setLinkThumbnail(link.getThumbnail());
      doc.setLinkThumbPath(link.getThumbpath());
      doc.setLinkDuration(link.getDuration());
      doc.setLinkSource(link.getSource());
      doc.setLinkTrailer(link.getTrailer());
      doc.setCreatedAt(link.getCreatedAt() != null ?
          java.util.Date.from(
              link.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()) : null);
      doc.setSearchableText(generateSearchableText(link));
      Set<String> categorySet = getCategorySet(link);
      if (categorySet != null) {
        doc.setCategories(categorySet.stream().collect(Collectors.toList()));
      }

      Set<String> modelSet = getModelSet(link);
      if (modelSet != null) {
        doc.setModels(modelSet.stream().collect(Collectors.toList()));
      }
      doc.setQuality(link.getQuality());
      doc.setSheetName(link.getSheetName());
      doc.setRandomOrder(link.getRandomOrder());
      doc.setThumbPathProcessed(link.getThumbPathProcessed());
      doc.setTrailerPresent(link.getTrailerPresent());
      doc.setHd(link.getHd());
      doc.setCreatedOn(link.getCreatedOn() != null ?
          java.util.Date.from(
              link.getCreatedOn().atZone(java.time.ZoneId.systemDefault()).toInstant()) : null);

      logger.info("Updating Elasticsearch document for link ID {} with {} categories",
          link.getId(), doc.getCategories().size());
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
    List<String> categories = util.tokenize(link.getCategory());
    List<String> models = util.tokenize(link.getStar());
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