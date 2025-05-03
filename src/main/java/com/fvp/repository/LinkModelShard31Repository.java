package com.fvp.repository;

import com.fvp.entity.LinkModelShard31;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LinkModelShard31Repository extends ShardedLinkModelRepository<LinkModelShard31> {

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm WHERE lm.tenant_id = :tenantId",
      nativeQuery = true)
  List<LinkModelShard31> findByTenantId(@Param("tenantId") Integer tenantId);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm WHERE lm.link_id = :linkId",
      nativeQuery = true)
  List<LinkModelShard31> findByLinkId(@Param("linkId") Integer linkId);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm WHERE lm.model = :model AND lm.tenant_id = :tenantId",
      nativeQuery = true)
  List<LinkModelShard31> findByModelAndTenantId(@Param("model") String model,
      @Param("tenantId") Integer tenantId);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkModelShard31> findByTenantIdAndModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY RAND() LIMIT 1",
      nativeQuery = true)
  Optional<LinkModelShard31> findRandomLinkByModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY lm.random_order",
      nativeQuery = true)
  List<LinkModelShard31> findByTenantIdAndModelOrderByRandomOrder(
      @Param("tenantId") Integer tenantId, @Param("model") String model);

  @Override
  @Query(value = "SELECT COUNT(lm.id) AS count FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1",
      nativeQuery = true)
  Long countByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
      "AND l.thumb_path_processed = 1 " +
      "GROUP BY lm.model " +
      "ORDER BY RAND()",
      nativeQuery = true)
  List<LinkModelShard31> findRandomLinksByModelNames(
      @Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames
  );

  @Override
  @Query(value =
      "SELECT lm.model, COUNT(lm.id) as count FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id "
          +
          "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
          "AND l.thumb_path_processed = 1 " +
          "GROUP BY lm.model",
      nativeQuery = true)
  List<Object[]> countByTenantIdAndModels(
      @Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames
  );

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
      "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY lm.random_order LIMIT :limit OFFSET :offset",
      nativeQuery = true)
  List<LinkModelShard31> findByModelWithFiltersPageable(
      @Param("tenantId") Integer tenantId,
      @Param("model") String model,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality,
      @Param("offset") int offset,
      @Param("limit") int limit
  );
  
  @Override
  @Query(value = "SELECT DISTINCT lm.model FROM link_model_shard_31 lm " +
      "JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId " +
      "AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<String> findAllDistinctModels(@Param("tenantId") Integer tenantId);
  
  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
      "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
      "AND l.thumb_path_processed = 1 " +
      "AND l.id != :excludeId " +
      "ORDER BY lm.random_order LIMIT :limit OFFSET :offset",
      nativeQuery = true)
  List<LinkModelShard31> findByModelWithFiltersExcludingLinkPageable(
      @Param("tenantId") Integer tenantId,
      @Param("model") String model,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality,
      @Param("excludeId") Integer excludeId,
      @Param("offset") int offset,
      @Param("limit") int limit
  );
  
  @Override
  @Query(value = "SELECT COUNT(lm.id) FROM link_model_shard_31 lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
      "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
      "AND l.thumb_path_processed = 1",
      nativeQuery = true)
  Long countByModelWithFilters(
      @Param("tenantId") Integer tenantId,
      @Param("model") String model,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality
  );
  
  @Override
  @Modifying
  @Transactional
  @Query(value = "DELETE FROM link_model_shard_31 WHERE link_id = :linkId", nativeQuery = true)
  void deleteByLinkId(@Param("linkId") Integer linkId);
}