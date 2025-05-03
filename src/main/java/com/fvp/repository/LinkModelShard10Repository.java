package com.fvp.repository;

import com.fvp.entity.LinkModelShard10;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkModelShard10Repository extends ShardedLinkModelRepository<LinkModelShard10> {

  @Override
  @Query(value = "SELECT * FROM link_model_shard_10 WHERE tenant_id = :tenantId", nativeQuery = true)
  List<LinkModelShard10> findByTenantId(@Param("tenantId") Integer tenantId);

  @Override
  @Query(value = "SELECT * FROM link_model_shard_10 WHERE link_id = :linkId", nativeQuery = true)
  List<LinkModelShard10> findByLinkId(@Param("linkId") Integer linkId);

  @Override
  @Query(value = "SELECT * FROM link_model_shard_10 WHERE model = :model AND tenant_id = :tenantId", nativeQuery = true)
  List<LinkModelShard10> findByModelAndTenantId(@Param("model") String model,
      @Param("tenantId") Integer tenantId);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_10 lm " +
      "JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND l.thumb_path_processed = true", nativeQuery = true)
  List<LinkModelShard10> findByTenantIdAndModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_10 lm " +
      "JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND l.thumb_path_processed = true " +
      "ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
  Optional<LinkModelShard10> findRandomLinkByModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_10 lm " +
      "JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND l.thumb_path_processed = true " +
      "ORDER BY lm.random_order", nativeQuery = true)
  List<LinkModelShard10> findByTenantIdAndModelOrderByRandomOrder(
      @Param("tenantId") Integer tenantId, @Param("model") String model);

  @Override
  @Query(value = "SELECT COUNT(*) FROM link_model_shard_10 lm " +
      "JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND l.thumb_path_processed = true", nativeQuery = true)
  Long countByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

  @Override
  @Query(value = "SELECT DISTINCT ON (lm.model) lm.* FROM link_model_shard_10 lm " +
      "JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
      "AND l.thumb_path_processed = true " +
      "ORDER BY lm.model, RANDOM()", nativeQuery = true)
  List<LinkModelShard10> findRandomLinksByModelNames(@Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames);

  @Override
  @Query(value = "SELECT model, COUNT(*) as count FROM link_model_shard_10 lm " +
      "JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
      "AND l.thumb_path_processed = true " +
      "GROUP BY model", nativeQuery = true)
  List<Object[]> countByTenantIdAndModels(@Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames);
} 