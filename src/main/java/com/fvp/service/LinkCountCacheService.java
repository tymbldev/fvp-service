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

@Service
public class LinkCountCacheService {

  private static final Logger logger = LoggingUtil.getLogger(LinkCountCacheService.class);
  private static final String LINK_COUNT_CACHE = "linkCount";
  private static final int CACHE_EXPIRY_HOURS = 24;

  @Autowired
  private LinkCategoryShardingService shardingService;

  @Autowired
  private CacheService cacheService;

  /**
   * Get link counts for categories with caching
   *
   * @param tenantId The tenant ID
   * @param categoryNames List of category names
   * @return Map of category name to link count
   */
  public Map<String, Long> getLinkCounts(Integer tenantId, List<String> categoryNames) {
    return LoggingUtil.logOperationTime(logger, "get link counts", () -> {
      Map<String, Long> result = new HashMap<>();

      // Try to get counts from cache first
      Map<String, Long> cachedCounts = getCachedCounts(tenantId, categoryNames);

      // Find which categories are not in cache
      List<String> uncachedCategories = categoryNames.stream()
          .filter(name -> !cachedCounts.containsKey(name))
          .collect(Collectors.toList());

      // Add cached counts to result
      result.putAll(cachedCounts);

      // If there are uncached categories, get them from DB using sharded repositories
      if (!uncachedCategories.isEmpty()) {
        Map<String, Long> dbCounts = getAndCacheDbCounts(tenantId, uncachedCategories);
        result.putAll(dbCounts);
      }

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
        logger.debug("Cache hit for category count: {}", categoryName);
      }
    }

    return cachedCounts;
  }

  private Map<String, Long> getAndCacheDbCounts(Integer tenantId, List<String> categoryNames) {
    Map<String, Long> dbCounts = new HashMap<>();

    // Group categories by their shard number
    Map<Integer, List<String>> categoriesByShard = categoryNames.stream()
        .collect(Collectors.groupingBy(shardingService::getShardNumber));

    // Process each shard
    for (Map.Entry<Integer, List<String>> entry : categoriesByShard.entrySet()) {
      int shardNumber = entry.getKey();
      List<String> categoriesInShard = entry.getValue();

      try {
        // Get counts from the appropriate sharded repository
        List<Object[]> counts = shardingService.getRepositoryForShard(shardNumber)
            .countByTenantIdAndCategories(tenantId, categoriesInShard);

        // Process and cache results
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
          logger.debug("Cached count for category: {}", categoryName);
        }
      } catch (Exception e) {
        logger.error("Error getting counts from shard {}: {}", shardNumber, e.getMessage());
        throw e;
      }
    }

    return dbCounts;
  }

  private String generateCacheKey(Integer tenantId, String categoryName) {
    return String.format("%d_%s", tenantId, categoryName);
  }
} 