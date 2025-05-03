package com.fvp.repository;

import com.fvp.entity.LinkModelShard2;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkModelShard2Repository extends ShardedLinkModelRepository<LinkModelShard2> {

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_2 lm WHERE lm.tenant_id = :tenantId",
      nativeQuery = true)
  List<LinkModelShard2> findByTenantId(@Param("tenantId") Integer tenantId);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_2 lm WHERE lm.link_id = :linkId",
      nativeQuery = true)
  List<LinkModelShard2> findByLinkId(@Param("linkId") Integer linkId);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_2 lm WHERE lm.model = :model AND lm.tenant_id = :tenantId",
      nativeQuery = true)
  List<LinkModelShard2> findByModelAndTenantId(@Param("model") String model,
      @Param("tenantId") Integer tenantId);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_2 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkModelShard2> findByTenantIdAndModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_2 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY RAND() LIMIT 1",
      nativeQuery = true)
  Optional<LinkModelShard2> findRandomLinkByModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_2 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY lm.random_order",
      nativeQuery = true)
  List<LinkModelShard2> findByTenantIdAndModelOrderByRandomOrder(
      @Param("tenantId") Integer tenantId, @Param("model") String model);

  @Override
  @Query(value = "SELECT COUNT(lm.id) AS count FROM link_model_shard_2 lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1",
      nativeQuery = true)
  Long countByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

  @Override
  @Query(value = "SELECT lm.* FROM link_model_shard_2 lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
      "AND l.thumb_path_processed = 1 " +
      "GROUP BY lm.model " +
      "ORDER BY RAND()",
      nativeQuery = true)
  List<LinkModelShard2> findRandomLinksByModelNames(
      @Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames
  );

  @Override
  @Query(value =
      "SELECT lm.model, COUNT(lm.id) as count FROM link_model_shard_2 lm JOIN link l ON lm.link_id = l.id "
          +
          "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
          "AND l.thumb_path_processed = 1 " +
          "GROUP BY lm.model",
      nativeQuery = true)
  List<Object[]> countByTenantIdAndModels(
      @Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames
  );
} 