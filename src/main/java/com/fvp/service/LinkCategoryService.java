package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.entity.BaseLinkCategory;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.ElasticsearchLinkCategoryRepository;
import com.fvp.document.LinkDocument;
import com.fvp.entity.Link;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import com.fvp.util.LoggingUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkCategoryService {

  private static final Logger logger = LoggingUtil.getLogger(LinkCategoryService.class);
  private static final String LINK_CATEGORY_CACHE = "linkCategory";
  private static final String CATEGORY_COUNT_CACHE = "categoryCount";
  private static final String CATEGORIES_CACHE = "categories";
  private static final int CACHE_EXPIRY_MINUTES = 60;


  @Autowired
  private CacheService cacheService;

  @Value("${category.recent-links-days:3}")
  private long recentLinksDays;

  private final ElasticsearchLinkCategoryRepository elasticsearchLinkCategoryRepository;

  public LinkCategoryService(ElasticsearchLinkCategoryRepository elasticsearchLinkCategoryRepository) {
    this.elasticsearchLinkCategoryRepository = elasticsearchLinkCategoryRepository;
  }

  /**
   * Find a random recent link by category
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @return an optional containing a random LinkCategory if found
   */
  public Optional<LinkCategory> findRandomRecentLinkByCategory(Integer tenantId, String category) {
    String cacheKey = generateCacheKey(tenantId, "recent:" + category);

    Optional<LinkCategory> cachedModel = cacheService.getFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        LinkCategory.class
    );

    if (cachedModel.isPresent()) {
      return cachedModel;
    }

    try {
      Optional<LinkDocument> result = elasticsearchLinkCategoryRepository.findRandomRecentLinkByCategory(
          tenantId, category, recentLinksDays);
      Optional<LinkCategory> linkCategory = result.map(doc -> convertToLinkCategory(doc, category));
      linkCategory.ifPresent(
          model -> cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, model));
      return linkCategory;
    } catch (Exception e) {
      logger.error("Error finding random recent link for category {} in Elasticsearch: {}",
          category, e.getMessage());
      throw e;
    }
  }

  /**
   * Find a random link by category
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @return an optional containing a random LinkCategory if found
   */
  public Optional<LinkCategory> findRandomLinkByCategory(Integer tenantId, String category) {
    String cacheKey = generateCacheKey(tenantId, "random:" + category);

    Optional<LinkCategory> cachedModel = cacheService.getFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        LinkCategory.class
    );

    if (cachedModel.isPresent()) {
      return cachedModel;
    }

    try {
      Optional<LinkDocument> result = elasticsearchLinkCategoryRepository.findRandomLinkByCategory(tenantId,
          category);
      Optional<LinkCategory> linkCategory = result.map(doc -> convertToLinkCategory(doc, category));
      linkCategory.ifPresent(
          model -> cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, model));
      return linkCategory;
    } catch (Exception e) {
      logger.error("Error finding random link for category {} in Elasticsearch: {}",
          category, e.getMessage());
      throw e;
    }
  }

  /**
   * Count links by category for a tenant
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @return the count of matching links
   */
  public Long countByTenantIdAndCategory(Integer tenantId, String category) {
    String cacheKey = generateCacheKey(tenantId, "count:" + category);

    return cacheService.getFromCache(
        CATEGORY_COUNT_CACHE,
        cacheKey,
        Long.class
    ).orElseGet(() -> {
      try {
        Long count = elasticsearchLinkCategoryRepository.countByTenantIdAndCategory(tenantId, category);
        cacheService.putInCache(CATEGORY_COUNT_CACHE, cacheKey, count);
        return count;
      } catch (Exception e) {
        logger.error("Error counting links for category {} in Elasticsearch: {}",
            category, e.getMessage());
        throw e;
      }
    });
  }

  /**
   * Finds links by tenant ID and link ID
   *
   * @param tenantId the tenant ID
   * @param linkId the link ID
   * @return a list of matching links
   */
  public List<LinkCategory> findByTenantIdAndLinkId(Integer tenantId, Integer linkId) {
    String cacheKey = generateCacheKey(tenantId, "linkId:" + linkId);

    return cacheService.getCollectionFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        new TypeReference<List<LinkCategory>>() {
        }
    ).orElseGet(() -> {
      try {
        List<LinkDocument> docs = elasticsearchLinkCategoryRepository.findByTenantIdAndLinkId(tenantId, linkId);
        List<LinkCategory> result = docs.stream().map(this::convertToLinkCategory).collect(Collectors.toList());
        cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, result);
        return result;
        } catch (Exception e) {
        logger.error("Error finding links for tenant {} and link {} in Elasticsearch: {}",
            tenantId, linkId, e.getMessage());
          throw e;
      }
    });
  }

  /**
   * Finds all distinct categories for a tenant
   *
   * @param tenantId the tenant ID
   * @return a list of distinct category names
   */
  public List<String> findAllDistinctCategories(Integer tenantId) {
    String cacheKey = generateCacheKey(tenantId, "distinct");

    return cacheService.getCollectionFromCache(
        CATEGORIES_CACHE,
        cacheKey,
        new TypeReference<List<String>>() {
        }
    ).orElseGet(() -> {
      try {
        List<String> result = elasticsearchLinkCategoryRepository.findAllDistinctCategories(tenantId);
        cacheService.putInCache(CATEGORIES_CACHE, cacheKey, result);
        return result;
        } catch (Exception e) {
        logger.error("Error finding distinct categories in Elasticsearch: {}", e.getMessage());
          throw e;
      }
    });
  }

  /**
   * Find links by link ID
   *
   * @param linkId the link ID
   * @return a list of matching LinkCategory entities
   */
  public List<LinkCategory> findByLinkId(Integer linkId) {
    String cacheKey = generateCacheKey(null, "linkId:" + linkId);

    return cacheService.getCollectionFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        new TypeReference<List<LinkCategory>>() {
        }
    ).orElseGet(() -> {
      try {
        List<LinkDocument> docs = elasticsearchLinkCategoryRepository.findByLinkId(linkId);
        List<LinkCategory> result = docs.stream().map(this::convertToLinkCategory).collect(Collectors.toList());
        cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, result);
        return result;
        } catch (Exception e) {
        logger.error("Error finding links for link {} in Elasticsearch: {}", linkId, e.getMessage());
          throw e;
      }
    });
  }


  /**
   * Delete a LinkCategory entity by link ID
   *
   * @param linkId the link ID to delete
   */
  @Transactional
  public void deleteByLinkId(Integer linkId) {
    try {
      elasticsearchLinkCategoryRepository.deleteByLinkId(linkId);
      cacheService.evictFromCache(LINK_CATEGORY_CACHE, "linkId:" + linkId);
    } catch (Exception e) {
      logger.error("Error deleting from Elasticsearch: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Finds random links by a list of category names for a tenant
   *
   * @param tenantId the tenant ID
   * @param categoryNames the list of category names
   * @return a list of random LinkCategory objects
   */
  public List<LinkCategory> findRandomLinksByCategoryNames(Integer tenantId, List<String> categoryNames) {
    List<LinkCategory> result = new ArrayList<>();
    for (String category : categoryNames) {
      findRandomLinkByCategory(tenantId, category).ifPresent(result::add);
    }
    return result;
  }

  /**
   * Find links by tenant ID and category
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @return a list of matching LinkCategory entities
   */
  public List<LinkCategory> findByTenantIdAndCategory(Integer tenantId, String category) {
    String cacheKey = generateCacheKey(tenantId, "category:" + category);

    return cacheService.getCollectionFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        new TypeReference<List<LinkCategory>>() {
        }
    ).orElseGet(() -> {
      try {
        List<LinkDocument> docs = elasticsearchLinkCategoryRepository.findByTenantIdAndCategory(tenantId, category);
        List<LinkCategory> models = docs.stream()
            .map(this::convertToLinkCategory)
            .collect(Collectors.toList());
        cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
        return models;
      } catch (Exception e) {
        logger.error("Error finding links for category {} in Elasticsearch: {}",
            category, e.getMessage());
        throw e;
      }
    });
  }

  /**
   * Find links by tenant ID and category ordered by random order
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @return a list of matching LinkCategory entities
   */
  public List<LinkCategory> findByTenantIdAndCategoryOrderByRandomOrder(Integer tenantId,
      String category) {
    String cacheKey = generateCacheKey(tenantId, "categoryOrder:" + category);

    return cacheService.getCollectionFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        new TypeReference<List<LinkCategory>>() {
        }
    ).orElseGet(() -> {
      try {
        List<LinkDocument> docs = elasticsearchLinkCategoryRepository.findByTenantIdAndCategoryOrderByRandomOrder(tenantId, category);
        List<LinkCategory> models = docs.stream()
            .map(this::convertToLinkCategory)
            .collect(Collectors.toList());
        cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
        return models;
      } catch (Exception e) {
        logger.error("Error finding links for category {} in Elasticsearch: {}",
            category, e.getMessage());
        throw e;
      }
    });
  }

  /**
   * Find links by category and tenant ID
   *
   * @param category the category name
   * @param tenantId the tenant ID
   * @return a list of matching LinkCategory entities
   */
  public List<LinkCategory> findByCategoryAndTenantId(String category, Integer tenantId) {
    String cacheKey = generateCacheKey(tenantId, "category:" + category);

    return cacheService.getCollectionFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        new TypeReference<List<LinkCategory>>() {
        }
    ).orElseGet(() -> {
      try {
        List<LinkDocument> docs = elasticsearchLinkCategoryRepository.findByCategoryAndTenantId(category, tenantId);
        List<LinkCategory> models = docs.stream()
            .map(this::convertToLinkCategory)
            .collect(Collectors.toList());
        cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
        return models;
      } catch (Exception e) {
        logger.error("Error finding links for category {} in Elasticsearch: {}",
            category, e.getMessage());
        throw e;
      }
    });
  }

  /**
   * Find links by category with filters
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @param minDuration the minimum duration
   * @param maxDuration the maximum duration
   * @param quality the quality
   * @param offset the offset
   * @param limit the limit
   * @return a list of LinkCategory entities
   */
  public List<LinkCategory> findByCategoryWithFiltersPageable(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality,
      int offset,
      int limit) {
    String cacheKey = generateCacheKey(tenantId,
        String.format("categoryFilters:%s:%d:%d:%s:%d:%d",
            category, minDuration, maxDuration, quality, offset, limit));

    return cacheService.getCollectionFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        new TypeReference<List<LinkCategory>>() {
        }
    ).orElseGet(() -> {
      try {
        List<LinkDocument> docs = elasticsearchLinkCategoryRepository.findByCategoryWithFiltersPageable(
            tenantId, category, minDuration, maxDuration, quality, offset, limit);
        List<LinkCategory> models = docs.stream()
            .map(this::convertToLinkCategory)
            .collect(Collectors.toList());
        cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
        return models;
      } catch (Exception e) {
        logger.error("Error finding links for category {} with filters in Elasticsearch: {}",
            category, e.getMessage());
        throw e;
      }
    });
  }

  /**
   * Count links by category with filters
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @param minDuration the minimum duration
   * @param maxDuration the maximum duration
   * @param quality the quality
   * @return the count
   */
  public Long countByCategoryWithFilters(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality) {
    String cacheKey = generateCacheKey(tenantId,
        String.format("categoryCountFilters:%s:%d:%d:%s",
            category, minDuration, maxDuration, quality));

    return cacheService.getFromCache(
        CATEGORY_COUNT_CACHE,
        cacheKey,
        Long.class
    ).orElseGet(() -> {
      try {
        Long count = elasticsearchLinkCategoryRepository.countByCategoryWithFilters(
            tenantId, category, minDuration, maxDuration, quality);
        cacheService.putInCache(CATEGORY_COUNT_CACHE, cacheKey, count);
        return count;
      } catch (Exception e) {
        logger.error("Error counting links for category {} with filters in Elasticsearch: {}",
            category, e.getMessage());
        throw e;
      }
    });
  }

  /**
   * Find links by category with filters excluding a link
   *
   * @param tenantId the tenant ID
   * @param category the category name
   * @param minDuration the minimum duration
   * @param maxDuration the maximum duration
   * @param quality the quality
   * @param excludeId the link ID to exclude
   * @param offset the offset
   * @param limit the limit
   * @return a list of LinkCategory entities
   */
  public List<LinkCategory> findByCategoryWithFiltersExcludingLinkPageable(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality,
      Integer excludeId,
      int offset,
      int limit) {
    String cacheKey = generateCacheKey(tenantId,
        String.format("categoryFiltersExclude:%s:%d:%d:%s:%d:%d:%d",
            category, minDuration, maxDuration, quality, excludeId, offset, limit));

    return cacheService.getCollectionFromCache(
        LINK_CATEGORY_CACHE,
        cacheKey,
        new TypeReference<List<LinkCategory>>() {
        }
    ).orElseGet(() -> {
      try {
        List<LinkDocument> docs = elasticsearchLinkCategoryRepository.findByCategoryWithFiltersExcludingLinkPageable(
            tenantId, category, minDuration, maxDuration, quality, excludeId, offset, limit);
        List<LinkCategory> models = docs.stream()
            .map(this::convertToLinkCategory)
            .collect(Collectors.toList());
        cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
        return models;
      } catch (Exception e) {
        logger.error(
            "Error finding links for category {} with filters excluding link {} in Elasticsearch: {}",
            category, excludeId, e.getMessage());
        throw e;
      }
    });
  }

  /**
   * Count links by tenant ID and categories
   *
   * @param tenantId the tenant ID
   * @param categoryNames the category names
   * @return a list of model counts
   */
  public List<Object[]> countByTenantIdAndCategories(Integer tenantId, List<String> categoryNames) {
    String cacheKey = generateCacheKey(tenantId,
        "categoryCounts:" + String.join(",", categoryNames));

    return cacheService.getCollectionFromCache(
        CATEGORY_COUNT_CACHE,
        cacheKey,
        new TypeReference<List<Object[]>>() {
        }
    ).orElseGet(() -> {
      try {
        List<Object[]> result = elasticsearchLinkCategoryRepository.countByTenantIdAndCategories(tenantId, categoryNames);
        cacheService.putInCache(CATEGORY_COUNT_CACHE, cacheKey, result);
        return result;
        } catch (Exception e) {
        logger.error("Error counting categories in Elasticsearch: {}", e.getMessage());
          throw e;
      }
    });
  }

  /**
   * Converts a LinkDocument to a LinkCategory entity (overloaded version without category name)
   *
   * @param doc the LinkDocument to convert
   * @return a new LinkCategory entity with copied properties
   */
  private LinkCategory convertToLinkCategory(LinkDocument doc) {
    String categoryName = doc.getCategories() != null && !doc.getCategories().isEmpty() ? doc.getCategories().get(0) : null;
    return convertToLinkCategory(doc, categoryName);
  }

  /**
   * Converts a LinkDocument to a LinkCategory entity
   *
   * @param doc the LinkDocument to convert
   * @param categoryName the specific category name to use
   * @return a new LinkCategory entity with copied properties
   */
  private LinkCategory convertToLinkCategory(LinkDocument doc, String categoryName) {
    LinkCategory linkCategory = new LinkCategory();
    linkCategory.setTenantId(doc.getTenantId());
    linkCategory.setCategory(categoryName);
    linkCategory.setRandomOrder(doc.getRandomOrder());
    linkCategory.setHd(doc.getHd());
    linkCategory.setTrailerFlag(doc.getTrailerPresent());
    linkCategory.setCreatedOn(doc.getCreatedOn() != null ? doc.getCreatedOn().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
    
    // Set Link object if needed
    Link link = new Link();
    
    // Safely convert linkId from String to Integer
    if (doc.getLinkId() != null && !doc.getLinkId().trim().isEmpty()) {
      try {
        link.setId(Integer.valueOf(doc.getLinkId()));
      } catch (NumberFormatException e) {
        logger.warn("Invalid linkId format: {}", doc.getLinkId());
        link.setId(null);
      }
    } else {
      link.setId(null);
    }
    
    link.setTenantId(doc.getTenantId());
    link.setLink(doc.getLink());
    link.setTitle(doc.getLinkTitle());
    link.setThumbnail(doc.getLinkThumbnail());
    link.setThumbpath(doc.getLinkThumbPath());
    link.setDuration(doc.getLinkDuration());
    link.setSource(doc.getLinkSource());
    link.setTrailer(doc.getLinkTrailer());
    linkCategory.setLink(link);
    return linkCategory;
  }

  /**
   * Generate a cache key
   *
   * @param tenantId the tenant ID
   * @param suffix the suffix
   * @return the cache key
   */
  private String generateCacheKey(Integer tenantId, String suffix) {
    return (tenantId != null ? tenantId + ":" : "") + suffix;
  }

  /**
   * Invalidate relevant caches for a LinkCategory
   *
   * @param linkCategory the LinkCategory
   */
  private void invalidateCaches(LinkCategory linkCategory) {
    if (linkCategory.getTenantId() != null) {
      // Invalidate tenant-specific caches
      cacheService.evictFromCache(LINK_CATEGORY_CACHE,
          generateCacheKey(linkCategory.getTenantId(), "*"));
      cacheService.evictFromCache(CATEGORY_COUNT_CACHE,
          generateCacheKey(linkCategory.getTenantId(), "*"));
      cacheService.evictFromCache(CATEGORIES_CACHE,
          generateCacheKey(linkCategory.getTenantId(), "*"));
    }

    if (linkCategory.getLinkId() != null) {
      // Invalidate link-specific caches
      cacheService.evictFromCache(LINK_CATEGORY_CACHE,
          generateCacheKey(null, "linkId:" + linkCategory.getLinkId()));
    }

    if (linkCategory.getCategory() != null) {
      // Invalidate category-specific caches
      cacheService.evictFromCache(LINK_CATEGORY_CACHE,
          generateCacheKey(linkCategory.getTenantId(), "category:" + linkCategory.getCategory()));
      cacheService.evictFromCache(CATEGORY_COUNT_CACHE,
          generateCacheKey(linkCategory.getTenantId(), "count:" + linkCategory.getCategory()));
    }
  }
}