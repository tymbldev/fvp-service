package com.fvp.service;

import com.fvp.entity.BaseLinkCategory;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.ShardedLinkCategoryRepository;
import com.fvp.util.ShardHashingUtil;
import com.fvp.util.SpringContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkCategoryShardingService {

  private static final Logger logger = LoggerFactory.getLogger(LinkCategoryShardingService.class);
  private static final int TOTAL_SHARDS = 50;
  private static final String CACHE_NAME = "categoryShardMapping";

  // Cache for category-to-shard mapping
  private final Map<String, Integer> categoryShardMap = new ConcurrentHashMap<>();

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private CacheManager cacheManager;

  /**
   * Determines the shard number for a given category name using consistent hashing
   *
   * @param category the category name
   * @return the shard number (1-50)
   */
  public int getShardNumber(String category) {
    category = category.toLowerCase();
    
    // First check the local cache
    if (categoryShardMap.containsKey(category)) {
      return categoryShardMap.get(category);
    }

    // Then check the Spring cache
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache != null) {
      Cache.ValueWrapper cachedValue = cache.get(category);
      if (cachedValue != null) {
        Integer shardNumber = (Integer) cachedValue.get();
        categoryShardMap.put(category, shardNumber);
        return shardNumber;
      }
    }

    // Calculate shard number if not found in caches
    int shardNumber = ShardHashingUtil.calculateShard(category.hashCode());
    shardNumber = ((shardNumber - 1) % TOTAL_SHARDS) + 1;

    // Store in both caches
    categoryShardMap.put(category, shardNumber);
    if (cache != null) {
      cache.put(category, shardNumber);
    }

    logger.debug("Category '{}' mapped to shard {}", category, shardNumber);
    return shardNumber;
  }

  /**
   * Gets the appropriate repository for a given category name
   *
   * @param category the category name
   * @return the repository for the determined shard
   */
  public ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> getRepositoryForCategory(
      String category) {
    int shardNumber = getShardNumber(category);
    return getRepositoryForShard(shardNumber);
  }

  /**
   * Gets the repository for a specific shard number
   *
   * @param shardNumber the shard number (11-50)
   * @return the repository for the specified shard
   */
  @SuppressWarnings("unchecked")
  public ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> getRepositoryForShard(
      int shardNumber) {
    try {
      String repositoryClassName = ShardHashingUtil.getRepositoryClassName(shardNumber, true);
      Class<?> repositoryClass = Class.forName(repositoryClassName);
      return (ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer>)
          SpringContext.getBean(repositoryClass);
    } catch (Exception e) {
      logger.error("Error getting repository for shard {}: {}", shardNumber, e.getMessage());
      throw new IllegalArgumentException("Invalid shard number: " + shardNumber, e);
    }
  }

  /**
   * Converts a LinkCategory entity to its appropriate shard entity
   *
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
    target.setHd(source.getHd());
    target.setTrailerFlag(source.getTrailerFlag());

    return target;
  }

  /**
   * Creates a new shard entity instance for the given shard number
   *
   * @param shardNumber the shard number (11-50)
   * @return a new instance of the appropriate shard entity
   */
  @SuppressWarnings("unchecked")
  private BaseLinkCategory createShardEntity(int shardNumber) {
    try {
      String entityClassName = ShardHashingUtil.getEntityClassName(shardNumber, true);
      Class<?> entityClass = Class.forName(entityClassName);
      return (BaseLinkCategory) entityClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      logger.error("Error creating shard entity for shard {}: {}", shardNumber, e.getMessage());
      throw new IllegalArgumentException("Invalid shard number: " + shardNumber, e);
    }
  }

  /**
   * Saves a BaseLinkCategory entity to its appropriate shard
   *
   * @param entity the entity to save
   * @return the saved entity
   */
  @Transactional
  public <T extends BaseLinkCategory> T save(T entity) {
    try {
      ShardedLinkCategoryRepository<T, Integer> repository =
          (ShardedLinkCategoryRepository<T, Integer>) getRepositoryForCategory(entity.getCategory());
      
      // Check if an entry already exists for this linkId and category using direct DB query
      List<T> existingEntities = repository.findByLinkIdAndCategory(entity.getLinkId(), entity.getCategory());
      
      if (!existingEntities.isEmpty()) {
        // Update existing entity
        T existing = existingEntities.get(0);
        existing.setTenantId(entity.getTenantId());
        existing.setCreatedOn(entity.getCreatedOn());
        existing.setRandomOrder(entity.getRandomOrder());
        existing.setHd(entity.getHd());
        existing.setTrailerFlag(entity.getTrailerFlag());
        return repository.save(existing);
      } else {
        // Save new entity
        return repository.save(entity);
      }
    } catch (Exception e) {
      logger.error("Error saving BaseLinkCategory for linkId {} and category {}: {}", 
          entity.getLinkId(), entity.getCategory(), e.getMessage(), e);
      throw new RuntimeException("Failed to save BaseLinkCategory", e);
    }
  }

  /**
   * Deletes a BaseLinkCategory entity from its appropriate shard
   *
   * @param entity the entity to delete
   */
  @Transactional
  public <T extends BaseLinkCategory> void delete(T entity) {
    ShardedLinkCategoryRepository<T, Integer> repository =
        (ShardedLinkCategoryRepository<T, Integer>) getRepositoryForCategory(entity.getCategory());
    repository.delete(entity);
  }

  /**
   * Saves multiple BaseLinkCategory entities to their appropriate shards
   *
   * @param entities the entities to save
   * @return the saved entities
   */
  @Transactional
  public <T extends BaseLinkCategory> List<T> saveAll(List<T> entities) {
    if (entities.isEmpty()) {
      return entities;
    }

    // Group entities by shard number
    Map<Integer, List<T>> entitiesByShard = new HashMap<>();
    for (T entity : entities) {
      int shardNumber = getShardNumber(entity.getCategory());
      entitiesByShard.computeIfAbsent(shardNumber, k -> new ArrayList<>()).add(entity);
    }

    // Save each group to its respective shard
    List<T> savedEntities = new ArrayList<>();
    for (Map.Entry<Integer, List<T>> entry : entitiesByShard.entrySet()) {
      int shardNumber = entry.getKey();
      List<T> shardEntities = entry.getValue();

      try {
        ShardedLinkCategoryRepository<T, Integer> repository =
            (ShardedLinkCategoryRepository<T, Integer>) getRepositoryForShard(shardNumber);
        savedEntities.addAll(repository.saveAll(shardEntities));
      } catch (Exception e) {
        logger.error("Error saving batch to shard {}: {}", shardNumber, e.getMessage());
        throw e;
      }
    }

    return savedEntities;
  }

  /**
   * Finds all LinkCategory entities by category across all shards
   *
   * @param categoryName the category name to find
   * @return a list of all matching entities from all shards
   */
  public List<BaseLinkCategory> findByCategory(String categoryName) {
    ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(
        getShardNumber(categoryName));
    return (List<BaseLinkCategory>) repository.findByCategoryAndTenantId(categoryName, null);
  }

  /**
   * Finds all LinkCategory entities by category and tenant ID
   *
   * @param categoryName the category name to find
   * @param tenantId the tenant ID to find
   * @return a list of all matching entities
   */
  public List<BaseLinkCategory> findByCategoryAndTenantId(String categoryName, Integer tenantId) {
    ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(
        getShardNumber(categoryName));
    return (List<BaseLinkCategory>) repository.findByCategoryAndTenantId(categoryName, tenantId);
  }

  /**
   * Finds all LinkCategory entities by link ID across all shards This requires querying all shards
   * since we don't know which shard contains the link
   *
   * @param linkId the link ID to find
   * @return a list of all matching entities from all shards
   */
  public List<BaseLinkCategory> findByLinkId(Integer linkId) {
    List<BaseLinkCategory> result = new ArrayList<>();

    for (int i = 11; i <= TOTAL_SHARDS; i++) {
      ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(
          i);
      result.addAll((List<BaseLinkCategory>) repository.findByLinkId(linkId));
    }

    return result;
  }

  /**
   * Deletes all LinkCategory entities by link ID across all shards This requires querying all
   * shards since we don't know which shard contains the link
   *
   * @param linkId the link ID to delete
   */
  public void deleteByLinkId(Integer linkId) {
    for (int i = 11; i <= TOTAL_SHARDS; i++) {
      ShardedLinkCategoryRepository<? extends BaseLinkCategory, Integer> repository = getRepositoryForShard(
          i);
      repository.deleteByLinkId(linkId);
    }
  }

  /**
   * Gets the total number of shards
   *
   * @return the total number of shards
   */
  public int getTotalShards() {
    return TOTAL_SHARDS;
  }

  /**
   * Saves a BaseLinkCategory entity to its appropriate shard using native SQL
   *
   * @param entity the entity to save
   * @return the saved entity
   */
  
  /**
   * Updates random_order in all link category shards by joining with the link table
   */
  @Transactional
  public void updateRandomOrderInAllShards() {
    logger.info("Starting to update random_order in all link category shards");
    for (int i = 1; i <= 50; i++) {
      try {
        String tableName = String.format("link_category_shard_%d", i);
        String sql = String.format(
            "UPDATE %s lc JOIN link l ON lc.link_id = l.id SET lc.random_order = l.random_order",
            tableName
        );
        logger.info("Executing SQL for shard {}: {}", i, sql);
        jdbcTemplate.execute(sql);
        logger.info("Successfully updated random_order in shard {}", i);
      } catch (Exception e) {
        logger.error("Error updating random_order in shard {}: {}", i, e.getMessage(), e);
        throw new RuntimeException("Failed to update random_order in shard " + i, e);
      }
    }
    logger.info("Completed updating random_order in all link category shards");
  }
} 