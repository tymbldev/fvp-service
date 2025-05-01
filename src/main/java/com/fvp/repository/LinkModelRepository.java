package com.fvp.repository;

import com.fvp.entity.LinkModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkModelRepository extends JpaRepository<LinkModel, Integer> {

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1")
    List<LinkModel> findByTenantIdAndModel(Integer tenantId, String model);

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1 ORDER BY RAND()")
    Optional<LinkModel> findRandomLinkByModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1 ORDER BY lm.randomOrder")
    List<LinkModel> findByTenantIdAndModelOrderByRandomOrder(Integer tenantId, String model);

    @Query("SELECT COUNT(lm) FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1")
    Long countByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND l.thumbPathProcessed = 1")
    List<LinkModel> findByTenantId(Integer tenantId);
    
    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.linkId = :linkId AND l.thumbPathProcessed = 1")
    List<LinkModel> findByLinkId(Integer linkId);
    
    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.model = :model AND lm.tenantId = :tenantId AND l.thumbPathProcessed = 1")
    List<LinkModel> findByModelAndTenantId(String model, Integer tenantId);
    
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

    @Query(value = "SELECT lm.model, COUNT(lm.id) as count FROM link_model lm JOIN link l ON lm.link_id = l.id " +
        "WHERE lm.tenant_id = :tenantId AND lm.model IN :modelNames " +
        "AND l.thumb_path_processed = 1 " +
        "GROUP BY lm.model", 
        nativeQuery = true)
    List<Object[]> countByTenantIdAndModels(
        @Param("tenantId") Integer tenantId,
        @Param("modelNames") List<String> modelNames
    );

    @Query(value = "SELECT COUNT(lm.id) FROM link_model lm JOIN link l ON lm.link_id = l.id " +
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
} 