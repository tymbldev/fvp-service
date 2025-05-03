package com.fvp.service;

import com.fvp.entity.BaseLinkModel;
import com.fvp.entity.Link;
import com.fvp.entity.LinkModel;
import com.fvp.repository.LinkModelRepository;
import com.fvp.repository.ShardedLinkModelRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class LinkModelService {
    private static final Logger logger = LoggerFactory.getLogger(LinkModelService.class);
    private static final String LINK_MODEL_CACHE = "linkModel";
    private static final String MODEL_COUNT_CACHE = "modelCount";
    private static final String MODELS_CACHE = "models";
    private static final int CACHE_EXPIRY_MINUTES = 60;
    private static final int TOTAL_SHARDS = 10;
    
    @Autowired
    private LinkModelShardingService shardingService;
    
    @Autowired
    private CacheService cacheService;
    
    /**
     * Find all models for a tenant
     * @param tenantId the tenant ID
     * @return a list of distinct model names
     */
    public List<String> findAllDistinctModels(Integer tenantId) {
        String cacheKey = generateCacheKey(tenantId, "distinct");
        
        return cacheService.getCollectionFromCache(
            MODELS_CACHE,
            cacheKey,
            new TypeReference<List<String>>() {}
        ).orElseGet(() -> {
            List<String> result = new ArrayList<>();
            
            // We need to check all shards and combine the results
            for (int i = 1; i <= TOTAL_SHARDS; i++) {
                ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                    shardingService.getRepositoryForShard(i);
                
                List<String> models = repository.findAllDistinctModels(tenantId);
                result.addAll(models);
            }
            
            List<String> distinctModels = result.stream().distinct().collect(Collectors.toList());
            cacheService.putInCache(MODELS_CACHE, cacheKey, distinctModels);
            return distinctModels;
        });
    }
    
    /**
     * Find links by model name for a tenant
     * @param tenantId the tenant ID
     * @param modelName the model name
     * @return a list of matching LinkModel entities
     */
    public List<LinkModel> findByTenantIdAndModel(Integer tenantId, String modelName) {
        String cacheKey = generateCacheKey(tenantId, "model:" + modelName);
        
        return cacheService.getCollectionFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            new TypeReference<List<LinkModel>>() {}
        ).orElseGet(() -> {
            ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                shardingService.getRepositoryForModel(modelName);
            
            List<? extends BaseLinkModel> shardedModels = repository.findByTenantIdAndModel(tenantId, modelName);
            List<LinkModel> models = shardedModels.stream()
                .map(this::convertToLinkModel)
                .collect(Collectors.toList());
            cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, models);
            return models;
        });
    }
    
    /**
     * Find a random link by model for a tenant
     * @param tenantId the tenant ID
     * @param modelName the model name
     * @return an optional containing a random LinkModel if found
     */
    public Optional<LinkModel> findRandomLinkByModel(Integer tenantId, String modelName) {
        String cacheKey = generateCacheKey(tenantId, "random:" + modelName);
        
        Optional<LinkModel> cachedModel = cacheService.getFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            LinkModel.class
        );
        
        if (cachedModel.isPresent()) {
            return cachedModel;
        }
        
        ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
            shardingService.getRepositoryForModel(modelName);
        
        Optional<? extends BaseLinkModel> result = repository.findRandomLinkByModel(tenantId, modelName);
        Optional<LinkModel> linkModel = result.map(this::convertToLinkModel);
        linkModel.ifPresent(model -> cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, model));
        return linkModel;
    }
    
    /**
     * Find links by link ID
     * @param linkId the link ID
     * @return a list of matching LinkModel entities
     */
    public List<LinkModel> findByLinkId(Integer linkId) {
        String cacheKey = generateCacheKey(null, "linkId:" + linkId);
        
        return cacheService.getCollectionFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            new TypeReference<List<LinkModel>>() {}
        ).orElseGet(() -> {
            List<LinkModel> result = new ArrayList<>();
            
            // Since we don't know which shard contains this linkId, we need to check all shards
            for (int i = 1; i <= TOTAL_SHARDS; i++) {
                ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                    shardingService.getRepositoryForShard(i);
                
                List<? extends BaseLinkModel> entities = repository.findByLinkId(linkId);
                entities.forEach(entity -> result.add(convertToLinkModel(entity)));
            }
            
            cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, result);
            return result;
        });
    }
    
    /**
     * Count links by model for a tenant
     * @param tenantId the tenant ID
     * @param modelName the model name
     * @return the count of matching links
     */
    public Long countByTenantIdAndModel(Integer tenantId, String modelName) {
        String cacheKey = generateCacheKey(tenantId, "count:" + modelName);
        
        return cacheService.getFromCache(
            MODEL_COUNT_CACHE,
            cacheKey,
            Long.class
        ).orElseGet(() -> {
            Long count = shardingService.getRepositoryForModel(modelName)
                .countByTenantIdAndModel(tenantId, modelName);
            cacheService.putInCache(MODEL_COUNT_CACHE, cacheKey, count);
            return count;
        });
    }
    
    /**
     * Save a LinkModel entity
     * @param linkModel the entity to save
     * @return the saved entity
     */
    public LinkModel save(LinkModel linkModel) {
        // Try to save to the appropriate shard first
        try {
            BaseLinkModel shardEntity = shardingService.convertToShardEntity(linkModel);
            BaseLinkModel savedEntity = shardingService.save(shardEntity);
            
            // Update the original entity with the saved ID
            linkModel.setId(savedEntity.getId());
            
            // Invalidate relevant caches
            invalidateCaches(linkModel);
            
            return linkModel;
        } catch (Exception e) {
            logger.error("Error saving to sharded table: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Save multiple LinkModel entities
     * @param linkModels the entities to save
     * @return the saved entities
     */
    public List<LinkModel> saveAll(List<LinkModel> linkModels) {
        return linkModels.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }
    
    /**
     * Delete a LinkModel entity by link ID
     * @param linkId the link ID to delete
     */
    @Transactional
    public void deleteByLinkId(Integer linkId) {
        // Try to delete from sharded tables first
        try {
            shardingService.deleteByLinkId(linkId);
            
            // Invalidate relevant caches
            cacheService.evictFromCache(LINK_MODEL_CACHE, "linkId:" + linkId);
        } catch (Exception e) {
            logger.error("Error deleting from sharded tables: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Flush any pending changes to the database
     */
    public void flush() {
        // No-op since we're not using the original repository anymore
    }
    
    /**
     * Find random links by model names
     * @param tenantId the tenant ID
     * @param modelNames the list of model names
     * @return a list of LinkModel entities
     */
    public List<LinkModel> findRandomLinksByModelNames(Integer tenantId, List<String> modelNames) {
        String cacheKey = generateCacheKey(tenantId, "random:" + String.join(",", modelNames));
        
        return cacheService.getCollectionFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            new TypeReference<List<LinkModel>>() {}
        ).orElseGet(() -> {
            List<LinkModel> result = new ArrayList<>();
            
            // For each model name, find its shard and get random links
            for (String modelName : modelNames) {
                try {
                    ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                        shardingService.getRepositoryForModel(modelName);
                    
                    List<? extends BaseLinkModel> shardedModels = repository.findRandomLinksByModelNames(tenantId, Collections.singletonList(modelName));
                    result.addAll(shardedModels.stream()
                        .map(this::convertToLinkModel)
                        .collect(Collectors.toList()));
                } catch (Exception e) {
                    logger.error("Error finding random links for model {} in sharded tables: {}", 
                        modelName, e.getMessage());
                    throw e;
                }
            }
            
            cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, result);
            return result;
        });
    }
    
    /**
     * Count links by tenant ID and models
     * @param tenantId the tenant ID
     * @param modelNames the model names
     * @return a list of model counts
     */
    public List<Object[]> countByTenantIdAndModels(Integer tenantId, List<String> modelNames) {
        String cacheKey = generateCacheKey(tenantId, "modelCounts:" + String.join(",", modelNames));
        
        return cacheService.getCollectionFromCache(
            MODEL_COUNT_CACHE,
            cacheKey,
            new TypeReference<List<Object[]>>() {}
        ).orElseGet(() -> {
            List<Object[]> result = new ArrayList<>();
            
            // For each model name, find its shard and get counts
            for (String modelName : modelNames) {
                try {
                    ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                        shardingService.getRepositoryForModel(modelName);
                    
                    List<Object[]> shardedCounts = repository.countByTenantIdAndModels(tenantId, Collections.singletonList(modelName));
                    result.addAll(shardedCounts);
                } catch (Exception e) {
                    logger.error("Error counting models for {} in sharded tables: {}", 
                        modelName, e.getMessage());
                    throw e;
                }
            }
            
            cacheService.putInCache(MODEL_COUNT_CACHE, cacheKey, result);
            return result;
        });
    }
    
    /**
     * Find links by model with filters
     * @param tenantId the tenant ID
     * @param model the model name
     * @param maxDuration the maximum duration
     * @param quality the quality
     * @param offset the offset
     * @param limit the limit
     * @return a list of LinkModel entities
     */
    public List<LinkModel> findByModelWithFiltersPageable(
            Integer tenantId,
            String model,
            Integer maxDuration,
            String quality,
            int offset,
            int limit) {
        String cacheKey = generateCacheKey(tenantId, 
            String.format("modelFilters:%s:%d:%s:%d:%d", 
                model, maxDuration, quality, offset, limit));
        
        return cacheService.getCollectionFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            new TypeReference<List<LinkModel>>() {}
        ).orElseGet(() -> {
            ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                shardingService.getRepositoryForModel(model);
            
            List<? extends BaseLinkModel> shardedModels = repository.findByModelWithFiltersPageable(
                tenantId, model, maxDuration, quality, offset, limit);
            List<LinkModel> models = shardedModels.stream()
                .map(this::convertToLinkModel)
                .collect(Collectors.toList());
            cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, models);
            return models;
        });
    }
    
    /**
     * Find links by model with filters excluding a link
     * @param tenantId the tenant ID
     * @param model the model name
     * @param maxDuration the maximum duration
     * @param quality the quality
     * @param excludeId the link ID to exclude
     * @param offset the offset
     * @param limit the limit
     * @return a list of LinkModel entities
     */
    public List<LinkModel> findByModelWithFiltersExcludingLinkPageable(
            Integer tenantId,
            String model,
            Integer maxDuration,
            String quality,
            Integer excludeId,
            int offset,
            int limit) {
        String cacheKey = generateCacheKey(tenantId, 
            String.format("modelFiltersExclude:%s:%d:%s:%d:%d:%d", 
                model, maxDuration, quality, excludeId, offset, limit));
        
        return cacheService.getCollectionFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            new TypeReference<List<LinkModel>>() {}
        ).orElseGet(() -> {
            ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                shardingService.getRepositoryForModel(model);
            
            List<? extends BaseLinkModel> shardedModels = repository.findByModelWithFiltersExcludingLinkPageable(
                tenantId, model, maxDuration, quality, excludeId, offset, limit);
            List<LinkModel> models = shardedModels.stream()
                .map(this::convertToLinkModel)
                .collect(Collectors.toList());
            cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, models);
            return models;
        });
    }
    
    /**
     * Count links by model with filters
     * @param tenantId the tenant ID
     * @param model the model name
     * @param maxDuration the maximum duration
     * @param quality the quality
     * @return the count
     */
    public Long countByModelWithFilters(
            Integer tenantId,
            String model,
            Integer maxDuration,
            String quality) {
        String cacheKey = generateCacheKey(tenantId, 
            String.format("modelCountFilters:%s:%d:%s", 
                model, maxDuration, quality));
        
        return cacheService.getFromCache(
            MODEL_COUNT_CACHE,
            cacheKey,
            Long.class
        ).orElseGet(() -> {
            ShardedLinkModelRepository<? extends BaseLinkModel> repository = 
                shardingService.getRepositoryForModel(model);
            
            Long count = repository.countByModelWithFilters(
                tenantId, model, maxDuration, quality);
            cacheService.putInCache(MODEL_COUNT_CACHE, cacheKey, count);
            return count;
        });
    }
    
    /**
     * Convert a BaseLinkModel to a LinkModel entity
     * @param shardEntity the sharded entity
     * @return a LinkModel entity with properties copied from the sharded entity
     */
    private LinkModel convertToLinkModel(BaseLinkModel shardEntity) {
        LinkModel linkModel = new LinkModel();
        linkModel.setId(shardEntity.getId());
        linkModel.setLinkId(shardEntity.getLinkId());
        linkModel.setTenantId(shardEntity.getTenantId());
        linkModel.setModel(shardEntity.getModel());
        linkModel.setCreatedOn(shardEntity.getCreatedOn());
        linkModel.setRandomOrder(shardEntity.getRandomOrder());
        
        return linkModel;
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
     * Invalidate relevant caches for a LinkModel
     * @param linkModel the LinkModel
     */
    private void invalidateCaches(LinkModel linkModel) {
        if (linkModel.getTenantId() != null) {
            // Invalidate tenant-specific caches
            cacheService.evictFromCache(LINK_MODEL_CACHE, generateCacheKey(linkModel.getTenantId(), "*"));
            cacheService.evictFromCache(MODEL_COUNT_CACHE, generateCacheKey(linkModel.getTenantId(), "*"));
            cacheService.evictFromCache(MODELS_CACHE, generateCacheKey(linkModel.getTenantId(), "*"));
        }
        
        if (linkModel.getLinkId() != null) {
            // Invalidate link-specific caches
            cacheService.evictFromCache(LINK_MODEL_CACHE, generateCacheKey(null, "linkId:" + linkModel.getLinkId()));
        }
        
        if (linkModel.getModel() != null) {
            // Invalidate model-specific caches
            cacheService.evictFromCache(LINK_MODEL_CACHE, generateCacheKey(linkModel.getTenantId(), "model:" + linkModel.getModel()));
            cacheService.evictFromCache(MODEL_COUNT_CACHE, generateCacheKey(linkModel.getTenantId(), "count:" + linkModel.getModel()));
        }
    }
} 