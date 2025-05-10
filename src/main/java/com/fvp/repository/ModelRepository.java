package com.fvp.repository;

import com.fvp.entity.Model;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, Integer> {

  List<Model> findByTenantId(Integer tenantId);

  /**
   * Find models by tenant ID where data is present
   * @param tenantId the tenant ID
   * @return list of models with data present
   */
  @Query(value = "SELECT m.* FROM model m FORCE INDEX (idx_tenant_data_present) WHERE m.tenant_id = :tenantId AND m.data_present = 1", nativeQuery = true)
  List<Model> findByTenantIdAndDataPresent(@Param("tenantId") Integer tenantId);

  Model findByTenantIdAndName(Integer tenantId, String name);

  @Query("SELECT m FROM Model m WHERE m.tenantId = :tenantId AND m.name IN :names")
  List<Model> findByTenantIdAndNameIn(@Param("tenantId") Integer tenantId,
      @Param("names") List<String> names);

  @Query("SELECT m FROM Model m WHERE m.tenantId = :tenantId AND m.country = :country")
  List<Model> findByCountry(Integer tenantId, String country);

  @Query("SELECT m FROM Model m WHERE m.tenantId = :tenantId AND m.age >= :minAge AND m.age <= :maxAge")
  List<Model> findByAgeBetween(Integer tenantId, Integer minAge, Integer maxAge);

  @Query("SELECT DISTINCT m.country FROM Model m WHERE m.tenantId = :tenantId AND m.country IS NOT NULL")
  List<String> findAllDistinctCountries(Integer tenantId);
} 