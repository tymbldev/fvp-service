package com.fvp.repository;

import com.fvp.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkRepository extends JpaRepository<Link, Integer> {
    
    @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId ORDER BY l.randomOrder DESC")
    List<Link> findAllOrderedByRandomOrder(Integer tenantId);
    
    @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.duration <= :maxDuration ORDER BY l.randomOrder DESC")
    List<Link> findByDurationLessThanEqualOrderedByRandomOrder(Integer tenantId, Integer maxDuration);
    
    Link findByTenantIdAndLink(Integer tenantId, String link);
    
    List<Link> findByTenantIdAndSource(Integer tenantId, String source);
    
    @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.title LIKE %:keyword%")
    List<Link> findByTitleContaining(Integer tenantId, String keyword);
    
    List<Link> findByTenantId(Integer tenantId);
} 