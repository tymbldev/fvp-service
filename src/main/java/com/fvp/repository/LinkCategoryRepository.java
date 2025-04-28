package com.fvp.repository;

import com.fvp.entity.LinkCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkCategoryRepository extends JpaRepository<LinkCategory, Integer> {
    
    @Query(value = "SELECT lc.* FROM link_categories lc WHERE lc.tenant_id = :tenantId AND lc.category = :category AND lc.created_on >= DATE_SUB(NOW(), INTERVAL 3 DAY) ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<LinkCategory> findRandomRecentLinkByCategory(
        @Param("tenantId") Integer tenantId, 
        @Param("category") String category
    );
    
    @Query(value = "SELECT lc.* FROM link_category lc WHERE lc.tenant_id = :tenantId AND lc.category = :category ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<LinkCategory> findRandomLinkByCategory(
        @Param("tenantId") Integer tenantId, 
        @Param("category") String category
    );
    
    @Query("SELECT COUNT(lc) FROM LinkCategory lc WHERE lc.tenantId = :tenantId AND lc.category = :category")
    Long countByTenantIdAndCategory(
        @Param("tenantId") Integer tenantId,
        @Param("category") String category
    );
    
    @Query("SELECT lc FROM LinkCategory lc WHERE lc.tenantId = :tenantId AND lc.link.id = :linkId")
    List<LinkCategory> findByTenantIdAndLinkId(Integer tenantId, Integer linkId);
    
    @Query("SELECT DISTINCT lc.category FROM LinkCategory lc WHERE lc.tenantId = :tenantId")
    List<String> findAllDistinctCategories(Integer tenantId);
    
    List<LinkCategory> findByTenantId(Integer tenantId);
    
    @Query("SELECT lc FROM LinkCategory lc WHERE lc.tenantId = :tenantId AND lc.category = :category")
    List<LinkCategory> findByTenantIdAndCategory(
        @Param("tenantId") Integer tenantId,
        @Param("category") String category
    );

    List<LinkCategory> findByTenantIdAndCategoryOrderByRandomOrder(Integer tenantId, String category);

    List<LinkCategory> findByLinkId(Integer linkId);
    
    List<LinkCategory> findByCategoryAndTenantId(String category, Integer tenantId);
    
    @Transactional
    void deleteByLinkId(Integer linkId);
    
    @Query(value = "SELECT lc.* FROM link_category lc JOIN link l ON lc.link_id = l.id " +
            "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
            "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
            "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
            "ORDER BY lc.random_order LIMIT :limit OFFSET :offset", 
            nativeQuery = true)
    List<LinkCategory> findByCategoryWithFiltersPageable(
        @Param("tenantId") Integer tenantId,
        @Param("category") String category,
        @Param("maxDuration") Integer maxDuration,
        @Param("quality") String quality,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    @Query(value = "SELECT COUNT(lc.id) FROM link_category lc JOIN link l ON lc.link_id = l.id " +
            "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
            "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
            "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality)",
            nativeQuery = true)
    Long countByCategoryWithFilters(
        @Param("tenantId") Integer tenantId,
        @Param("category") String category,
        @Param("maxDuration") Integer maxDuration,
        @Param("quality") String quality
    );

    @Query(value = "SELECT lc.* FROM link_category lc JOIN link l ON lc.link_id = l.id " +
            "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
            "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
            "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
            "AND l.id != :excludeId " +
            "ORDER BY lc.random_order LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<LinkCategory> findByCategoryWithFiltersExcludingLinkPageable(
        @Param("tenantId") Integer tenantId,
        @Param("category") String category,
        @Param("maxDuration") Integer maxDuration,
        @Param("quality") String quality,
        @Param("excludeId") Integer excludeId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
} 