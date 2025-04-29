package com.fvp.repository;

import com.fvp.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkRepository extends JpaRepository<Link, Integer> {
    
    @Query(value = "SELECT l.* FROM links l WHERE l.tenant_id = :tenantId ORDER BY RAND()", nativeQuery = true)
    List<Link> findAllOrderedByRandomOrder(@Param("tenantId") Integer tenantId);
    
    @Query(value = "SELECT l.* FROM links l WHERE l.tenant_id = :tenantId AND l.duration <= :maxDuration ORDER BY RAND()", nativeQuery = true)
    List<Link> findByDurationLessThanEqualOrderedByRandomOrder(@Param("tenantId") Integer tenantId, @Param("maxDuration") Integer maxDuration);
    
    Link findByTenantIdAndLink(Integer tenantId, String link);
    
    List<Link> findByTenantIdAndSource(Integer tenantId, String source);
    
    @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.title LIKE %:keyword%")
    List<Link> findByTitleContaining(Integer tenantId, String keyword);
    
    List<Link> findByTenantId(Integer tenantId);

    // Find a link by URL and tenant ID
    Link findByLinkAndTenantId(String link, Integer tenantId);

    /**
     * Find all links with pagination
     * @param offset Starting position
     * @param limit Maximum number of records to return
     * @return List of links
     */
    @Query(value = "SELECT l FROM Link l ORDER BY l.id")
    List<Link> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);
} 