package com.fvp.repository;

import com.fvp.entity.LinkModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LinkModelRepository extends JpaRepository<LinkModel, Integer> {

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkModel> findByTenantIdAndModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY RAND() LIMIT 1",
      nativeQuery = true)
  Optional<LinkModel> findRandomLinkByModel(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY lm.random_order",
      nativeQuery = true)
  List<LinkModel> findByTenantIdAndModelOrderByRandomOrder(@Param("tenantId") Integer tenantId,
      @Param("model") String model);

  @Query(value = "SELECT COUNT(lm.id) AS count FROM link_model lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1",
      nativeQuery = true)
  Long countByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkModel> findByTenantId(@Param("tenantId") Integer tenantId);

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id WHERE lm.link_id = :linkId AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkModel> findByLinkId(@Param("linkId") Integer linkId);

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id WHERE lm.model = :model AND lm.tenant_id = :tenantId AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkModel> findByModelAndTenantId(@Param("model") String model,
      @Param("tenantId") Integer tenantId);

  @Transactional
  void deleteByLinkId(Integer linkId);

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId " +
      "AND lm.model IN :modelNames " +
      "AND l.thumb_path_processed = 1 " +
      "GROUP BY lm.model " +
      "ORDER BY RAND()",
      nativeQuery = true)
  List<LinkModel> findRandomLinksByModelNames(
      @Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames
  );

  @Query(value =
      "SELECT lm.model, COUNT(lm.id) as count FROM link_model lm JOIN link l ON lm.link_id = l.id "
          +
          "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
          "AND l.thumb_path_processed = 1 " +
          "GROUP BY lm.model",
      nativeQuery = true)
  List<Object[]> countByTenantIdAndModels(
      @Param("tenantId") Integer tenantId,
      @Param("modelNames") List<String> modelNames
  );

  @Query(value = "SELECT COUNT(lm.id) AS count FROM link_model lm JOIN link l ON lm.link_id = l.id "
      +
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

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
      "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
      "AND l.id != :excludeId " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY lm.random_order LIMIT :limit OFFSET :offset",
      nativeQuery = true)
  List<LinkModel> findByModelWithFiltersExcludingLinkPageable(
      @Param("tenantId") Integer tenantId,
      @Param("model") String model,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality,
      @Param("excludeId") Integer excludeId,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Query(value = "SELECT lm.* FROM link_model lm JOIN link l ON lm.link_id = l.id " +
      "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
      "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
      "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY lm.random_order LIMIT :limit OFFSET :offset",
      nativeQuery = true)
  List<LinkModel> findByModelWithFiltersPageable(
      @Param("tenantId") Integer tenantId,
      @Param("model") String model,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Query("SELECT lm FROM LinkModel lm WHERE lm.model = :model AND lm.tenantId IS NOT NULL")
  List<LinkModel> findByModelAndTenantIdIsNotNull(@Param("model") String model);

  @Query(value = "SELECT lm.* FROM link_model lm WHERE lm.model = :model LIMIT :limit OFFSET :offset",
      nativeQuery = true)
  List<LinkModel> findModelChunk(
      @Param("model") String model,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Modifying
  @Query("DELETE FROM LinkModel lm WHERE lm.id IN :ids")
  int deleteByIdIn(@Param("ids") List<Integer> ids);
} 