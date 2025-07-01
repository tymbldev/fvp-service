package com.fvp.service;

import com.fvp.util.LoggingUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fvp.repository.ElasticsearchLinkCategoryRepository;

@Service
public class LinkCountCacheService {

  private static final Logger logger = LoggingUtil.getLogger(LinkCountCacheService.class);
  private static final String LINK_COUNT_CACHE = "linkCount";
  private static final int CACHE_EXPIRY_HOURS = 24;

  @Autowired
  private CacheService cacheService;

  @Autowired
  private ElasticsearchLinkCategoryRepository elasticsearchLinkCategoryRepository;

  /**
   * Get link counts for categories with caching
   *
   * @param tenantId The tenant ID
   * @param categoryNames List of category names
   * @return Map of category name to link count
   */
  public Map<String, Long> getLinkCounts(Integer tenantId, List<String> categoryNames) {
    return LoggingUtil.logOperationTime(logger, "get link counts", () -> {
      logger.info("Getting link counts for tenant {} and {} categories", tenantId, categoryNames.size());
      Map<String, Long> result = new HashMap<>();

      // Try to get counts from cache first
      Map<String, Long> cachedCounts = getCachedCounts(tenantId, categoryNames);
      logger.info("Retrieved {} counts from cache", cachedCounts.size());

      // Find which categories are not in cache
      List<String> uncachedCategories = categoryNames.stream()
          .filter(name -> !cachedCounts.containsKey(name))
          .collect(Collectors.toList());

      logger.info("Found {} categories not in cache", uncachedCategories.size());

      // Add cached counts to result
      result.putAll(cachedCounts);

      // If there are uncached categories, get them from DB using sharded repositories
      if (!uncachedCategories.isEmpty()) {
        logger.info("Fetching {} uncached categories from database", uncachedCategories.size());
        Map<String, Long> dbCounts = getAndCacheDbCounts(tenantId, uncachedCategories);
        logger.info("Retrieved {} counts from database", dbCounts.size());
        result.putAll(dbCounts);
      }

      logger.info("Total link counts retrieved: {}", result.size());
      return result;
    });
  }

  private Map<String, Long> getCachedCounts(Integer tenantId, List<String> categoryNames) {
    Map<String, Long> cachedCounts = new HashMap<>();

    for (String categoryName : categoryNames) {
      String cacheKey = generateCacheKey(tenantId, categoryName);
      Optional<Long> count = cacheService.getFromCache(LINK_COUNT_CACHE, cacheKey, Long.class);
      if (count.isPresent()) {
        cachedCounts.put(categoryName, count.get());
        logger.info("Cache hit for category count: {} = {}", categoryName, count.get());
      } else {
        logger.info("Cache miss for category count: {}", categoryName);
      }
    }

    return cachedCounts;
  }

  private Map<String, Long> getAndCacheDbCounts(Integer tenantId, List<String> categoryNames) {
    Map<String, Long> dbCounts = new HashMap<>();
    // Get counts for all categories at once from Elasticsearch
    List<Object[]> counts = elasticsearchLinkCategoryRepository.countByTenantIdAndCategories(tenantId, categoryNames);
    for (Object[] count : counts) {
      String categoryName = (String) count[0];
      Long countValue = ((Number) count[1]).longValue();
      dbCounts.put(categoryName, countValue);
      // Cache individual count
      String cacheKey = generateCacheKey(tenantId, categoryName);
      cacheService.putInCacheWithExpiry(
          LINK_COUNT_CACHE,
          cacheKey,
          countValue,
          CACHE_EXPIRY_HOURS,
          TimeUnit.HOURS
      );
      logger.info("Cached count for category {} = {}", categoryName, countValue);
    }
    return dbCounts;
  }

  private String generateCacheKey(Integer tenantId, String categoryName) {
    return String.format("%d_%s", tenantId, categoryName);
  }
} 