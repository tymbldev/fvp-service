package com.fvp.service;

import com.fvp.entity.*;
import com.fvp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LinkModelShardingService {
    private static final Logger logger = LoggerFactory.getLogger(LinkModelShardingService.class);
    private static final int TOTAL_SHARDS = 10;
    
    // Cache for model-to-shard mapping
    private final Map<String, Integer> modelShardMap = new ConcurrentHashMap<>();
    
    @Autowired
    private LinkModelShard1Repository shard1Repository;
    
    @Autowired
    private LinkModelShard2Repository shard2Repository;
    
    @Autowired
    private LinkModelShard3Repository shard3Repository;
    
    @Autowired
    private LinkModelShard4Repository shard4Repository;
    
    @Autowired
    private LinkModelShard5Repository shard5Repository;
    
    @Autowired
    private LinkModelShard6Repository shard6Repository;
    
    @Autowired
    private LinkModelShard7Repository shard7Repository;
    
    @Autowired
    private LinkModelShard8Repository shard8Repository;
    
    @Autowired
    private LinkModelShard9Repository shard9Repository;
    
    @Autowired
    private LinkModelShard10Repository shard10Repository;
    
    /**
     * Determines the shard number for a given model name using consistent hashing
     * @param model the model name
     * @return the shard number (1-10)
     */
    @Cacheable(value = "modelShardMapping", key = "#model")
    public int getShardNumber(String model) {
        // First check the cache
        if (modelShardMap.containsKey(model)) {
            return modelShardMap.get(model);
        }
        
        // Calculate shard number based on model name hash
        int shardNumber = (Math.abs(model.hashCode()) % TOTAL_SHARDS) + 1;
        
        // Cache the result
        modelShardMap.put(model, shardNumber);
        
        logger.debug("Model '{}' mapped to shard {}", model, shardNumber);
        return shardNumber;
    }
    
    /**
     * Gets the appropriate repository for a given model name
     * @param model the model name
     * @return the repository for the determined shard
     */
    public ShardedLinkModelRepository<?> getRepositoryForModel(String model) {
        int shardNumber = getShardNumber(model);
        return getRepositoryForShard(shardNumber);
    }
    
    /**
     * Gets the repository for a specific shard number
     * @param shardNumber the shard number (1-10)
     * @return the repository for the specified shard
     */
    @SuppressWarnings("unchecked")
    public ShardedLinkModelRepository<?> getRepositoryForShard(int shardNumber) {
        switch (shardNumber) {
            case 1: return shard1Repository;
            case 2: return shard2Repository;
            case 3: return shard3Repository;
            case 4: return shard4Repository;
            case 5: return shard5Repository;
            case 6: return shard6Repository;
            case 7: return shard7Repository;
            case 8: return shard8Repository;
            case 9: return shard9Repository;
            case 10: return shard10Repository;
            default: throw new IllegalArgumentException("Invalid shard number: " + shardNumber);
        }
    }
    
    /**
     * Converts a LinkModel entity to its appropriate shard entity
     * @param source the source LinkModel entity
     * @return the target shard entity
     */
    public BaseLinkModel convertToShardEntity(LinkModel source) {
        int shardNumber = getShardNumber(source.getModel());
        BaseLinkModel target = createShardEntity(shardNumber);
        
        // Copy properties
        target.setLinkId(source.getLinkId());
        target.setTenantId(source.getTenantId());
        target.setModel(source.getModel());
        target.setCreatedOn(source.getCreatedOn());
        target.setRandomOrder(source.getRandomOrder());
        
        return target;
    }
    
    /**
     * Creates a new instance of the appropriate shard entity
     * @param shardNumber the shard number (1-10)
     * @return a new instance of the appropriate shard entity
     */
    private BaseLinkModel createShardEntity(int shardNumber) {
        switch (shardNumber) {
            case 1: return new LinkModelShard1();
            case 2: return new LinkModelShard2();
            case 3: return new LinkModelShard3();
            case 4: return new LinkModelShard4();
            case 5: return new LinkModelShard5();
            case 6: return new LinkModelShard6();
            case 7: return new LinkModelShard7();
            case 8: return new LinkModelShard8();
            case 9: return new LinkModelShard9();
            case 10: return new LinkModelShard10();
            default: throw new IllegalArgumentException("Invalid shard number: " + shardNumber);
        }
    }
    
    /**
     * Saves a BaseLinkModel entity to its appropriate shard
     * @param entity the entity to save
     * @return the saved entity
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseLinkModel> T save(T entity) {
        int shardNumber = getShardNumber(entity.getModel());
        ShardedLinkModelRepository<T> repository = (ShardedLinkModelRepository<T>) getRepositoryForShard(shardNumber);
        return repository.save(entity);
    }
    
    /**
     * Finds all LinkModel entities by model across all shards
     * @param modelName the model name to find
     * @return a list of all matching entities from all shards
     */
    public List<BaseLinkModel> findByModel(String modelName) {
        ShardedLinkModelRepository<?> repository = getRepositoryForShard(getShardNumber(modelName));
        return (List<BaseLinkModel>) repository.findByModelAndTenantId(modelName, null);
    }
    
    /**
     * Finds all LinkModel entities by model and tenant ID
     * @param modelName the model name to find
     * @param tenantId the tenant ID to find
     * @return a list of all matching entities
     */
    public List<BaseLinkModel> findByModelAndTenantId(String modelName, Integer tenantId) {
        ShardedLinkModelRepository<?> repository = getRepositoryForShard(getShardNumber(modelName));
        return (List<BaseLinkModel>) repository.findByModelAndTenantId(modelName, tenantId);
    }
    
    /**
     * Finds all LinkModel entities by link ID across all shards
     * This requires querying all shards since we don't know which shard contains the link
     * @param linkId the link ID to find
     * @return a list of all matching entities from all shards
     */
    public List<BaseLinkModel> findByLinkId(Integer linkId) {
        List<BaseLinkModel> result = new ArrayList<>();
        
        for (int i = 1; i <= TOTAL_SHARDS; i++) {
            ShardedLinkModelRepository<?> repository = getRepositoryForShard(i);
            result.addAll((List<BaseLinkModel>) repository.findByLinkId(linkId));
        }
        
        return result;
    }
    
    /**
     * Deletes all LinkModel entities by link ID across all shards
     * This requires querying all shards since we don't know which shard contains the link
     * @param linkId the link ID to delete
     */
    public void deleteByLinkId(Integer linkId) {
        for (int i = 1; i <= TOTAL_SHARDS; i++) {
            ShardedLinkModelRepository<?> repository = getRepositoryForShard(i);
            repository.deleteByLinkId(linkId);
        }
    }
} 