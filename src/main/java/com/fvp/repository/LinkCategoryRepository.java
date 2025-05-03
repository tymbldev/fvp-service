package com.fvp.repository;

import com.fvp.entity.LinkCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LinkCategoryRepository extends JpaRepository<LinkCategory, Integer> {
    // Methods needed by services despite migration comment
    
    @Query("SELECT lc FROM LinkCategory lc WHERE lc.linkId = :linkId")
    List<LinkCategory> findByLinkId(@Param("linkId") Integer linkId);
    
    @Query("SELECT lc FROM LinkCategory lc WHERE lc.tenantId = :tenantId")
    List<LinkCategory> findByTenantId(@Param("tenantId") Integer tenantId);
    
    @Query("SELECT COUNT(lc) AS count FROM LinkCategory lc WHERE lc.tenantId = :tenantId AND lc.category = :category")
    Long countByTenantIdAndCategory(@Param("tenantId") Integer tenantId, @Param("category") String category);
    
    @Query(value = "SELECT lc.* FROM link_category lc JOIN link l ON lc.link_id = l.id WHERE lc.tenant_id = :tenantId AND lc.category = :category AND l.thumb_path_processed = 1 ORDER BY RAND() LIMIT 1", 
        nativeQuery = true)
    Optional<LinkCategory> findRandomLinkByCategory(@Param("tenantId") Integer tenantId, @Param("category") String category);
    
    @Query(value = "SELECT lc.* FROM link_category lc JOIN link l ON lc.link_id = l.id WHERE lc.tenant_id = :tenantId AND lc.category IN :categoryNames AND l.thumb_path_processed = 1 GROUP BY lc.category ORDER BY RAND()", 
        nativeQuery = true)
    List<LinkCategory> findRandomLinksByCategoryNames(
        @Param("tenantId") Integer tenantId,
        @Param("categoryNames") List<String> categoryNames
    );
    
    @Query(value = "SELECT lc.category, COUNT(lc.id) as count FROM link_category lc WHERE lc.tenant_id = :tenantId AND lc.category IN :categories GROUP BY lc.category", 
        nativeQuery = true)
    List<Object[]> countByTenantIdAndCategories(
        @Param("tenantId") Integer tenantId,
        @Param("categories") List<String> categories
    );
    
    @Query("SELECT lc FROM LinkCategory lc WHERE lc.category = :category AND lc.tenantId IS NOT NULL")
    List<LinkCategory> findByCategoryAndTenantIdIsNotNull(@Param("category") String category);
    
    @Query(value = "SELECT lc.* FROM link_category lc WHERE lc.category = :category LIMIT :limit OFFSET :offset", 
        nativeQuery = true)
    List<LinkCategory> findCategoryChunk(
        @Param("category") String category,
        @Param("offset") int offset, 
        @Param("limit") int limit
    );

    @Modifying
    @Query("DELETE FROM LinkCategory lc WHERE lc.id IN :ids")
    @Transactional
    int deleteByIdIn(@Param("ids") List<Integer> ids);
} 