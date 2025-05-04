package com.fvp.service;

import com.fvp.entity.BaseLinkModel;
import com.fvp.entity.LinkModel;
import com.fvp.repository.ShardedLinkModelRepository;
import com.fvp.util.ShardHashingUtil;
import com.fvp.util.SpringContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkModelShardingService {

  private static final Logger logger = LoggerFactory.getLogger(LinkModelShardingService.class);
  private static final int TOTAL_SHARDS = 50;

  // Cache for model-to-shard mapping
  private final Map<String, Integer> modelShardMap = new ConcurrentHashMap<>();

  /**
   * Determines the shard number for a given model name using consistent hashing
   *
   * @param model the model name
   * @return the shard number (1-50)
   */
  public int getShardNumber(String model) {
    // First check the cache
    if (modelShardMap.containsKey(model)) {
      return modelShardMap.get(model);
    }

    // Use the ShardHashingUtil to calculate shard number
    int shardNumber = ShardHashingUtil.calculateShard(model.hashCode());

    // Ensure shard number is between 1 and 50
    shardNumber = ((shardNumber - 1) % TOTAL_SHARDS) + 1;

    // Cache the result
    modelShardMap.put(model, shardNumber);

    logger.debug("Model '{}' mapped to shard {}", model, shardNumber);
    return shardNumber;
  }

  /**
   * Gets the appropriate repository for a given model name
   *
   * @param model the model name
   * @return the repository for the determined shard
   */
  public ShardedLinkModelRepository<? extends BaseLinkModel> getRepositoryForModel(String model) {
    int shardNumber = getShardNumber(model);
    return getRepositoryForShard(shardNumber);
  }

  /**
   * Gets the repository for a specific shard number
   *
   * @param shardNumber the shard number (1-50)
   * @return the repository for the specified shard
   */
  @SuppressWarnings("unchecked")
  public ShardedLinkModelRepository<? extends BaseLinkModel> getRepositoryForShard(
      int shardNumber) {
    try {
      String repositoryClassName = ShardHashingUtil.getRepositoryClassName(shardNumber, false);
      Class<?> repositoryClass = Class.forName(repositoryClassName);
      return (ShardedLinkModelRepository<? extends BaseLinkModel>)
          SpringContext.getBean(repositoryClass);
    } catch (Exception e) {
      logger.error("Error getting repository for shard {}: {}", shardNumber, e.getMessage());
      throw new IllegalArgumentException("Invalid shard number: " + shardNumber, e);
    }
  }

  /**
   * Converts a LinkModel entity to its appropriate shard entity
   *
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
   * Creates a new shard entity instance for the given shard number
   *
   * @param shardNumber the shard number (1-50)
   * @return a new instance of the appropriate shard entity
   */
  @SuppressWarnings("unchecked")
  private BaseLinkModel createShardEntity(int shardNumber) {
    try {
      String entityClassName = ShardHashingUtil.getEntityClassName(shardNumber, false);
      Class<?> entityClass = Class.forName(entityClassName);
      return (BaseLinkModel) entityClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      logger.error("Error creating shard entity for shard {}: {}", shardNumber, e.getMessage());
      throw new IllegalArgumentException("Invalid shard number: " + shardNumber, e);
    }
  }

  /**
   * Saves a BaseLinkModel entity to its appropriate shard
   *
   * @param entity the entity to save
   * @return the saved entity
   */
  @Transactional
  public <T extends BaseLinkModel> T save(T entity) {
    ShardedLinkModelRepository<T> repository =
        (ShardedLinkModelRepository<T>) getRepositoryForModel(entity.getModel());
    return repository.save(entity);
  }

  /**
   * Saves multiple BaseLinkModel entities to their appropriate shards
   *
   * @param entities the entities to save
   * @return the saved entities
   */
  @Transactional
  public <T extends BaseLinkModel> List<T> saveAll(List<T> entities) {
    if (entities.isEmpty()) {
      return entities;
    }

    // Group entities by shard number
    Map<Integer, List<T>> entitiesByShard = new HashMap<>();
    for (T entity : entities) {
      int shardNumber = getShardNumber(entity.getModel());
      entitiesByShard.computeIfAbsent(shardNumber, k -> new ArrayList<>()).add(entity);
    }

    // Save each group to its respective shard
    List<T> savedEntities = new ArrayList<>();
    for (Map.Entry<Integer, List<T>> entry : entitiesByShard.entrySet()) {
      int shardNumber = entry.getKey();
      List<T> shardEntities = entry.getValue();

      try {
        ShardedLinkModelRepository<T> repository =
            (ShardedLinkModelRepository<T>) getRepositoryForShard(shardNumber);
        savedEntities.addAll(repository.saveAll(shardEntities));
      } catch (Exception e) {
        logger.error("Error saving batch to shard {}: {}", shardNumber, e.getMessage());
        throw e;
      }
    }

    return savedEntities;
  }

  /**
   * Finds all LinkModel entities by model across all shards
   *
   * @param modelName the model name to find
   * @return a list of all matching entities from all shards
   */
  public List<BaseLinkModel> findByModel(String modelName) {
    ShardedLinkModelRepository<? extends BaseLinkModel> repository = getRepositoryForShard(
        getShardNumber(modelName));
    return (List<BaseLinkModel>) repository.findByModelAndTenantId(modelName, null);
  }

  /**
   * Finds all LinkModel entities by model and tenant ID
   *
   * @param modelName the model name to find
   * @param tenantId the tenant ID to find
   * @return a list of all matching entities
   */
  public List<BaseLinkModel> findByModelAndTenantId(String modelName, Integer tenantId) {
    ShardedLinkModelRepository<? extends BaseLinkModel> repository = getRepositoryForShard(
        getShardNumber(modelName));
    return (List<BaseLinkModel>) repository.findByModelAndTenantId(modelName, tenantId);
  }

  /**
   * Finds all LinkModel entities by link ID across all shards This requires querying all shards
   * since we don't know which shard contains the link
   *
   * @param linkId the link ID to find
   * @return a list of all matching entities from all shards
   */
  public List<BaseLinkModel> findByLinkId(Integer linkId) {
    List<BaseLinkModel> result = new ArrayList<>();

    for (int i = 1; i <= TOTAL_SHARDS; i++) {
      ShardedLinkModelRepository<? extends BaseLinkModel> repository = getRepositoryForShard(i);
      result.addAll((List<BaseLinkModel>) repository.findByLinkId(linkId));
    }

    return result;
  }

  /**
   * Deletes all LinkModel entities by link ID across all shards This requires querying all shards
   * since we don't know which shard contains the link
   *
   * @param linkId the link ID to delete
   */
  public void deleteByLinkId(Integer linkId) {
    for (int i = 1; i <= TOTAL_SHARDS; i++) {
      ShardedLinkModelRepository<? extends BaseLinkModel> repository = getRepositoryForShard(i);
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
} 