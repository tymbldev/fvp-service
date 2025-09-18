package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.CategoryWithCountDTO;
import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.LinkRepository;
import com.fvp.util.LoggingUtil;
import com.fvp.util.CacheBypassUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
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
  private static final int CHUNK_SIZE = 100;
  private static final String ALL_CATEGORIES_CACHE = "allCategories";

  private final AllCatRepository allCatRepository;
  private final LinkRepository linkRepository;
  private final CacheService cacheService;
  private final JedisPool jedisPool;
  private final AllCatService allCatService;
  private final LinkService linkService;
  private final LinkCountCacheService linkCountCacheService;
  private final LinkCategoryService linkCategoryService;
  private final int recentLinksDays;
  private final ExecutorService executorService;

  public CategoryService(
      AllCatRepository allCatRepository,
      LinkRepository linkRepository,
      CacheService cacheService,
      JedisPool jedisPool,
      AllCatService allCatService,
      LinkService linkService,
      LinkCountCacheService linkCountCacheService,
      LinkCategoryService linkCategoryService,
      @Value("${category.recent-links-days:90}") int recentLinksDays) {
    this.allCatRepository = allCatRepository;
    this.linkRepository = linkRepository;
    this.cacheService = cacheService;
    this.jedisPool = jedisPool;
    this.allCatService = allCatService;
    this.linkService = linkService;
    this.linkCountCacheService = linkCountCacheService;
    this.linkCategoryService = linkCategoryService;
    this.recentLinksDays = recentLinksDays;
    this.executorService = Executors.newFixedThreadPool(30);
  }

  public List<CategoryWithLinkDTO> getHomeCategoriesWithLinks(Integer tenantId) {
    long methodStartTime = System.currentTimeMillis();
    logger.info("=== Starting getHomeCategoriesWithLinks method ===");
    logger.info("Request parameters - tenantId: {}", tenantId);
    
    return LoggingUtil.logOperationTime(logger, "get home categories with links", () -> {
      String cacheKey = CacheService.generateCacheKey(HOME_CATEGORIES_CACHE, tenantId);
      logger.info("Generated cache key: '{}'", cacheKey);

      TypeReference<List<CategoryWithLinkDTO>> typeRef = new TypeReference<List<CategoryWithLinkDTO>>() {
      };

      // Try to get from cache first
      long cacheStartTime = System.currentTimeMillis();
      Optional<List<CategoryWithLinkDTO>> cachedResult = cacheService.getCollectionFromCache(
          CacheService.CACHE_NAME_CATEGORIES,
          cacheKey,
          typeRef
      );
      long cacheDuration = System.currentTimeMillis() - cacheStartTime;

      if (cachedResult.isPresent() && !cachedResult.get().isEmpty()) {
        logger.info("Cache HIT for home categories - key: '{}', duration: {} ms, cached result size: {} for tenant {}",
            cacheKey, cacheDuration, cachedResult.get().size(), tenantId);
        return cachedResult.get();
      }

      logger.info("Cache MISS for home categories - key: '{}', cache lookup duration: {} ms for tenant {}", 
          cacheKey, cacheDuration, tenantId);

      // Get all categories with home=1 ordered by home_cat_order
      logger.info("Fetching home categories from database - tenantId: {}, home=1", tenantId);
      List<AllCat> categories = LoggingUtil.logOperationTime(
          logger,
          "fetch home categories from database",
          () -> allCatService.findByTenantIdAndHomeOrderByHomeCatOrder(tenantId, 1)
      );

      if (categories.isEmpty()) {
        logger.info("No home categories found for tenant {} - returning empty list", tenantId);
        return Collections.emptyList();
      }
      
      logger.info("Found {} home categories for tenant {} - extracting category names", 
          categories.size(), tenantId);

      // Extract category names
      List<String> categoryNames = categories.stream()
          .map(AllCat::getName)
          .collect(Collectors.toList());
      logger.info("Extracted {} category names: {}", categoryNames.size(), categoryNames);

      // Get first links for all categories in bulk
      logger.info("Getting first links for {} categories in bulk for tenant {}", 
          categoryNames.size(), tenantId);
      List<CategoryWithLinkDTO> firstLinks = getCategoryFirstLinks(tenantId, categoryNames);
      logger.info("Retrieved {} first links for home categories", firstLinks.size());

      // Create a map for easy lookup
      Map<String, CategoryWithLinkDTO> dtoMap = firstLinks.stream()
          .collect(Collectors.toMap(CategoryWithLinkDTO::getName, dto -> dto));
      logger.info("Created DTO map with {} entries for lookup", dtoMap.size());

      // Create final result list preserving original order
      List<CategoryWithLinkDTO> result = categories.stream()
          .map(category -> dtoMap.get(category.getName()))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      logger.info("Created final result list with {} items (preserving original order)", result.size());

      // Store in cache
      logger.info("Storing {} home categories in cache with key: '{}'", result.size(), cacheKey);
      LoggingUtil.logOperationTime(
          logger,
          "store home categories in cache",
          () -> {
            cacheService.putInCache(CacheService.CACHE_NAME_CATEGORIES, cacheKey, result);
            return null;
          }
      );

      long methodDuration = System.currentTimeMillis() - methodStartTime;
      logger.info("=== Method completed in {} ms - stored {} home categories in cache for tenant {} ===", 
          methodDuration, result.size(), tenantId);
      return result;
    });
  }

  public CategoryWithLinkDTO getCategoryFirstLink(Integer tenantId, String categoryName) {
    long methodStartTime = System.currentTimeMillis();
    logger.info("=== Starting getCategoryFirstLink method ===");
    logger.info("Request parameters - tenantId: {}, categoryName: '{}'", tenantId, categoryName);
    
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters detected - tenantId: {}, categoryName: '{}' - returning null", 
          tenantId, categoryName);
      return null;
    }

    String cacheKey = tenantId + "_" + categoryName;
    logger.info("Generated cache key: '{}'", cacheKey);

    // Try to get from cache first
    long cacheStartTime = System.currentTimeMillis();
    Optional<CategoryWithLinkDTO> cachedResult = LoggingUtil.logOperationTime(
        logger,
        "get category first link from cache",
        () -> cacheService.getFromCache(
            CATEGORY_FIRST_LINK_CACHE,
            cacheKey,
            CategoryWithLinkDTO.class
        )
    );
    long cacheDuration = System.currentTimeMillis() - cacheStartTime;

    if (cachedResult.isPresent()) {
      logger.info("Cache HIT for category first link - key: '{}', duration: {} ms for tenant {} and category '{}'",
          cacheKey, cacheDuration, tenantId, categoryName);
      return cachedResult.get();
    }

    logger.info("Cache MISS for category first link - key: '{}', cache lookup duration: {} ms for tenant {} and category '{}'",
        cacheKey, cacheDuration, tenantId, categoryName);

    // Get the category by name
    logger.info("Fetching category from database - tenantId: {}, categoryName: '{}'", tenantId, categoryName);
    AllCat category = LoggingUtil.logOperationTime(
        logger,
        "fetch category by name",
        () -> allCatService.findByTenantIdAndName(tenantId, categoryName)
    );

    if (category == null) {
      logger.warn("Category NOT FOUND in database - tenantId: {}, categoryName: '{}' - returning null", 
          tenantId, categoryName);
      return null;
    }
    
    logger.info("Category found - ID: {}, Name: '{}', Description: '{}', TenantId: {}", 
        category.getId(), category.getName(), category.getDescription(), category.getTenantId());

    // Get the count of links for this category
    logger.info("Counting links for category - tenantId: {}, categoryName: '{}'", tenantId, categoryName);
    Long linkCount = LoggingUtil.logOperationTime(
        logger,
        "count links for category",
        () -> linkCategoryService.countByTenantIdAndCategory(tenantId, categoryName)
    );
    logger.info("Link count for category '{}': {}", categoryName, linkCount);

    // Get a random link for this category
    logger.info("Finding random link for category - tenantId: {}, categoryName: '{}'", tenantId, categoryName);
    Optional<LinkCategory> randomLinkCategory = LoggingUtil.logOperationTime(
        logger,
        "find random link by category",
        () -> linkCategoryService.findRandomLinkByCategory(tenantId, categoryName)
    );

    if (!randomLinkCategory.isPresent()) {
      logger.warn("No random link found for category '{}' in tenant {} - returning null", 
          categoryName, tenantId);
      return null;
    }

    Link link = randomLinkCategory.get().getLink();
    logger.info("Random link found - ID: {}, Title: '{}', Link: '{}', TenantId: {}", 
        link.getId(), link.getTitle(), link.getLink(), link.getTenantId());

    // Ensure the link belongs to the same tenant
    if (!link.getTenantId().equals(tenantId)) {
      logger.warn("Link tenant ID {} does not match requested tenant ID {} - returning null",
          link.getTenantId(), tenantId);
      return null;
    }

    logger.info("Creating CategoryWithLinkDTO for category '{}' and link '{}'", 
        categoryName, link.getLink());
    CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
    dto.setId(category.getId());
    dto.setName(category.getName());
    dto.setDescription(category.getDescription());
    dto.setLink(link.getLink());
    dto.setLinkTitle(link.getTitle());
    dto.setLinkThumbnail(link.getThumbnail());
    dto.setLinkThumbPath(link.getThumbpath());
    dto.setLinkSource(link.getSource());
    dto.setLinkTrailer(link.getTrailer());
    dto.setLinkDuration(link.getDuration());
    dto.setLinkCount(linkCount);

    // Store in cache
    logger.info("Storing category first link in cache with key: '{}'", cacheKey);
    LoggingUtil.logOperationTime(
        logger,
        "store category first link in cache",
        () -> {
          cacheService.putInCache(CATEGORY_FIRST_LINK_CACHE, cacheKey, dto);
          return null;
        }
    );

    long methodDuration = System.currentTimeMillis() - methodStartTime;
    logger.info("=== Method completed in {} ms - returning CategoryWithLinkDTO for category '{}' ===", 
        methodDuration, categoryName);
    return dto;
  }

  private List<CategoryWithLinkDTO> processChunk(Integer tenantId, List<String> categoryChunk,
      Map<String, AllCat> categoryMap, List<LinkCategory> linkCategories) {
    logger.info("Processing chunk with {} categories and {} link categories for tenant {}", 
        categoryChunk.size(), linkCategories.size(), tenantId);
    List<CategoryWithLinkDTO> chunkResults = new ArrayList<>();

    // Get link counts from cache
    logger.info("Getting link counts from cache for {} categories", categoryChunk.size());
    Map<String, Long> linkCountMap = linkCountCacheService.getLinkCounts(tenantId, categoryChunk);
    logger.info("Retrieved link counts for {} categories", linkCountMap.size());

    int processedCount = 0;
    int skippedCount = 0;

    for (LinkCategory linkCategory : linkCategories) {
      String categoryName = linkCategory.getCategory();
      AllCat category = categoryMap.get(categoryName);
      Link link = linkCategory.getLink();

      if (category == null || link == null || !link.getTenantId().equals(tenantId)) {
        logger.info("Skipping link category - category: '{}', categoryFound: {}, linkFound: {}, tenantMatch: {}", 
            categoryName, category != null, link != null, 
            link != null && link.getTenantId().equals(tenantId));
        skippedCount++;
        continue;
      }

      Long linkCount = linkCountMap.getOrDefault(categoryName, 0L);

      CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
      dto.setId(category.getId());
      dto.setName(category.getName());
      dto.setDescription(category.getDescription());
      dto.setLink(link.getLink());
      dto.setLinkTitle(link.getTitle());
      dto.setLinkThumbnail(link.getThumbnail());
      dto.setLinkThumbPath(link.getThumbpath());
      dto.setLinkSource(link.getSource());
      dto.setLinkTrailer(link.getTrailer());
      dto.setLinkDuration(link.getDuration());
      dto.setLinkCount(linkCount);

      chunkResults.add(dto);
      processedCount++;

      String cacheKey = tenantId + "_" + categoryName;
      cacheService.putInCache(CATEGORY_FIRST_LINK_CACHE, cacheKey, dto);
      logger.info("Created and cached DTO for category '{}' with link '{}'", 
          categoryName, link.getLink());
    }

    logger.info("Chunk processing completed - Processed: {}, Skipped: {}, Total DTOs: {} for tenant {}", 
        processedCount, skippedCount, chunkResults.size(), tenantId);
    return chunkResults;
  }

  public List<CategoryWithLinkDTO> getCategoryFirstLinks(Integer tenantId,
      List<String> categoryNames) {
    long methodStartTime = System.currentTimeMillis();
    logger.info("=== Starting getCategoryFirstLinks method ===");
    logger.info("Request parameters - tenantId: {}, categoryNames: {} ({} categories)", 
        tenantId, categoryNames, categoryNames != null ? categoryNames.size() : 0);
    
    if (tenantId == null || categoryNames == null || categoryNames.isEmpty()) {
      logger.warn("Invalid parameters detected - tenantId: {}, categoryNames: {} - returning empty list", 
          tenantId, categoryNames);
      return Collections.emptyList();
    }

    List<CategoryWithLinkDTO> results = new ArrayList<>();
    List<String> missedCategories = new ArrayList<>();

    logger.info("Checking cache for {} categories", categoryNames.size());
    // First try to get all from cache
    for (String categoryName : categoryNames) {
      String cacheKey = tenantId + "_" + categoryName;

      Optional<CategoryWithLinkDTO> cachedResult = LoggingUtil.logOperationTime(
          logger,
          "get category first link from cache for " + categoryName,
          () -> cacheService.getFromCache(
              CATEGORY_FIRST_LINK_CACHE,
              cacheKey,
              CategoryWithLinkDTO.class
          )
      );

      if (cachedResult.isPresent()) {
        logger.info("Cache HIT for category first link - tenant: {}, category: '{}'",
            tenantId, categoryName);
        results.add(cachedResult.get());
      } else {
        logger.info("Cache MISS for category first link - tenant: {}, category: '{}'",
            tenantId, categoryName);
        missedCategories.add(categoryName);
      }
    }
    
    logger.info("Cache check completed - Hits: {}, Misses: {} out of {} categories", 
        results.size(), missedCategories.size(), categoryNames.size());

    // If we have cache misses, process them in chunks
    if (!missedCategories.isEmpty()) {
      logger.info("Processing {} missed categories in chunks for tenant {}",
          missedCategories.size(), tenantId);

      // Split missed categories into chunks
      List<List<String>> chunks = new ArrayList<>();
      for (int i = 0; i < missedCategories.size(); i += CHUNK_SIZE) {
        chunks.add(missedCategories.subList(i, Math.min(i + CHUNK_SIZE, missedCategories.size())));
      }
      logger.info("Split missed categories into {} chunks of size {}", chunks.size(), CHUNK_SIZE);

      // Process chunks in parallel
      List<CompletableFuture<List<CategoryWithLinkDTO>>> futures = chunks.stream()
          .map(chunk -> {
            logger.info("Creating CompletableFuture for chunk: {}", chunk);
            // Capture the current cache bypass state
            boolean cacheBypass = CacheBypassUtil.isCacheBypass();
            return CompletableFuture.supplyAsync(
                () -> {
                  try {
                    logger.info("Starting parallel processing for chunk: {}", chunk);
                    // Set the cache bypass flag in the new thread
                    if (cacheBypass) {
                      CacheBypassUtil.setCacheBypass(true);
                      logger.info("Cache bypass enabled for chunk: {}", chunk);
                    }

                    // Get link categories
                    logger.info("Finding random links for {} categories in chunk", chunk.size());
                    List<LinkCategory> linkCategories = linkCategoryService.findRandomLinksByCategoryNames(
                        tenantId, chunk);
                    logger.info("Found {} link categories for chunk", linkCategories.size());

                    // Get categories and build a map by name
                    logger.info("Fetching categories from database for chunk: {}", chunk);
                    Map<String, AllCat> categoryMap = new HashMap<>();
                    List<AllCat> categories = allCatRepository.findByTenantIdAndNameIn(tenantId,
                        chunk);
                    for (AllCat category : categories) {
                      categoryMap.put(category.getName(), category);
                    }
                    logger.info("Created category map with {} entries for chunk", categoryMap.size());
                    
                    return processChunk(tenantId, chunk, categoryMap, linkCategories);
                  } catch (Exception e) {
                    logger.error("Exception occurred while processing chunk {}: {}", chunk,
                        e.getMessage(), e);
                    return new ArrayList<CategoryWithLinkDTO>();
                  } finally {
                    // Clear the cache bypass flag in the new thread
                    CacheBypassUtil.clearCacheBypass();
                    logger.info("Completed parallel processing for chunk: {}", chunk);
                  }
                },
                executorService);
          })
          .collect(Collectors.toList());

      logger.info("Created {} CompletableFutures for parallel processing", futures.size());

      // Wait for all chunks to complete and collect results
      int totalProcessed = 0;
      for (CompletableFuture<List<CategoryWithLinkDTO>> future : futures) {
        try {
          List<CategoryWithLinkDTO> chunkResults = future.get();
          results.addAll(chunkResults);
          totalProcessed += chunkResults.size();
          logger.info("Completed chunk processing - {} results added, total so far: {}", 
              chunkResults.size(), totalProcessed);
        } catch (Exception e) {
          logger.error("Error processing category chunk: {}", e.getMessage(), e);
        }
      }
      
      logger.info("Parallel processing completed - Total processed: {} DTOs from {} chunks", 
          totalProcessed, futures.size());
    }

    long methodDuration = System.currentTimeMillis() - methodStartTime;
    logger.info("=== Method completed in {} ms - returning {} CategoryWithLinkDTOs for {} categories ===", 
        methodDuration, results.size(), categoryNames.size());
    return results;
  }

  public List<CategoryWithLinkDTO> getHomeSeoCategories(Integer tenantId) {
    long methodStartTime = System.currentTimeMillis();
    logger.info("=== Starting getHomeSeoCategories method ===");
    logger.info("Request parameters - tenantId: {}", tenantId);
    
    String cacheKey = "homeSeoCategories_" + tenantId;
    logger.info("Generated cache key: '{}'", cacheKey);

    TypeReference<List<CategoryWithLinkDTO>> typeRef = new TypeReference<List<CategoryWithLinkDTO>>() {
    };

    // Try to get from cache first
    long cacheStartTime = System.currentTimeMillis();
    Optional<List<CategoryWithLinkDTO>> cachedResult = LoggingUtil.logOperationTime(
        logger,
        "get home SEO categories from cache",
        () -> cacheService.getCollectionFromCache(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            typeRef
        )
    );
    long cacheDuration = System.currentTimeMillis() - cacheStartTime;

    if (cachedResult.isPresent() && !cachedResult.get().isEmpty()) {
      logger.info("Cache HIT for home SEO categories - key: '{}', duration: {} ms, cached result size: {} for tenant {}",
          cacheKey, cacheDuration, cachedResult.get().size(), tenantId);
      return cachedResult.get();
    }

    logger.info("Cache MISS for home SEO categories - key: '{}', cache lookup duration: {} ms for tenant {}",
        cacheKey, cacheDuration, tenantId);

    // Get all categories with homeSEO=true
    logger.info("Fetching home SEO categories from database - tenantId: {}", tenantId);
    List<AllCat> categories = LoggingUtil.logOperationTime(
        logger,
        "fetch home SEO categories from database",
        () -> allCatService.findAllHomeSEOCategories(tenantId)
    );

    if (categories.isEmpty()) {
      logger.info("No home SEO categories found for tenant {} - returning empty list", tenantId);
      return Collections.emptyList();
    }
    
    logger.info("Found {} home SEO categories for tenant {} - extracting category names", 
        categories.size(), tenantId);

    // Extract category names
    List<String> categoryNames = categories.stream()
        .map(AllCat::getName)
        .collect(Collectors.toList());
    logger.info("Extracted {} category names: {}", categoryNames.size(), categoryNames);

    // Get first links for all categories in bulk
    logger.info("Getting first links for {} home SEO categories in bulk for tenant {}", 
        categoryNames.size(), tenantId);
    List<CategoryWithLinkDTO> result = getCategoryFirstLinks(tenantId, categoryNames);
    logger.info("Retrieved {} first links for home SEO categories", result.size());

    // Store in cache
    logger.info("Storing {} home SEO categories in cache with key: '{}'", result.size(), cacheKey);
    LoggingUtil.logOperationTime(
        logger,
        "store home SEO categories in cache",
        () -> {
          cacheService.putInCache(CacheService.CACHE_NAME_CATEGORIES, cacheKey, result);
          return null;
        }
    );

    long methodDuration = System.currentTimeMillis() - methodStartTime;
    logger.info("=== Method completed in {} ms - stored {} home SEO categories in cache for tenant {} ===", 
        methodDuration, result.size(), tenantId);
    return result;
  }

  /**
   * Helper method to create DTOs from LinkCategory entities
   */
  private List<CategoryWithLinkDTO> createDTOsFromLinkCategories(List<LinkCategory> linkCategories,
      AllCat category) {
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
      dto.setLinkThumbPath(link.getThumbpath());
      dto.setLinkSource(link.getSource());
      dto.setLinkTrailer(link.getTrailer());
      dto.setLinkDuration(link.getDuration());
      dtos.add(dto);
    }

    return dtos;
  }

  private String generateCacheKey(Integer tenantId, String key) {
    return CATEGORY_PREFIX + tenantId + "_" + key;
  }

  public List<CategoryWithCountDTO> getAllCategoriesWithLinkCounts(Integer tenantId) {
    long methodStartTime = System.currentTimeMillis();
    logger.info("=== Starting getAllCategoriesWithLinkCounts method ===");
    logger.info("Request parameters - tenantId: {}", tenantId);
    
    if (tenantId == null) {
      logger.warn("Invalid tenant ID detected - returning empty list");
      return Collections.emptyList();
    }

    String cacheKey = CacheService.generateCacheKey(ALL_CATEGORIES_CACHE, tenantId);
    logger.info("Generated cache key: '{}'", cacheKey);
    TypeReference<List<CategoryWithCountDTO>> typeRef = new TypeReference<List<CategoryWithCountDTO>>() {
    };

    // Try to get from cache first
    long cacheStartTime = System.currentTimeMillis();
    Optional<List<CategoryWithCountDTO>> cachedResult = LoggingUtil.logOperationTime(
        logger,
        "get all categories from cache",
        () -> cacheService.getCollectionFromCache(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            typeRef
        )
    );
    long cacheDuration = System.currentTimeMillis() - cacheStartTime;

    if (cachedResult.isPresent() && !cachedResult.get().isEmpty()) {
      logger.info("Cache HIT for all categories - key: '{}', duration: {} ms, cached result size: {} for tenant {}",
          cacheKey, cacheDuration, cachedResult.get().size(), tenantId);
      return cachedResult.get();
    }

    logger.info("Cache MISS for all categories - key: '{}', cache lookup duration: {} ms for tenant {}",
        cacheKey, cacheDuration, tenantId);

    // Get all categories from database
    logger.info("Fetching all categories from database - tenantId: {}, createdViaLink=false", tenantId);
    List<AllCat> categories = LoggingUtil.logOperationTime(
        logger,
        "fetch all categories from database",
        () -> allCatRepository.findByTenantIdAndCreatedViaLink(tenantId, false)
    );

    if (categories.isEmpty()) {
      logger.info("No categories found for tenant {} - returning empty list", tenantId);
      return Collections.emptyList();
    }
    
    logger.info("Found {} categories for tenant {} - extracting category names", 
        categories.size(), tenantId);

    // Extract category names
    List<String> categoryNames = categories.stream()
        .map(AllCat::getName)
        .collect(Collectors.toList());
    logger.info("Extracted {} category names: {}", categoryNames.size(), categoryNames);

    // Get link counts from cache service
    logger.info("Getting link counts from cache service for {} categories", categoryNames.size());
    Map<String, Long> linkCountMap = linkCountCacheService.getLinkCounts(tenantId, categoryNames);
    logger.info("Retrieved link counts for {} categories", linkCountMap.size());

    // Create DTOs with only required fields
    logger.info("Creating CategoryWithCountDTOs for {} categories", categories.size());
    List<CategoryWithCountDTO> result = categories.stream()
        .map(category -> {
          CategoryWithCountDTO dto = new CategoryWithCountDTO();
          dto.setName(category.getName());
          dto.setDescription(category.getDescription());
          dto.setLinkCount(linkCountMap.getOrDefault(category.getName(), 0L));
          logger.info("Created DTO for category '{}' with link count: {}", 
              category.getName(), dto.getLinkCount());
          return dto;
        })
        .collect(Collectors.toList());

    // Store in cache
    logger.info("Storing {} categories in cache with key: '{}' (24 hour expiry)", result.size(), cacheKey);
    LoggingUtil.logOperationTime(
        logger,
        "store all categories in cache",
        () -> {
          cacheService.putInCacheWithExpiry(
              CacheService.CACHE_NAME_CATEGORIES,
              cacheKey,
              result,
              24,
              TimeUnit.HOURS
          );
          return null;
        }
    );

    long methodDuration = System.currentTimeMillis() - methodStartTime;
    logger.info("=== Method completed in {} ms - stored {} categories in cache for tenant {} ===", 
        methodDuration, result.size(), tenantId);
    return result;
  }


  public void buildSystemCache() {
    long methodStartTime = System.currentTimeMillis();
    logger.info("=== Starting buildSystemCache method ===");
    
    try {
      // Get all distinct category names
      Set<String> allCategoryNames = new HashSet<>();
      logger.info("Initializing system cache build process");

      // Build home categories cache
      logger.info("Building home categories cache for tenant 1");
      List<CategoryWithLinkDTO> homeCategories = getHomeCategoriesWithLinks(1);
      logger.info("Built home categories cache with {} categories", homeCategories.size());
      allCategoryNames.addAll(
          homeCategories.stream().map(CategoryWithLinkDTO::getName).collect(Collectors.toSet()));
      logger.info("Added {} home category names to all categories set", 
          homeCategories.stream().map(CategoryWithLinkDTO::getName).collect(Collectors.toSet()).size());

      // Build home SEO categories cache
      logger.info("Building home SEO categories cache for tenant 1");
      List<CategoryWithLinkDTO> homeSeoCategories = getHomeSeoCategories(1);
      logger.info("Built home SEO categories cache with {} categories", homeSeoCategories.size());
      allCategoryNames.addAll(
          homeSeoCategories.stream().map(CategoryWithLinkDTO::getName).collect(Collectors.toSet()));
      logger.info("Added {} home SEO category names to all categories set", 
          homeSeoCategories.stream().map(CategoryWithLinkDTO::getName).collect(Collectors.toSet()).size());

      logger.info("Total unique category names collected: {}", allCategoryNames.size());

      // Build cache for each category's first page
      Pageable firstPage = PageRequest.of(0, 20, Sort.by("randomOrder"));
      logger.info("Building first page cache for {} categories", allCategoryNames.size());
      
      int successCount = 0;
      int errorCount = 0;
      
      for (String categoryName : allCategoryNames) {
        try {
          logger.info("Building cache for category: '{}'", categoryName);
          
          // Get links for the category
          List<LinkCategory> linkCategories = linkCategoryService.findByCategoryWithFiltersPageable(
              1, categoryName, null, null, null, 0, 20);
          logger.info("Found {} link categories for category '{}'", linkCategories.size(), categoryName);

          // Create DTOs from link categories
          List<CategoryWithLinkDTO> dtos = linkCategories.stream()
              .map(linkCategory -> {
                Link link = linkCategory.getLink();
                CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
                dto.setName(categoryName);
                dto.setLink(link.getLink());
                dto.setLinkTitle(link.getTitle());
                dto.setLinkThumbnail(link.getThumbnail());
                dto.setLinkThumbPath(link.getThumbpath());
                dto.setLinkDuration(link.getDuration());
                dto.setLinkSource(link.getSource());
                dto.setLinkTrailer(link.getTrailer());
                return dto;
              })
              .collect(Collectors.toList());
          logger.info("Created {} DTOs for category '{}'", dtos.size(), categoryName);

          // Store in cache
          String cacheKey = String.format("%s_%s_%d_%d_null_null_null",
              1, categoryName, 0, 20);
          logger.info("Storing cache for category '{}' with key: '{}'", categoryName, cacheKey);
          cacheService.putInCacheWithExpiry(
              CATEGORY_LINKS_CACHE,
              cacheKey,
              new PageImpl<>(dtos, firstPage, dtos.size()),
              1,
              TimeUnit.HOURS
          );

          logger.info("Successfully built cache for category: '{}' with {} items", 
              categoryName, dtos.size());
          successCount++;
        } catch (Exception e) {
          logger.error("Error building cache for category '{}': {}", categoryName, e.getMessage(), e);
          errorCount++;
        }
      }

      long methodDuration = System.currentTimeMillis() - methodStartTime;
      logger.info("=== System cache build completed in {} ms - Success: {}, Errors: {}, Total categories: {} ===",
          methodDuration, successCount, errorCount, allCategoryNames.size());
    } catch (Exception e) {
      long methodDuration = System.currentTimeMillis() - methodStartTime;
      logger.error("Error building system cache after {} ms: {}", methodDuration, e.getMessage(), e);
      throw e;
    }
  }
} 