package com.fvp.service;

import com.fvp.config.ElasticsearchSyncConfig;
import com.fvp.document.LinkDocument;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.entity.LinkModel;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.LinkModelRepository;
import com.fvp.repository.LinkRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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
  private final LinkCategoryRepository linkCategoryRepository;
  private final LinkModelRepository linkModelRepository;
  private final AllCatRepository allCatRepository;
  private final ElasticsearchClientService elasticsearchClientService;
  private final ElasticsearchSyncConfig elasticsearchSyncConfig;
  private final JdbcTemplate jdbcTemplate;

  // Cache for storing categories by tenant ID
  private final Map<Integer, List<AllCat>> categoriesCache = new ConcurrentHashMap<>();

  @Autowired
  public LinkProcessingService(
      DataSource dataSource,
      LinkRepository linkRepository,
      LinkCategoryRepository linkCategoryRepository,
      LinkModelRepository linkModelRepository,
      AllCatRepository allCatRepository,
      ElasticsearchClientService elasticsearchClientService,
      ElasticsearchSyncConfig elasticsearchSyncConfig,
      JdbcTemplate jdbcTemplate) {
    this.dataSource = dataSource;
    this.linkRepository = linkRepository;
    this.linkCategoryRepository = linkCategoryRepository;
    this.linkModelRepository = linkModelRepository;
    this.allCatRepository = allCatRepository;
    this.elasticsearchClientService = elasticsearchClientService;
    this.elasticsearchSyncConfig = elasticsearchSyncConfig;
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostConstruct
  public void logJdbcUrl() throws SQLException {
    String jdbcUrl = dataSource.getConnection().getMetaData().getURL();
    System.out.println("JDBC URL: " + jdbcUrl);
  }

  public void processLink(Link link, String categories, String models) {
    if (link == null) {
      logger.warn("Cannot process null link");
      return;
    }

    long startTimeMs = System.currentTimeMillis();
    long checkExistingTimeMs = 0;
    long saveLinkTimeMs = 0;
    long processCategoriesTimeMs = 0;
    long processModelsTimeMs = 0;
    
    try {
      // Check if link with same URL already exists
      long checkStartMs = System.currentTimeMillis();
      Link existingLink = linkRepository.findByLinkAndTenantId(link.getLink(), link.getTenantId());
      checkExistingTimeMs = System.currentTimeMillis() - checkStartMs;

      long saveStartMs = System.currentTimeMillis();
      if (existingLink != null) {
        logger.info("Found existing link with URL '{}', updating instead of creating new",
            link.getLink());

        // Clean up old categories and models before adding new ones
        logger.info("Deleting from link_category table: entries for link ID {}", existingLink.getId());
        linkCategoryRepository.deleteByLinkId(existingLink.getId());
        linkCategoryRepository.flush(); // Explicitly flush to DB

        logger.info("Deleting from link_model table: entries for link ID {}", existingLink.getId());
        linkModelRepository.deleteByLinkId(Integer.valueOf(existingLink.getId()));
        linkModelRepository.flush(); // Explicitly flush to DB

        // Update existing link with new data
        existingLink.setTitle(link.getTitle());
        existingLink.setThumbnail(link.getThumbnail());
        existingLink.setDuration(link.getDuration());
        existingLink.setQuality(link.getQuality());
        existingLink.setThumbPath(link.getThumbPath());
        existingLink.setSheetName(link.getSheetName());
        existingLink.setSource(link.getSource());
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

        // Create Elasticsearch document
        createElasticsearchDocument(link);
      }
      saveLinkTimeMs = System.currentTimeMillis() - saveStartMs;

      // Process categories if provided
      if (categories != null && !categories.trim().isEmpty()) {
        long categoriesStartMs = System.currentTimeMillis();
        processCategories(link, categories);
        processCategoriesTimeMs = System.currentTimeMillis() - categoriesStartMs;
      }

      // Process models if provided
      if (models != null && !models.trim().isEmpty()) {
        long modelsStartMs = System.currentTimeMillis();
        processModels(link, models);
        processModelsTimeMs = System.currentTimeMillis() - modelsStartMs;
      }
      
      long totalTimeMs = System.currentTimeMillis() - startTimeMs;
      logger.info("Link processing completed in {} ms (check: {} ms, save: {} ms, categories: {} ms, models: {} ms): {}",
          totalTimeMs, checkExistingTimeMs, saveLinkTimeMs, processCategoriesTimeMs, processModelsTimeMs, link.getTitle());
      
    } catch (Exception e) {
      long errorTimeMs = System.currentTimeMillis() - startTimeMs;
      logger.error("Error processing link after {} ms: {}", errorTimeMs, e.getMessage(), e);
      throw e; 
    }
  }

  private void processCategories(Link link, String categories) {
    // Tokenize categories using multiple delimiters (space, comma, hyphen)
    Set<String> categorySet = new HashSet<>();
    Set<String> categoriesToCreate = new HashSet<>();
    String[] tokens = TOKEN_PATTERN.split(categories);

    // Get all categories for this tenant from cache
    List<AllCat> allCategories = getCategoriesForTenant(link.getTenantId());

    // For each token from the input
    for (String token : tokens) {
      String category = token.trim();
      if (!category.isEmpty()) {
        // Check if the token exactly matches any category
        boolean exactMatch = false;
        for (AllCat allCat : allCategories) {
          if (allCat.getName().equalsIgnoreCase(category)) {
            categorySet.add(allCat.getName());
            exactMatch = true;
            break;
          }
        }

        // If no exact match, check for substring matches
        if (!exactMatch) {
          boolean substringMatch = false;
          for (AllCat allCat : allCategories) {
            if (category.toLowerCase().contains(allCat.getName().toLowerCase())) {
              categorySet.add(allCat.getName());
              logger.info("Found substring match: '{}' contains '{}'", category, allCat.getName());
              substringMatch = true;
            }
          }
          
          // If no match at all, mark for creation
          if (!exactMatch && !substringMatch) {
            categoriesToCreate.add(category);
          }
        }
      }
    }

    // Create new categories first
    if (!categoriesToCreate.isEmpty()) {
      logger.info("Creating {} new categories in AllCat", categoriesToCreate.size());
      List<AllCat> newCategories = new ArrayList<>();
      
      for (String categoryName : categoriesToCreate) {
        AllCat newCategory = new AllCat();
        newCategory.setTenantId(link.getTenantId());
        newCategory.setName(categoryName);
        newCategory.setHomeThumb(false);
        newCategory.setHeader(false);
        newCategory.setHomeSEO(false);
        newCategory.setHomeCatOrder(0);
        newCategory.setHome(0);
        newCategory.setDescription(null);
        newCategory.setCreatedAt(LocalDateTime.now());
        
        newCategories.add(newCategory);
      }
      
      // Save all new categories in a single batch
      List<AllCat> savedCategories = allCatRepository.saveAll(newCategories);
      allCatRepository.flush(); // Explicitly flush to DB
      logger.info("Created {} new categories in AllCat", savedCategories.size());
      
      // Add to the set of categories to create link associations
      for (AllCat savedCategory : savedCategories) {
        categorySet.add(savedCategory.getName());
        allCategories.add(savedCategory);
      }
      
      // Update the cache with the new categories
      categoriesCache.put(link.getTenantId(), allCategories);
    }

    // Create LinkCategory entries for all valid categories
    if (!categorySet.isEmpty()) {
      logger.info("Saving to link_category table: {} entries for link ID {}", categorySet.size(), link.getId());
      List<LinkCategory> linkCategories = new ArrayList<>();
      
      for (String categoryName : categorySet) {
        LinkCategory linkCategory = new LinkCategory();
        linkCategory.setTenantId(link.getTenantId());
        linkCategory.setLinkId(link.getId());
        linkCategory.setCategory(categoryName);
        linkCategory.setCreatedOn(link.getCreatedOn());
        linkCategory.setRandomOrder(link.getRandomOrder());
        linkCategories.add(linkCategory);
      }
      
      // Save all link categories in a single batch
      linkCategoryRepository.saveAll(linkCategories);
      linkCategoryRepository.flush(); // Explicitly flush to DB
      logger.info("Saved {} entries to link_category table for link ID {}", linkCategories.size(), link.getId());
    }
  }

  private void processModels(Link link, String models) {
    Set<String> modelSet = tokenize(models);
    logger.info("Saving to link_model table: {} entries for link ID {}", modelSet.size(),
        link.getId());
    
    List<LinkModel> linkModels = new ArrayList<>();
    for (String model : modelSet) {
      LinkModel linkModel = new LinkModel();
      linkModel.setTenantId(link.getTenantId());
      linkModel.setLinkId(link.getId());
      linkModel.setModel(model);
      linkModel.setCreatedOn(link.getCreatedOn());
      linkModel.setRandomOrder(link.getRandomOrder());
      linkModels.add(linkModel);
      logger.debug("Preparing to save model: {} for link ID {}", model, link.getId());
    }
    
    // Save all models in batch
    if (!linkModels.isEmpty()) {
      linkModelRepository.saveAll(linkModels);
      linkModelRepository.flush(); // Explicitly flush to DB
      logger.info("Saved {} entries to link_model table for link ID {}", modelSet.size(), link.getId());
    }
  }

  private Set<String> tokenize(String input) {
    Set<String> tokens = new HashSet<>();
    if (input != null && !input.trim().isEmpty()) {
      String[] words = TOKEN_PATTERN.split(input);
      for (String word : words) {
        String token = word.trim();
        if (!token.isEmpty()) {
          tokens.add(token);
        }
      }
    }
    return tokens;
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
   * Creates a new Elasticsearch document for a link
   */
  private void createElasticsearchDocument(Link link) {
    if (!elasticsearchSyncConfig.isEnabled()) {
      logger.debug("Elasticsearch sync is disabled, skipping document creation for link ID {}", link.getId());
      return;
    }
    
    try {
      LinkDocument doc = new LinkDocument();
      doc.setId(link.getId().toString());
      doc.setTenantId(link.getTenantId());
      doc.setTitle(link.getTitle());
      doc.setLink(link.getLink());
      doc.setThumbnail(link.getThumbnail());
      doc.setDuration(link.getDuration());
      doc.setSheetName(link.getSheetName());
      doc.setCreatedAt(new Date());
      doc.setSearchableText(generateSearchableText(link));

      // Get categories for the link
      List<LinkCategory> linkCategories = linkCategoryRepository.findByLinkId(link.getId());
      List<String> categories = linkCategories.stream()
          .map(LinkCategory::getCategory)
          .collect(Collectors.toList());
      doc.setCategories(categories);

      logger.info("Creating Elasticsearch document for link ID {} with {} categories", link.getId(), categories.size());
      elasticsearchClientService.saveLinkDocument(doc);
    } catch (Exception e) {
      logger.error("Error creating Elasticsearch document for link ID {}: {}", link.getId(), e.getMessage(), e);
    }
  }

  /**
   * Updates an existing Elasticsearch document for a link
   */
  private void updateElasticsearchDocument(Link link) {
    if (!elasticsearchSyncConfig.isEnabled()) {
      logger.debug("Elasticsearch sync is disabled, skipping document update for link ID {}", link.getId());
      return;
    }
    
    try {
      // Find existing document
      List<LinkDocument> existingDocs = elasticsearchClientService.searchByTitleOrText(link.getTitle(), null);
      
      if (!existingDocs.isEmpty()) {
        LinkDocument doc = existingDocs.get(0);
        doc.setTitle(link.getTitle());
        doc.setLink(link.getLink());
        doc.setThumbnail(link.getThumbnail());
        doc.setDuration(link.getDuration());
        doc.setSheetName(link.getSheetName());
        doc.setSearchableText(generateSearchableText(link));

        // Update categories
        List<LinkCategory> linkCategories = linkCategoryRepository.findByLinkId(link.getId());
        List<String> categories = linkCategories.stream()
            .map(LinkCategory::getCategory)
            .collect(Collectors.toList());
        doc.setCategories(categories);

        logger.info("Updating Elasticsearch document for link ID {} with {} categories", link.getId(), categories.size());
        elasticsearchClientService.saveLinkDocument(doc);
      } else {
        // If document doesn't exist in Elasticsearch, create it
        logger.info("Elasticsearch document not found for link ID {}, creating new document", link.getId());
        createElasticsearchDocument(link);
      }
    } catch (Exception e) {
      logger.error("Error updating Elasticsearch document for link ID {}: {}", link.getId(), e.getMessage(), e);
    }
  }

  /**
   * Generates searchable text for a link
   */
  private String generateSearchableText(Link link) {
    StringBuilder searchableText = new StringBuilder();
    searchableText.append(link.getTitle());
    
    if (link.getSheetName() != null && !link.getSheetName().isEmpty()) {
      searchableText.append(" ").append(link.getSheetName());
    }
    
    // Add categories to searchable text
    List<LinkCategory> linkCategories = linkCategoryRepository.findByLinkId(link.getId());
    for (LinkCategory linkCategory : linkCategories) {
      searchableText.append(" ").append(linkCategory.getCategory());
    }
    
    return searchableText.toString().trim();
  }

}