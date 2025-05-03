package com.fvp.service;

import com.fvp.entity.BaseLinkCategory;
import com.fvp.entity.LinkCategory;
import com.fvp.entity.LinkCategoryShard1;
import com.fvp.entity.LinkCategoryShard2;
import com.fvp.entity.LinkCategoryShard3;
import com.fvp.entity.LinkCategoryShard4;
import com.fvp.entity.LinkCategoryShard5;
import com.fvp.entity.LinkCategoryShard6;
import com.fvp.entity.LinkCategoryShard7;
import com.fvp.entity.LinkCategoryShard8;
import com.fvp.entity.LinkCategoryShard9;
import com.fvp.entity.LinkCategoryShard10;
import com.fvp.repository.LinkCategoryShard1Repository;
import com.fvp.repository.LinkCategoryShard2Repository;
import com.fvp.repository.LinkCategoryShard3Repository;
import com.fvp.repository.LinkCategoryShard4Repository;
import com.fvp.repository.LinkCategoryShard5Repository;
import com.fvp.repository.LinkCategoryShard6Repository;
import com.fvp.repository.LinkCategoryShard7Repository;
import com.fvp.repository.LinkCategoryShard8Repository;
import com.fvp.repository.LinkCategoryShard9Repository;
import com.fvp.repository.LinkCategoryShard10Repository;
import com.fvp.repository.ShardedLinkCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LinkCategoryShardingService {
    private static final Logger logger = LoggerFactory.getLogger(LinkCategoryShardingService.class);
    private static final int TOTAL_SHARDS = 10;
    
    // Cache for category-to-shard mapping
    private final Map<String, Integer> categoryShardMap = new ConcurrentHashMap<>();
    
    @Autowired
    private LinkCategoryShard1Repository shard1Repository;
    
    @Autowired
    private LinkCategoryShard2Repository shard2Repository;
    
    @Autowired
    private LinkCategoryShard3Repository shard3Repository;
    
    @Autowired
    private LinkCategoryShard4Repository shard4Repository;
    
    @Autowired
    private LinkCategoryShard5Repository shard5Repository;
    
    @Autowired
    private LinkCategoryShard6Repository shard6Repository;
    
    @Autowired
    private LinkCategoryShard7Repository shard7Repository;
    
    @Autowired
    private LinkCategoryShard8Repository shard8Repository;
    
    @Autowired
    private LinkCategoryShard9Repository shard9Repository;
    
    @Autowired
    private LinkCategoryShard10Repository shard10Repository;
    
    /**
     * Determines the shard number for a given category name using consistent hashing
     * @param category the category name
     * @return the shard number (1-10)
     */
    @Cacheable(value = "categoryShardMapping", key = "#category")
    public int getShardNumber(String category) {
        // First check the cache
        if (categoryShardMap.containsKey(category)) {
            return categoryShardMap.get(category);
        }
        
        // Calculate shard number based on category name hash
        int shardNumber = (Math.abs(category.hashCode()) % TOTAL_SHARDS) + 1;
        
        // Cache the result
        categoryShardMap.put(category, shardNumber);
        
        logger.debug("Category '{}' mapped to shard {}", category, shardNumber);
        return shardNumber;
    }
    
    /**
     * Gets the appropriate repository for a given category name
     * @param category the category name
     * @return the repository for the determined shard
     */
    public ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> getRepositoryForCategory(String category) {
        int shardNumber = getShardNumber(category);
        return getRepositoryForShard(shardNumber);
    }
    
    /**
     * Gets the repository for a specific shard number
     * @param shardNumber the shard number (1-10)
     * @return the repository for the specified shard
     */
    @SuppressWarnings("unchecked")
    public ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> getRepositoryForShard(int shardNumber) {
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
     * Converts a LinkCategory entity to its appropriate shard entity
     * @param source the source LinkCategory entity
     * @return the target shard entity
     */
    public BaseLinkCategory convertToShardEntity(LinkCategory source) {
        int shardNumber = getShardNumber(source.getCategory());
        BaseLinkCategory target = createShardEntity(shardNumber);
        
        // Copy properties
        target.setLinkId(source.getLinkId());
        target.setTenantId(source.getTenantId());
        target.setCategory(source.getCategory());
        target.setCreatedOn(source.getCreatedOn());
        target.setRandomOrder(source.getRandomOrder());
        
        return target;
    }
    
    /**
     * Creates a new instance of the appropriate shard entity
     * @param shardNumber the shard number (1-10)
     * @return a new instance of the appropriate shard entity
     */
    private BaseLinkCategory createShardEntity(int shardNumber) {
        switch (shardNumber) {
            case 1: return new LinkCategoryShard1();
            case 2: return new LinkCategoryShard2();
            case 3: return new LinkCategoryShard3();
            case 4: return new LinkCategoryShard4();
            case 5: return new LinkCategoryShard5();
            case 6: return new LinkCategoryShard6();
            case 7: return new LinkCategoryShard7();
            case 8: return new LinkCategoryShard8();
            case 9: return new LinkCategoryShard9();
            case 10: return new LinkCategoryShard10();
            default: throw new IllegalArgumentException("Invalid shard number: " + shardNumber);
        }
    }
    
    /**
     * Saves a BaseLinkCategory entity to its appropriate shard
     * @param entity the entity to save
     * @return the saved entity
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseLinkCategory> T save(T entity) {
        int shardNumber = getShardNumber(entity.getCategory());
        ShardedLinkCategoryRepository<T, Integer> repository = (ShardedLinkCategoryRepository<T, Integer>) getRepositoryForShard(shardNumber);
        return repository.save(entity);
    }
    
    /**
     * Deletes a BaseLinkCategory entity from its appropriate shard
     * @param entity the entity to delete
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseLinkCategory> void delete(T entity) {
        int shardNumber = getShardNumber(entity.getCategory());
        ShardedLinkCategoryRepository<T, Integer> repository = (ShardedLinkCategoryRepository<T, Integer>) getRepositoryForShard(shardNumber);
        repository.delete(entity);
    }
    
    /**
     * Finds all LinkCategory entities by category across all shards
     * @param categoryName the category name to find
     * @return a list of all matching entities from all shards
     */
    public List<BaseLinkCategory> findByCategory(String categoryName) {
        ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(getShardNumber(categoryName));
        return (List<BaseLinkCategory>) repository.findByCategoryAndTenantId(categoryName, null);
    }
    
    /**
     * Finds all LinkCategory entities by category and tenant ID
     * @param categoryName the category name to find
     * @param tenantId the tenant ID to find
     * @return a list of all matching entities
     */
    public List<BaseLinkCategory> findByCategoryAndTenantId(String categoryName, Integer tenantId) {
        ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(getShardNumber(categoryName));
        return (List<BaseLinkCategory>) repository.findByCategoryAndTenantId(categoryName, tenantId);
    }
    
    /**
     * Finds all LinkCategory entities by link ID across all shards
     * This requires querying all shards since we don't know which shard contains the link
     * @param linkId the link ID to find
     * @return a list of all matching entities from all shards
     */
    public List<BaseLinkCategory> findByLinkId(Integer linkId) {
        List<BaseLinkCategory> result = new ArrayList<>();
        
        for (int i = 1; i <= TOTAL_SHARDS; i++) {
            ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(i);
            result.addAll((List<BaseLinkCategory>) repository.findByLinkId(linkId));
        }
        
        return result;
    }
    
    /**
     * Deletes all LinkCategory entities by link ID across all shards
     * This requires querying all shards since we don't know which shard contains the link
     * @param linkId the link ID to delete
     */
    public void deleteByLinkId(Integer linkId) {
        for (int i = 1; i <= TOTAL_SHARDS; i++) {
            ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(i);
            repository.deleteByLinkId(linkId);
        }
    }
    
    /**
     * Gets the total number of shards
     * @return the total number of shards
     */
    public int getTotalShards() {
        return TOTAL_SHARDS;
    }
} 