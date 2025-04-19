package com.fvp.repository;

import com.fvp.entity.LinkCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkCategoryRepository extends JpaRepository<LinkCategory, Integer> {
    
    @Query(value = "SELECT lc.* FROM link_categories lc WHERE lc.tenant_id = :tenantId AND lc.category = :category AND lc.created_on >= DATE_SUB(NOW(), INTERVAL 3 DAY) ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<LinkCategory> findRandomRecentLinkByCategory(
        @Param("tenantId") Integer tenantId, 
        @Param("category") String category
    );
    
    @Query(value = "SELECT lc.* FROM link_categories lc WHERE lc.tenant_id = :tenantId AND lc.category = :category ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<LinkCategory> findRandomLinkByCategory(
        @Param("tenantId") Integer tenantId, 
        @Param("category") String category
    );
    
    @Query(value = "SELECT COUNT(*) FROM link_categories lc WHERE lc.tenant_id = :tenantId AND lc.category = :category", nativeQuery = true)
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
} 