package com.fvp.repository;

import com.fvp.entity.BaseLinkModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ShardedLinkModelRepository<T extends BaseLinkModel> extends JpaRepository<T, Integer> {
    
    @Query(value = "SELECT lm.* FROM #{#entityName} lm WHERE lm.tenant_id = :tenantId", 
        nativeQuery = true)
    List<T> findByTenantId(@Param("tenantId") Integer tenantId);
    
    @Query(value = "SELECT lm.* FROM #{#entityName} lm WHERE lm.link_id = :linkId", 
        nativeQuery = true)
    List<T> findByLinkId(@Param("linkId") Integer linkId);
    
    @Query(value = "SELECT lm.* FROM #{#entityName} lm WHERE lm.model = :model AND lm.tenant_id = :tenantId", 
        nativeQuery = true)
    List<T> findByModelAndTenantId(@Param("model") String model, @Param("tenantId") Integer tenantId);
    
    @Transactional
    void deleteByLinkId(Integer linkId);

    @Query(value = "SELECT lm.* FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1", 
        nativeQuery = true)
    List<T> findByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query(value = "SELECT lm.* FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY RAND() LIMIT 1", 
        nativeQuery = true)
    Optional<T> findRandomLinkByModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query(value = "SELECT lm.* FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1 ORDER BY lm.random_order", 
        nativeQuery = true)
    List<T> findByTenantIdAndModelOrderByRandomOrder(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query(value = "SELECT COUNT(lm.id) AS count FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id WHERE lm.tenant_id = :tenantId AND lm.model = :model AND l.thumb_path_processed = 1", 
        nativeQuery = true)
    Long countByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query(value = "SELECT lm.* FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id " +
        "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
        "AND l.thumb_path_processed = 1 " +
        "GROUP BY lm.model " +
        "ORDER BY RAND()", 
        nativeQuery = true)
    List<T> findRandomLinksByModelNames(
        @Param("tenantId") Integer tenantId,
        @Param("modelNames") List<String> modelNames
    );

    @Query(value = "SELECT lm.model, COUNT(lm.id) as count FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id " +
        "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
        "AND l.thumb_path_processed = 1 " +
        "GROUP BY lm.model", 
        nativeQuery = true)
    List<Object[]> countByTenantIdAndModels(
        @Param("tenantId") Integer tenantId,
        @Param("modelNames") List<String> modelNames
    );

    @Query(value = "SELECT COUNT(lm.id) AS count FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id " +
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

    @Query(value = "SELECT lm.* FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id " +
        "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
        "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
        "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
        "AND l.id != :excludeId " +
        "AND l.thumb_path_processed = 1 " +
        "ORDER BY lm.random_order LIMIT :limit OFFSET :offset", 
        nativeQuery = true)
    List<T> findByModelWithFiltersExcludingLinkPageable(
        @Param("tenantId") Integer tenantId,
        @Param("model") String model,
        @Param("maxDuration") Integer maxDuration,
        @Param("quality") String quality,
        @Param("excludeId") Integer excludeId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    @Query(value = "SELECT lm.* FROM #{#entityName} lm JOIN link l ON lm.link_id = l.id " +
        "WHERE lm.tenant_id = :tenantId AND lm.model = :model " +
        "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
        "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
        "AND l.thumb_path_processed = 1 " +
        "ORDER BY lm.random_order LIMIT :limit OFFSET :offset", 
        nativeQuery = true)
    List<T> findByModelWithFiltersPageable(
        @Param("tenantId") Integer tenantId,
        @Param("model") String model,
        @Param("maxDuration") Integer maxDuration,
        @Param("quality") String quality,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    @Query(value = "SELECT DISTINCT lm.model FROM #{#entityName} lm " +
        "WHERE lm.tenant_id = :tenantId", 
        nativeQuery = true)
    List<String> findAllDistinctModels(
        @Param("tenantId") Integer tenantId
    );
} 