package com.fvp.repository;

import com.fvp.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelRepository extends JpaRepository<Model, Integer> {
    
    Model findByTenantIdAndName(Integer tenantId, String name);
    
    @Query("SELECT m FROM Model m WHERE m.tenantId = :tenantId AND m.country = :country")
    List<Model> findByCountry(Integer tenantId, String country);
    
    @Query("SELECT m FROM Model m WHERE m.tenantId = :tenantId AND m.age >= :minAge AND m.age <= :maxAge")
    List<Model> findByAgeBetween(Integer tenantId, Integer minAge, Integer maxAge);
    
    @Query("SELECT DISTINCT m.country FROM Model m WHERE m.tenantId = :tenantId AND m.country IS NOT NULL")
    List<String> findAllDistinctCountries(Integer tenantId);
    
    List<Model> findByTenantId(Integer tenantId);
} 