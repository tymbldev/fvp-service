package com.fvp.service;

import com.fvp.entity.BaseLinkCategory;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.ShardedLinkCategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LinkCategoryService {
    private static final Logger logger = LoggerFactory.getLogger(LinkCategoryService.class);
    private static final String LINK_CATEGORY_CACHE = "linkCategory";
    private static final String CATEGORY_COUNT_CACHE = "categoryCount";
    private static final String CATEGORIES_CACHE = "categories";
    private static final int CACHE_EXPIRY_MINUTES = 60;
    
    @Autowired
    private LinkCategoryRepository linkCategoryRepository;
    
    @Autowired
    private LinkCategoryShardingService shardingService;
    
    @Autowired
    private CacheService cacheService;
    
    /**
     * Saves a LinkCategory entity to the appropriate shard
     * @param linkCategory the LinkCategory entity to save
     * @return the saved entity converted back to LinkCategory
     */
    @Transactional
    public LinkCategory save(LinkCategory linkCategory) {
        // Try to save to the appropriate shard first
        try {
            BaseLinkCategory shardEntity = shardingService.convertToShardEntity(linkCategory);
            BaseLinkCategory savedEntity = shardingService.save(shardEntity);
            
            // Update the original entity with the saved ID
            linkCategory.setId(savedEntity.getId());
            
            // Invalidate relevant caches
            invalidateCaches(linkCategory);
            
            return linkCategory;
        } catch (Exception e) {
            logger.warn("Error saving to sharded table, falling back to original table: {}", e.getMessage());
        }
        
        // Fall back to original table
        return linkCategoryRepository.save(linkCategory);
    }
    
    /**
     * Deletes a LinkCategory entity from the appropriate shard
     * @param linkCategory the LinkCategory entity to delete
     */
    @Transactional
    public void delete(LinkCategory linkCategory) {
        BaseLinkCategory shardEntity = shardingService.convertToShardEntity(linkCategory);
        shardingService.delete(shardEntity);
        
        // Invalidate relevant caches
        invalidateCaches(linkCategory);
    }
    
    /**
     * Find a random recent link by category
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
            ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                shardingService.getRepositoryForCategory(category);
            
            Optional<? extends BaseLinkCategory> result = repository.findRandomRecentLinkByCategory(tenantId, category);
            Optional<LinkCategory> linkCategory = result.map(this::convertToLinkCategory);
            linkCategory.ifPresent(model -> cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, model));
            return linkCategory;
        } catch (Exception e) {
            logger.error("Error finding random recent link for category {} in sharded tables: {}", 
                category, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Find a random link by category
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
            ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                shardingService.getRepositoryForCategory(category);
            
            Optional<? extends BaseLinkCategory> result = repository.findRandomLinkByCategory(tenantId, category);
            Optional<LinkCategory> linkCategory = result.map(this::convertToLinkCategory);
            linkCategory.ifPresent(model -> cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, model));
            return linkCategory;
        } catch (Exception e) {
            logger.error("Error finding random link for category {} in sharded tables: {}", 
                category, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Count links by category for a tenant
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
                Long count = shardingService.getRepositoryForCategory(category)
                    .countByTenantIdAndCategory(tenantId, category);
                cacheService.putInCache(CATEGORY_COUNT_CACHE, cacheKey, count);
                return count;
            } catch (Exception e) {
                logger.error("Error counting links for category {} in sharded tables: {}", 
                    category, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Finds links by tenant ID and link ID
     * @param tenantId the tenant ID
     * @param linkId the link ID
     * @return a list of matching links
     */
    public List<LinkCategory> findByTenantIdAndLinkId(Integer tenantId, Integer linkId) {
        String cacheKey = generateCacheKey(tenantId, "linkId:" + linkId);
        
        return cacheService.getCollectionFromCache(
            LINK_CATEGORY_CACHE,
            cacheKey,
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            List<LinkCategory> result = new ArrayList<>();
            
            // Since we don't know which shard contains this linkId, we need to check all shards
            for (int i = 1; i <= shardingService.getTotalShards(); i++) {
                try {
                    ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                        shardingService.getRepositoryForShard(i);
                    
                    List<? extends BaseLinkCategory> entities = repository.findByTenantIdAndLinkId(tenantId, linkId);
                    entities.forEach(entity -> result.add(convertToLinkCategory(entity)));
                } catch (Exception e) {
                    logger.error("Error finding links for tenant {} and link {} in shard {}: {}", 
                        tenantId, linkId, i, e.getMessage());
                    throw e;
                }
            }
            
            cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, result);
            return result;
        });
    }
    
    /**
     * Finds all distinct categories for a tenant
     * @param tenantId the tenant ID
     * @return a list of distinct category names
     */
    public List<String> findAllDistinctCategories(Integer tenantId) {
        String cacheKey = generateCacheKey(tenantId, "distinct");
        
        return cacheService.getCollectionFromCache(
            CATEGORIES_CACHE,
            cacheKey,
            new TypeReference<List<String>>() {}
        ).orElseGet(() -> {
            List<String> result = new ArrayList<>();
            
            // We need to check all shards and combine the results
            for (int i = 1; i <= shardingService.getTotalShards(); i++) {
                try {
                    ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                        shardingService.getRepositoryForShard(i);
                    
                    List<String> categories = repository.findAllDistinctCategories(tenantId);
                    result.addAll(categories);
                } catch (Exception e) {
                    logger.error("Error finding distinct categories in shard {}: {}", 
                        i, e.getMessage());
                    throw e;
                }
            }
            
            List<String> distinctCategories = result.stream().distinct().collect(Collectors.toList());
            cacheService.putInCache(CATEGORIES_CACHE, cacheKey, distinctCategories);
            return distinctCategories;
        });
    }
    
    /**
     * Find links by link ID
     * @param linkId the link ID
     * @return a list of matching LinkCategory entities
     */
    public List<LinkCategory> findByLinkId(Integer linkId) {
        String cacheKey = generateCacheKey(null, "linkId:" + linkId);
        
        return cacheService.getCollectionFromCache(
            LINK_CATEGORY_CACHE,
            cacheKey,
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            List<LinkCategory> result = new ArrayList<>();
            
            // Since we don't know which shard contains this linkId, we need to check all shards
            for (int i = 1; i <= shardingService.getTotalShards(); i++) {
                try {
                    ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                        shardingService.getRepositoryForShard(i);
                    
                    List<? extends BaseLinkCategory> entities = repository.findByLinkId(linkId);
                    entities.forEach(entity -> result.add(convertToLinkCategory(entity)));
                } catch (Exception e) {
                    logger.error("Error finding links for link {} in shard {}: {}", 
                        linkId, i, e.getMessage());
                    throw e;
                }
            }
            
            cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, result);
            return result;
        });
    }
    
    /**
     * Save multiple LinkCategory entities
     * @param linkCategories the entities to save
     * @return the saved entities
     */
    public List<LinkCategory> saveAll(List<LinkCategory> linkCategories) {
        return linkCategories.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }
    
    /**
     * Delete a LinkCategory entity by link ID
     * @param linkId the link ID to delete
     */
    @Transactional
    public void deleteByLinkId(Integer linkId) {
        // Try to delete from sharded tables first
        try {
            shardingService.deleteByLinkId(linkId);
            
            // Invalidate relevant caches
            cacheService.evictFromCache(LINK_CATEGORY_CACHE, "linkId:" + linkId);
        } catch (Exception e) {
            logger.error("Error deleting from sharded tables: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Flush any pending changes to the database
     */
    public void flush() {
        linkCategoryRepository.flush();
    }
    
    /**
     * Find random links by category names
     * @param tenantId the tenant ID
     * @param categoryNames the list of category names
     * @return a list of LinkCategory entities
     */
    public List<LinkCategory> findRandomLinksByCategoryNames(Integer tenantId, List<String> categoryNames) {
        String cacheKey = generateCacheKey(tenantId, "random:" + String.join(",", categoryNames));
        
        return cacheService.getCollectionFromCache(
            LINK_CATEGORY_CACHE,
            cacheKey,
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            List<LinkCategory> result = new ArrayList<>();
            
            // For each category name, find its shard and get random links
            for (String categoryName : categoryNames) {
                try {
                    ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                        shardingService.getRepositoryForCategory(categoryName);
                    
                    List<? extends BaseLinkCategory> shardedCategories = repository.findRandomLinksByCategoryNames(tenantId, Collections.singletonList(categoryName));
                    List<LinkCategory> models = shardedCategories.stream()
                        .map(this::convertToLinkCategory)
                        .collect(Collectors.toList());
                    result.addAll(models);
                } catch (Exception e) {
                    logger.error("Error finding random links for category {} in sharded tables: {}", 
                        categoryName, e);
                    throw e;
                }
            }
            
            cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, result);
            return result;
        });
    }
    
    /**
     * Find links by tenant ID and category
     * @param tenantId the tenant ID
     * @param category the category name
     * @return a list of matching LinkCategory entities
     */
    public List<LinkCategory> findByTenantIdAndCategory(Integer tenantId, String category) {
        String cacheKey = generateCacheKey(tenantId, "category:" + category);
        
        return cacheService.getCollectionFromCache(
            LINK_CATEGORY_CACHE,
            cacheKey,
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            try {
                ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                    shardingService.getRepositoryForCategory(category);
                
                List<? extends BaseLinkCategory> shardedCategories = repository.findByTenantIdAndCategory(tenantId, category);
                List<LinkCategory> models = shardedCategories.stream()
                    .map(this::convertToLinkCategory)
                    .collect(Collectors.toList());
                cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
                return models;
            } catch (Exception e) {
                logger.error("Error finding links for category {} in sharded tables: {}", 
                    category, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Find links by tenant ID and category ordered by random order
     * @param tenantId the tenant ID
     * @param category the category name
     * @return a list of matching LinkCategory entities
     */
    public List<LinkCategory> findByTenantIdAndCategoryOrderByRandomOrder(Integer tenantId, String category) {
        String cacheKey = generateCacheKey(tenantId, "categoryOrder:" + category);
        
        return cacheService.getCollectionFromCache(
            LINK_CATEGORY_CACHE,
            cacheKey,
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            try {
                ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                    shardingService.getRepositoryForCategory(category);
                
                List<? extends BaseLinkCategory> shardedCategories = repository.findByTenantIdAndCategoryOrderByRandomOrder(tenantId, category);
                List<LinkCategory> models = shardedCategories.stream()
                    .map(this::convertToLinkCategory)
                    .collect(Collectors.toList());
                cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
                return models;
            } catch (Exception e) {
                logger.error("Error finding links for category {} in sharded tables: {}", 
                    category, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Find links by category and tenant ID
     * @param category the category name
     * @param tenantId the tenant ID
     * @return a list of matching LinkCategory entities
     */
    public List<LinkCategory> findByCategoryAndTenantId(String category, Integer tenantId) {
        String cacheKey = generateCacheKey(tenantId, "category:" + category);
        
        return cacheService.getCollectionFromCache(
            LINK_CATEGORY_CACHE,
            cacheKey,
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            try {
                ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                    shardingService.getRepositoryForCategory(category);
                
                List<? extends BaseLinkCategory> shardedCategories = repository.findByCategoryAndTenantId(category, tenantId);
                List<LinkCategory> models = shardedCategories.stream()
                    .map(this::convertToLinkCategory)
                    .collect(Collectors.toList());
                cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
                return models;
            } catch (Exception e) {
                logger.error("Error finding links for category {} in sharded tables: {}", 
                    category, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Find links by category with filters
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
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            try {
                ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                    shardingService.getRepositoryForCategory(category);
                
                List<? extends BaseLinkCategory> shardedCategories = repository.findByCategoryWithFiltersPageable(
                    tenantId, category, minDuration, maxDuration, quality, offset, limit);
                List<LinkCategory> models = shardedCategories.stream()
                    .map(this::convertToLinkCategory)
                    .collect(Collectors.toList());
                cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
                return models;
            } catch (Exception e) {
                logger.error("Error finding links for category {} with filters in sharded tables: {}", 
                    category, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Count links by category with filters
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
                ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                    shardingService.getRepositoryForCategory(category);
                
                Long count = repository.countByCategoryWithFilters(
                    tenantId, category, minDuration, maxDuration, quality);
                cacheService.putInCache(CATEGORY_COUNT_CACHE, cacheKey, count);
                return count;
            } catch (Exception e) {
                logger.error("Error counting links for category {} with filters in sharded tables: {}", 
                    category, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Find links by category with filters excluding a link
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
            new TypeReference<List<LinkCategory>>() {}
        ).orElseGet(() -> {
            try {
                ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                    shardingService.getRepositoryForCategory(category);
                
                List<? extends BaseLinkCategory> shardedCategories = repository.findByCategoryWithFiltersExcludingLinkPageable(
                    tenantId, category, minDuration, maxDuration, quality, excludeId, offset, limit);
                List<LinkCategory> models = shardedCategories.stream()
                    .map(this::convertToLinkCategory)
                    .collect(Collectors.toList());
                cacheService.putInCache(LINK_CATEGORY_CACHE, cacheKey, models);
                return models;
            } catch (Exception e) {
                logger.error("Error finding links for category {} with filters excluding link {} in sharded tables: {}", 
                    category, excludeId, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Count links by tenant ID and categories
     * @param tenantId the tenant ID
     * @param categoryNames the category names
     * @return a list of model counts
     */
    public List<Object[]> countByTenantIdAndCategories(Integer tenantId, List<String> categoryNames) {
        String cacheKey = generateCacheKey(tenantId, "categoryCounts:" + String.join(",", categoryNames));
        
        return cacheService.getCollectionFromCache(
            CATEGORY_COUNT_CACHE,
            cacheKey,
            new TypeReference<List<Object[]>>() {}
        ).orElseGet(() -> {
            List<Object[]> result = new ArrayList<>();
            
            // For each category name, find its shard and get counts
            for (String categoryName : categoryNames) {
                try {
                    ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = 
                        shardingService.getRepositoryForCategory(categoryName);
                    
                    List<Object[]> shardedCounts = repository.countByTenantIdAndCategories(tenantId, Collections.singletonList(categoryName));
                    result.addAll(shardedCounts);
                } catch (Exception e) {
                    logger.error("Error counting categories for {} in sharded tables: {}", 
                        categoryName, e.getMessage());
                    throw e;
                }
            }
            
            cacheService.putInCache(CATEGORY_COUNT_CACHE, cacheKey, result);
            return result;
        });
    }
    
    /**
     * Converts a BaseLinkCategory entity to a LinkCategory entity
     * @param entity the BaseLinkCategory entity to convert
     * @return a new LinkCategory entity with copied properties
     */
    private LinkCategory convertToLinkCategory(BaseLinkCategory entity) {
        LinkCategory linkCategory = new LinkCategory();
        BeanUtils.copyProperties(entity, linkCategory);
        return linkCategory;
    }
    
    /**
     * Generate a cache key
     * @param tenantId the tenant ID
     * @param suffix the suffix
     * @return the cache key
     */
    private String generateCacheKey(Integer tenantId, String suffix) {
        return (tenantId != null ? tenantId + ":" : "") + suffix;
    }
    
    /**
     * Invalidate relevant caches for a LinkCategory
     * @param linkCategory the LinkCategory
     */
    private void invalidateCaches(LinkCategory linkCategory) {
        if (linkCategory.getTenantId() != null) {
            // Invalidate tenant-specific caches
            cacheService.evictFromCache(LINK_CATEGORY_CACHE, generateCacheKey(linkCategory.getTenantId(), "*"));
            cacheService.evictFromCache(CATEGORY_COUNT_CACHE, generateCacheKey(linkCategory.getTenantId(), "*"));
            cacheService.evictFromCache(CATEGORIES_CACHE, generateCacheKey(linkCategory.getTenantId(), "*"));
        }
        
        if (linkCategory.getLinkId() != null) {
            // Invalidate link-specific caches
            cacheService.evictFromCache(LINK_CATEGORY_CACHE, generateCacheKey(null, "linkId:" + linkCategory.getLinkId()));
        }
        
        if (linkCategory.getCategory() != null) {
            // Invalidate category-specific caches
            cacheService.evictFromCache(LINK_CATEGORY_CACHE, generateCacheKey(linkCategory.getTenantId(), "category:" + linkCategory.getCategory()));
            cacheService.evictFromCache(CATEGORY_COUNT_CACHE, generateCacheKey(linkCategory.getTenantId(), "count:" + linkCategory.getCategory()));
        }
    }
}