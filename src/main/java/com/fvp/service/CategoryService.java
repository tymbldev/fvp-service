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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Autowired
  private AllCatRepository allCatRepository;

  @Autowired
  private LinkRepository linkRepository;

  @Autowired
  private CacheService cacheService;

  @Autowired
  private JedisPool jedisPool;

  @Autowired
  private AllCatService allCatService;

  @Autowired
  private LinkService linkService;

  @Autowired
  private LinkCountCacheService linkCountCacheService;

  @Autowired
  private LinkCategoryService linkCategoryService;

  @Value("${category.recent-links-days:90}")
  private int recentLinksDays;

  private final ExecutorService executorService = Executors.newFixedThreadPool(30);

  public List<CategoryWithLinkDTO> getHomeCategoriesWithLinks(Integer tenantId) {
    String cacheKey = CacheService.generateCacheKey(HOME_CATEGORIES_CACHE, tenantId);

    TypeReference<List<CategoryWithLinkDTO>> typeRef = new TypeReference<List<CategoryWithLinkDTO>>() {
    };

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
        () -> allCatService.findByTenantIdAndHomeOrderByHomeCatOrder(tenantId, 1)
    );

    if (categories.isEmpty()) {
      logger.info("No home categories found for tenant {}", tenantId);
      return Collections.emptyList();
    }

    // Extract category names
    List<String> categoryNames = categories.stream()
        .map(AllCat::getName)
        .collect(Collectors.toList());

    // Get first links for all categories in bulk
    List<CategoryWithLinkDTO> firstLinks = getCategoryFirstLinks(tenantId, categoryNames);

    // Create a map for easy lookup
    Map<String, CategoryWithLinkDTO> dtoMap = firstLinks.stream()
        .collect(Collectors.toMap(CategoryWithLinkDTO::getName, dto -> dto));

    // Create final result list preserving original order
    List<CategoryWithLinkDTO> result = categories.stream()
        .map(category -> dtoMap.get(category.getName()))
        .filter(Objects::nonNull)
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

    logger.info(
        "Cache miss for category first link, fetching from database for tenant {} and category {}",
        tenantId, categoryName);

    // Get the category by name
    AllCat category = LoggingUtil.logOperationTime(
        logger,
        "fetch category by name",
        () -> allCatService.findByTenantIdAndName(tenantId, categoryName)
    );

    if (category == null) {
      logger.warn("Category not found for tenant {} and name {}", tenantId, categoryName);
      return null;
    }

    // Get the count of links for this category
    Long linkCount = LoggingUtil.logOperationTime(
        logger,
        "count links for category",
        () -> linkCategoryService.countByTenantIdAndCategory(tenantId, categoryName)
    );

    // Get a random link for this category
    Optional<LinkCategory> randomLinkCategory = LoggingUtil.logOperationTime(
        logger,
        "find random link by category",
        () -> linkCategoryService.findRandomLinkByCategory(tenantId, categoryName)
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
    dto.setLinkThumbPath(link.getThumbpath());
    dto.setLinkSource(link.getSource());
    dto.setLinkTrailer(link.getTrailer());
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

  private List<CategoryWithLinkDTO> processChunk(Integer tenantId, List<String> categoryChunk,
      Map<String, AllCat> categoryMap, List<LinkCategory> linkCategories) {
    List<CategoryWithLinkDTO> chunkResults = new ArrayList<>();

    // Get link counts from cache
    Map<String, Long> linkCountMap = linkCountCacheService.getLinkCounts(tenantId, categoryChunk);

    for (LinkCategory linkCategory : linkCategories) {
      String categoryName = linkCategory.getCategory();
      AllCat category = categoryMap.get(categoryName);
      Link link = linkCategory.getLink();

      if (category == null || link == null || !link.getTenantId().equals(tenantId)) {
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

      String cacheKey = tenantId + "_" + categoryName;
      cacheService.putInCache(CATEGORY_FIRST_LINK_CACHE, cacheKey, dto);
    }

    return chunkResults;
  }

  public List<CategoryWithLinkDTO> getCategoryFirstLinks(Integer tenantId,
      List<String> categoryNames) {
    if (tenantId == null || categoryNames == null || categoryNames.isEmpty()) {
      logger.warn("Invalid parameters: tenantId={}, categoryNames={}", tenantId, categoryNames);
      return Collections.emptyList();
    }

    List<CategoryWithLinkDTO> results = new ArrayList<>();
    List<String> missedCategories = new ArrayList<>();

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
        logger.debug("Cache hit for category first link: tenant={}, category={}",
            tenantId, categoryName);
        results.add(cachedResult.get());
      } else {
        logger.debug("Cache miss for category first link: tenant={}, category={}",
            tenantId, categoryName);
        missedCategories.add(categoryName);
      }
    }

    // If we have cache misses, process them in chunks
    if (!missedCategories.isEmpty()) {
      logger.info("Processing {} missed categories in chunks for tenant {}",
          missedCategories.size(), tenantId);

      // Split missed categories into chunks
      List<List<String>> chunks = new ArrayList<>();
      for (int i = 0; i < missedCategories.size(); i += CHUNK_SIZE) {
        chunks.add(missedCategories.subList(i, Math.min(i + CHUNK_SIZE, missedCategories.size())));
      }

      // Process chunks in parallel
      List<CompletableFuture<List<CategoryWithLinkDTO>>> futures = chunks.stream()
          .map(chunk -> CompletableFuture.supplyAsync(
              () -> {
                // Get link categories
                List<LinkCategory> linkCategories = linkCategoryService.findRandomLinksByCategoryNames(
                    tenantId, chunk);

                // Get categories and build a map by name
                Map<String, AllCat> categoryMap = new HashMap<>();
                List<AllCat> categories = allCatRepository.findByTenantIdAndNameIn(tenantId, chunk);
                for (AllCat category : categories) {
                  categoryMap.put(category.getName(), category);
                }

                return processChunk(tenantId, chunk, categoryMap, linkCategories);
              },
              executorService
          ))
          .collect(Collectors.toList());

      // Wait for all chunks to complete and collect results
      for (CompletableFuture<List<CategoryWithLinkDTO>> future : futures) {
        try {
          results.addAll(future.get());
        } catch (Exception e) {
          logger.error("Error processing category chunk: {}", e.getMessage(), e);
        }
      }
    }

    return results;
  }

  public List<CategoryWithLinkDTO> getHomeSeoCategories(Integer tenantId) {
    String cacheKey = "homeSeoCategories_" + tenantId;

    TypeReference<List<CategoryWithLinkDTO>> typeRef = new TypeReference<List<CategoryWithLinkDTO>>() {
    };

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

    logger.info("Cache miss for home SEO categories, fetching from database for tenant {}",
        tenantId);

    // Get all categories with homeSEO=true
    List<AllCat> categories = LoggingUtil.logOperationTime(
        logger,
        "fetch home SEO categories from database",
        () -> allCatService.findAllHomeSEOCategories(tenantId)
    );

    if (categories.isEmpty()) {
      logger.info("No home SEO categories found for tenant {}", tenantId);
      return Collections.emptyList();
    }

    // Extract category names
    List<String> categoryNames = categories.stream()
        .map(AllCat::getName)
        .collect(Collectors.toList());

    // Get first links for all categories in bulk
    List<CategoryWithLinkDTO> result = getCategoryFirstLinks(tenantId, categoryNames);

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
    if (tenantId == null) {
      logger.warn("Invalid tenant ID: null");
      return Collections.emptyList();
    }

    String cacheKey = CacheService.generateCacheKey(ALL_CATEGORIES_CACHE, tenantId);
    TypeReference<List<CategoryWithCountDTO>> typeRef = new TypeReference<List<CategoryWithCountDTO>>() {
    };

    // Try to get from cache first
    Optional<List<CategoryWithCountDTO>> cachedResult = LoggingUtil.logOperationTime(
        logger,
        "get all categories from cache",
        () -> cacheService.getCollectionFromCache(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            typeRef
        )
    );

    if (cachedResult.isPresent() && !cachedResult.get().isEmpty()) {
      logger.info("Retrieved {} categories from cache for tenant {}",
          cachedResult.get().size(), tenantId);
      return cachedResult.get();
    }

    logger.info("Cache miss for all categories, fetching from database for tenant {}", tenantId);

    // Get all categories from database
    List<AllCat> categories = LoggingUtil.logOperationTime(
        logger,
        "fetch all categories from database",
        () -> allCatRepository.findByTenantIdAndCreatedViaLink(tenantId,false)
    );

    if (categories.isEmpty()) {
      logger.info("No categories found for tenant {}", tenantId);
      return Collections.emptyList();
    }

    // Extract category names
    List<String> categoryNames = categories.stream()
        .map(AllCat::getName)
        .collect(Collectors.toList());

    // Get link counts from cache service
    Map<String, Long> linkCountMap = linkCountCacheService.getLinkCounts(tenantId, categoryNames);

    // Create DTOs with only required fields
    List<CategoryWithCountDTO> result = categories.stream()
        .map(category -> {
          CategoryWithCountDTO dto = new CategoryWithCountDTO();
          dto.setName(category.getName());
          dto.setDescription(category.getDescription());
          dto.setLinkCount(linkCountMap.getOrDefault(category.getName(), 0L));
          return dto;
        })
        .collect(Collectors.toList());

    // Store in cache
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

    logger.info("Stored {} categories in cache for tenant {}", result.size(), tenantId);
    return result;
  }
} 