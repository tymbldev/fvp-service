package com.fvp.repository;

import com.fvp.entity.AllCat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllCatRepository extends JpaRepository<AllCat, Integer> {
    
    @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.homeThumb = true")
    List<AllCat> findAllHomeThumbCategories(Integer tenantId);
    
    @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.header = true")
    List<AllCat> findAllHeaderCategories(Integer tenantId);
    
    @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.homeSEO = true")
    List<AllCat> findAllHomeSEOCategories(Integer tenantId);
    
    @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.home = :home ORDER BY a.homeCatOrder ASC")
    List<AllCat> findByTenantIdAndHomeOrderByHomeCatOrder(Integer tenantId, Integer home);
    
    AllCat findByTenantIdAndName(Integer tenantId, String name);
    
    List<AllCat> findByTenantId(Integer tenantId);
    
    @Query("SELECT DISTINCT a.tenantId FROM AllCat a")
    List<Integer> findAllDistinctTenantIds();
} 