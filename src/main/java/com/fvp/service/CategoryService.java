package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.LinkRepository;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class CategoryService {

  private static final Logger logger = LoggingUtil.getLogger(CategoryService.class);
  private static final String HOME_CATEGORIES_CACHE = "homeCategories";
  private static final String CATEGORY_FIRST_LINK_CACHE = "categoryFirstLink";
  private static final String CATEGORY_LINKS_CACHE = "categoryLinks";
  private static final String CATEGORY_PREFIX = "homeCategories_";
  private static final int BATCH_SIZE = 1000;

  @Autowired
  private AllCatRepository allCatRepository;

  @Autowired
  private LinkCategoryRepository linkCategoryRepository;

  @Autowired
  private LinkRepository linkRepository;

  @Autowired
  private CacheService cacheService;
  
  @Autowired
  private JedisPool jedisPool;

  @Value("${category.recent-links-days:90}")
  private int recentLinksDays;

  private final ExecutorService executorService = Executors.newFixedThreadPool(30);

  @PostConstruct
  public void preloadCacheInBackground() {
    Thread thread = new Thread(() -> {
        try {
            logger.info("Starting cache preloading in background thread...");
            
            // Get distinct tenant IDs (optimized query)
            List<Integer> tenantIds = LoggingUtil.logOperationTime(
                logger,
                "fetch distinct tenant IDs",
                () -> linkRepository.findDistinctTenantIds()
            );
            logger.info("Found {} tenants for cache preloading", tenantIds.size());
            
            // Process each tenant
            for (Integer tenantId : tenantIds) {
                try {
                    logger.info("Preloading cache for tenant {}", tenantId);
                    // Get category keys for this tenant
                    List<String> keysToDelete = LoggingUtil.logOperationTime(
                        logger,
                        "get category keys for tenant",
                        () -> {
                            try (Jedis jedis = jedisPool.getResource()) {
                                Set<String> keys = jedis.keys(generateCacheKey(tenantId, "*"));
                                return keys.stream()
                                        .filter(key -> key.contains(CATEGORY_PREFIX))
                                        .collect(Collectors.toList());
                            }
                        }
                    );
                    
                    // Delete existing keys
                    if (!keysToDelete.isEmpty()) {
                        LoggingUtil.logOperationTime(
                            logger,
                            "delete existing category keys",
                            () -> {
                                try (Jedis jedis = jedisPool.getResource()) {
                                    jedis.del(keysToDelete.toArray(new String[0]));
                                }
                                return null;
                            }
                        );
                        logger.info("Deleted {} existing category cache keys for tenant {}", keysToDelete.size(), tenantId);
                    }
                    
                    // Get home categories with links - use pagination to avoid memory issues
                    List<CategoryWithLinkDTO> categories = getHomeCategoriesWithLinks(tenantId);
                    logger.info("Preloaded {} categories for tenant {}", categories.size(), tenantId);
                } catch (Exception e) {
                    logger.error("Error preloading cache for tenant {}: {}", tenantId, e.getMessage());
                }
            }
            
            logger.info("Completed cache preloading in background thread");
        } catch (Exception e) {
            logger.error("Error during cache preloading: {}", e.getMessage(), e);
        }
    });
    
    thread.setName("Cache-Preloader");
    thread.setDaemon(true);
    thread.start();
  }

  public List<CategoryWithLinkDTO> getHomeCategoriesWithLinks(Integer tenantId) {
    String cacheKey = CacheService.generateCacheKey(HOME_CATEGORIES_CACHE, tenantId);
    
    TypeReference<List<CategoryWithLinkDTO>> typeRef = new TypeReference<List<CategoryWithLinkDTO>>() {};
    
    // Try to get from cache first
    Optional<List<CategoryWithLinkDTO>> cachedResult = LoggingUtil.logOperationTime(
        logger, 
        "get home categories from cache", 
        () -> cacheService.getCollectionFromCache(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            typeRef
        )
    );

    if (cachedResult.isPresent() && !cachedResult.get().isEmpty()) {
      logger.info("Retrieved {} home categories from cache for tenant {}", 
          cachedResult.get().size(), tenantId);
      return cachedResult.get();
    }

    logger.info("Cache miss for home categories, fetching from database for tenant {}", tenantId);
    
    // Get all categories with home=1 ordered by home_cat_order
    List<AllCat> categories = LoggingUtil.logOperationTime(
        logger, 
        "fetch home categories from database", 
        () -> allCatRepository.findByTenantIdAndHomeOrderByHomeCatOrder(tenantId, 1)
    );
    
    if (categories.isEmpty()) {
      logger.info("No home categories found for tenant {}", tenantId);
      return Collections.emptyList();
    }
    
    // Process each category in parallel
    List<CompletableFuture<CategoryWithLinkDTO>> futures = new ArrayList<>();
    
    for (AllCat category : categories) {
      CompletableFuture<CategoryWithLinkDTO> future = CompletableFuture.supplyAsync(() -> {
        try {
          // Get the count of links for this category
          Long linkCount = LoggingUtil.logOperationTime(
              logger, 
              "count links for category " + category.getName(), 
              () -> linkCategoryRepository.countByTenantIdAndCategory(tenantId, category.getName())
          );

          // Get the first link using the dedicated method
          CategoryWithLinkDTO firstLink = getCategoryFirstLink(tenantId, category.getName());
          if (firstLink == null) {
            logger.warn("No links found for category {} in tenant {}", category.getName(), tenantId);
            return null; // Skip if no links found
          }

          CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
          dto.setId(category.getId());
          dto.setName(category.getName());
          dto.setDescription(category.getDescription());
          dto.setLink(firstLink.getLink());
          dto.setLinkTitle(firstLink.getLinkTitle());
          dto.setLinkThumbnail(firstLink.getLinkThumbnail());
          dto.setLinkThumbPath(firstLink.getLinkThumbPath());
          dto.setLinkDuration(firstLink.getLinkDuration());
          dto.setLinkCount(linkCount);
          
          return dto;
        } catch (Exception e) {
          logger.error("Error processing category {}: {}", category.getName(), e.getMessage(), e);
          return null;
        }
      }, executorService);
      
      futures.add(future);
    }
    
    // Wait for all futures to complete and collect results
    List<CategoryWithLinkDTO> result = futures.stream()
        .map(CompletableFuture::join)
        .filter(dto -> dto != null)
        .collect(Collectors.toList());

    // Store in cache
    LoggingUtil.logOperationTime(
        logger, 
        "store home categories in cache", 
        () -> {
          cacheService.putInCache(CacheService.CACHE_NAME_CATEGORIES, cacheKey, result);
          return null;
        }
    );
    
    logger.info("Stored {} home categories in cache for tenant {}", result.size(), tenantId);
    return result;
  }

  public CategoryWithLinkDTO getCategoryFirstLink(Integer tenantId, String categoryName) {
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters: tenantId={}, categoryName={}", tenantId, categoryName);
      return null;
    }

    String cacheKey = tenantId + "_" + categoryName;
    
    // Try to get from cache first
    Optional<CategoryWithLinkDTO> cachedResult = LoggingUtil.logOperationTime(
        logger, 
        "get category first link from cache", 
        () -> cacheService.getFromCache(
            CATEGORY_FIRST_LINK_CACHE, 
            cacheKey, 
            CategoryWithLinkDTO.class
        )
    );
    
    if (cachedResult.isPresent()) {
      logger.info("Retrieved category first link from cache for tenant {} and category {}", 
          tenantId, categoryName);
      return cachedResult.get();
    }
    
    logger.info("Cache miss for category first link, fetching from database for tenant {} and category {}", 
        tenantId, categoryName);
    
    // Get the category by name
    AllCat category = LoggingUtil.logOperationTime(
        logger, 
        "fetch category by name", 
        () -> allCatRepository.findByTenantIdAndName(tenantId, categoryName)
    );
    
    if (category == null) {
      logger.warn("Category not found for tenant {} and name {}", tenantId, categoryName);
      return null;
    }

    // Get the count of links for this category
    Long linkCount = LoggingUtil.logOperationTime(
        logger, 
        "count links for category", 
        () -> linkCategoryRepository.countByTenantIdAndCategory(tenantId, categoryName)
    );

    // Get a random link for this category
    Optional<LinkCategory> randomLinkCategory = LoggingUtil.logOperationTime(
        logger, 
        "find random link by category", 
        () -> linkCategoryRepository.findRandomLinkByCategory(tenantId, categoryName)
    );
    
    if (!randomLinkCategory.isPresent()) {
      logger.warn("No random link found for category {} in tenant {}", categoryName, tenantId);
      return null;
    }

    Link link = randomLinkCategory.get().getLink();

    // Ensure the link belongs to the same tenant
    if (!link.getTenantId().equals(tenantId)) {
      logger.warn("Link tenant ID {} does not match requested tenant ID {}", 
          link.getTenantId(), tenantId);
      return null;
    }

    CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
    dto.setId(category.getId());
    dto.setName(category.getName());
    dto.setDescription(category.getDescription());
    dto.setLink(link.getLink());
    dto.setLinkTitle(link.getTitle());
    dto.setLinkThumbnail(link.getThumbnail());
    dto.setLinkThumbPath(link.getThumbPath());
    dto.setLinkDuration(link.getDuration());
    dto.setLinkCount(linkCount);

    // Store in cache
    LoggingUtil.logOperationTime(
        logger, 
        "store category first link in cache", 
        () -> {
          cacheService.putInCache(CATEGORY_FIRST_LINK_CACHE, cacheKey, dto);
          return null;
        }
    );
    
    return dto;
  }

  public Page<CategoryWithLinkDTO> getCategoryLinks(Integer tenantId, String categoryName,
      Pageable pageable, Integer maxDuration, String quality) {
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters: tenantId={}, categoryName={}", tenantId, categoryName);
      return Page.empty();
    }

    logger.info("Fetching category links for tenant {} and category {}, page: {}, size: {}, maxDuration: {}, quality: {}", 
        tenantId, categoryName, pageable.getPageNumber(), pageable.getPageSize(), maxDuration, quality);
    
    // Get the category by name
    AllCat category = LoggingUtil.logOperationTime(
        logger, 
        "fetch category by name", 
        () -> allCatRepository.findByTenantIdAndName(tenantId, categoryName)
    );
    
    if (category == null) {
      logger.warn("Category not found for tenant {} and name {}", tenantId, categoryName);
      return Page.empty();
    }
    
    // Get the total count with filters (for pagination metadata)
    Long totalCount = LoggingUtil.logOperationTime(
        logger, 
        "count links with filters", 
        () -> linkCategoryRepository.countByCategoryWithFilters(tenantId, categoryName, maxDuration, quality)
    );
    
    if (totalCount == 0) {
      logger.info("No links found for category {} with given filters", categoryName);
      return Page.empty();
    }
    
    List<CategoryWithLinkDTO> pageContent = new ArrayList<>();
    
    // For first page, include the "first link" as the first item
    if (pageable.getPageNumber() == 0) {
      CategoryWithLinkDTO firstLink = getCategoryFirstLink(tenantId, categoryName);
      
      if (firstLink != null) {
        // Only include the first link when no filters are applied (default landing)
        boolean includeFirstLink = (maxDuration == null && quality == null);
        
        if (includeFirstLink) {
          pageContent.add(firstLink);
          
          // If first page has only one item (pageSize=1), we're done
          if (pageable.getPageSize() == 1) {
            return new PageImpl<>(pageContent, pageable, totalCount);
          }
          
          // Get link entity for exclusion from subsequent query
          Link linkEntity = null;
          try {
            linkEntity = linkRepository.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId, firstLink.getLink());
          } catch (Exception e) {
            logger.warn("Error fetching link entity: {}", e.getMessage());
          }
          
          // For first page, get one less item from DB and exclude the first link's ID
          Integer firstLinkId = linkEntity != null ? linkEntity.getId() : null;
          
          // Adjust offset and limit for the subsequent query
          int adjustedOffset = 0;
          int limit = pageable.getPageSize() - 1;
          
          // Get the remaining items for the first page
          List<LinkCategory> dbItems = LoggingUtil.logOperationTime(
              logger, 
              "fetch items with pagination excluding first link", 
              () -> linkCategoryRepository.findByCategoryWithFiltersExcludingLinkPageable(
                  tenantId, 
                  categoryName, 
                  maxDuration, 
                  quality, 
                  firstLinkId, 
                  adjustedOffset, 
                  limit
              )
          );
          
          pageContent.addAll(createDTOsFromLinkCategories(dbItems, category));
          return new PageImpl<>(pageContent, pageable, totalCount);
        }
      }
    }
    
    // For non-first pages or when filters are applied, use regular pagination
    int offset = (int) pageable.getOffset();
    int limit = pageable.getPageSize();
    
    List<LinkCategory> dbItems = LoggingUtil.logOperationTime(
        logger, 
        "fetch items with pagination", 
        () -> linkCategoryRepository.findByCategoryWithFiltersPageable(
            tenantId, 
            categoryName, 
            maxDuration, 
            quality, 
            offset, 
            limit
        )
    );
    
    // Add DB items to page content if first page didn't already add items
    if (pageContent.isEmpty()) {
      pageContent.addAll(createDTOsFromLinkCategories(dbItems, category));
    }
    
    logger.info("Returning page {} with {} items for tenant {} and category {}", 
        pageable.getPageNumber(), pageContent.size(), tenantId, categoryName);
    
    return new PageImpl<>(pageContent, pageable, totalCount);
  }
  
  /**
   * Helper method to create DTOs from LinkCategory entities
   */
  private List<CategoryWithLinkDTO> createDTOsFromLinkCategories(List<LinkCategory> linkCategories, AllCat category) {
    List<CategoryWithLinkDTO> dtos = new ArrayList<>();
    
    for (LinkCategory linkCategory : linkCategories) {
      Link link = linkCategory.getLink();
      
      // Skip links that don't match the tenant id (additional safety check)
      if (!link.getTenantId().equals(category.getTenantId())) {
        continue;
      }
      
      CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
      dto.setId(category.getId());
      dto.setName(category.getName());
      dto.setDescription(category.getDescription());
      dto.setLink(link.getLink());
      dto.setLinkTitle(link.getTitle());
      dto.setLinkThumbnail(link.getThumbnail());
      dto.setLinkThumbPath(link.getThumbPath());
      dto.setLinkDuration(link.getDuration());
      dto.setLinkCount(linkCategoryRepository.countByTenantIdAndCategory(category.getTenantId(), category.getName()));
      
      dtos.add(dto);
    }
    
    return dtos;
  }

  public List<CategoryWithLinkDTO> getHomeSeoCategories(Integer tenantId) {
    String cacheKey = "homeSeoCategories_" + tenantId;
    
    TypeReference<List<CategoryWithLinkDTO>> typeRef = new TypeReference<List<CategoryWithLinkDTO>>() {};
    
    // Try to get from cache first
    Optional<List<CategoryWithLinkDTO>> cachedResult = LoggingUtil.logOperationTime(
        logger, 
        "get home SEO categories from cache", 
        () -> cacheService.getCollectionFromCache(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            typeRef
        )
    );

    if (cachedResult.isPresent() && !cachedResult.get().isEmpty()) {
      logger.info("Retrieved {} home SEO categories from cache for tenant {}", 
          cachedResult.get().size(), tenantId);
      return cachedResult.get();
    }

    logger.info("Cache miss for home SEO categories, fetching from database for tenant {}", tenantId);
    
    // Get all categories with homeSEO=true
    List<AllCat> categories = LoggingUtil.logOperationTime(
        logger, 
        "fetch home SEO categories from database", 
        () -> allCatRepository.findAllHomeSEOCategories(tenantId)
    );
    
    if (categories.isEmpty()) {
      logger.info("No home SEO categories found for tenant {}", tenantId);
      return Collections.emptyList();
    }
    
    // Process each category in parallel
    List<CompletableFuture<CategoryWithLinkDTO>> futures = new ArrayList<>();
    
    for (AllCat category : categories) {
      CompletableFuture<CategoryWithLinkDTO> future = CompletableFuture.supplyAsync(() -> {
        try {
          // Get the count of links for this category
          Long linkCount = LoggingUtil.logOperationTime(
              logger, 
              "count links for category " + category.getName(), 
              () -> linkCategoryRepository.countByTenantIdAndCategory(tenantId, category.getName())
          );

          // Get the first link using the dedicated method
          CategoryWithLinkDTO firstLink = getCategoryFirstLink(tenantId, category.getName());
          if (firstLink == null) {
            logger.warn("No links found for category {} in tenant {}", category.getName(), tenantId);
            return null; // Skip if no links found
          }

          CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
          dto.setId(category.getId());
          dto.setName(category.getName());
          dto.setDescription(category.getDescription());
          dto.setLink(firstLink.getLink());
          dto.setLinkTitle(firstLink.getLinkTitle());
          dto.setLinkThumbnail(firstLink.getLinkThumbnail());
          dto.setLinkThumbPath(firstLink.getLinkThumbPath());
          dto.setLinkDuration(firstLink.getLinkDuration());
          dto.setLinkCount(linkCount);
          
          return dto;
        } catch (Exception e) {
          logger.error("Error processing category {}: {}", category.getName(), e.getMessage(), e);
          return null;
        }
      }, executorService);
      
      futures.add(future);
    }
    
    // Wait for all futures to complete and collect results
    List<CategoryWithLinkDTO> result = futures.stream()
        .map(CompletableFuture::join)
        .filter(dto -> dto != null)
        .collect(Collectors.toList());

    // Store in cache
    LoggingUtil.logOperationTime(
        logger, 
        "store home SEO categories in cache", 
        () -> {
          cacheService.putInCache(CacheService.CACHE_NAME_CATEGORIES, cacheKey, result);
          return null;
        }
    );
    
    logger.info("Stored {} home SEO categories in cache for tenant {}", result.size(), tenantId);
    return result;
  }

  private String generateCacheKey(Integer tenantId, String key) {
    return CATEGORY_PREFIX + tenantId + "_" + key;
  }
} 