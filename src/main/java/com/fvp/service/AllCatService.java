package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.entity.AllCat;
import com.fvp.repository.AllCatRepository;
import com.fvp.util.LoggingUtil;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AllCatService {

  private static final Logger logger = LoggingUtil.getLogger(AllCatService.class);
  private static final String CATEGORY_CACHE_PREFIX = "category_";
  private static final int CACHE_EXPIRY_MINUTES = 60;

  @Autowired
  private AllCatRepository allCatRepository;

  @Autowired
  private CacheService cacheService;

  public AllCat findByTenantIdAndName(Integer tenantId, String name) {
    String cacheKey = generateCacheKey(tenantId, name);

    return LoggingUtil.logOperationTime(logger, "find category by tenant and name", () -> {
      Optional<AllCat> cachedCategory = cacheService.getFromCache(
          CacheService.CACHE_NAME_CATEGORIES,
          cacheKey,
          AllCat.class
      );
      if (cachedCategory.isPresent()) {
        logger.info("Cache hit for category: {}", name);
        return cachedCategory.get();
      }

      logger.info("Cache miss for category: {}", name);
      AllCat category = allCatRepository.findByTenantIdAndName(tenantId, name);
      if (category != null) {
        cacheService.putInCacheWithExpiry(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            category,
            CACHE_EXPIRY_MINUTES,
            TimeUnit.MINUTES
        );
      }
      return category;
    });
  }

  public List<AllCat> findByTenantIdAndHomeOrderByHomeCatOrder(Integer tenantId, Integer home) {
    String cacheKey = generateCacheKey(tenantId, "home_" + home);

    return LoggingUtil.logOperationTime(logger, "find home categories", () -> {
      TypeReference<List<AllCat>> typeRef = new TypeReference<List<AllCat>>() {
      };
      Optional<List<AllCat>> cachedCategories = cacheService.getCollectionFromCache(
          CacheService.CACHE_NAME_CATEGORIES,
          cacheKey,
          typeRef
      );
      if (cachedCategories.isPresent()) {
        logger.info("Cache hit for home categories");
        return cachedCategories.get();
      }

      logger.info("Cache miss for home categories");
      List<AllCat> categories = allCatRepository.findByTenantIdAndHomeOrderByHomeCatOrder(tenantId,
          home);
      if (!categories.isEmpty()) {
        cacheService.putInCacheWithExpiry(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            categories,
            CACHE_EXPIRY_MINUTES,
            TimeUnit.MINUTES
        );
      }
      return categories;
    });
  }

  public List<AllCat> findAllHomeSEOCategories(Integer tenantId) {
    String cacheKey = generateCacheKey(tenantId, "home_seo");

    return LoggingUtil.logOperationTime(logger, "find home SEO categories", () -> {
      TypeReference<List<AllCat>> typeRef = new TypeReference<List<AllCat>>() {
      };
      Optional<List<AllCat>> cachedCategories = cacheService.getCollectionFromCache(
          CacheService.CACHE_NAME_CATEGORIES,
          cacheKey,
          typeRef
      );
      if (cachedCategories.isPresent()) {
        logger.info("Cache hit for home SEO categories");
        return cachedCategories.get();
      }

      logger.info("Cache miss for home SEO categories");
      List<AllCat> categories = allCatRepository.findAllHomeSEOCategories(tenantId);
      if (!categories.isEmpty()) {
        cacheService.putInCacheWithExpiry(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            categories,
            CACHE_EXPIRY_MINUTES,
            TimeUnit.MINUTES
        );
      }
      return categories;
    });
  }

  private String generateCacheKey(Integer tenantId, String suffix) {
    return CATEGORY_CACHE_PREFIX + tenantId + "_" + suffix;
  }
} 