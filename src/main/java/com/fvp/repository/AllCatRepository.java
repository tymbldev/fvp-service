package com.fvp.repository;

import com.fvp.entity.AllCat;
import com.sun.org.apache.xpath.internal.operations.Bool;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AllCatRepository extends JpaRepository<AllCat, Integer> {

  @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.homeThumb = true AND a.createdViaLink = false")
  List<AllCat> findAllHomeThumbCategories(Integer tenantId);

  @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.header = true AND a.createdViaLink = false")
  List<AllCat> findAllHeaderCategories(Integer tenantId);

  @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.homeSEO = true AND a.createdViaLink = false")
  List<AllCat> findAllHomeSEOCategories(Integer tenantId);

  @Query("SELECT a FROM AllCat a WHERE a.tenantId = :tenantId AND a.createdViaLink = false AND a.home = :home ORDER BY a.homeCatOrder ASC")
  List<AllCat> findByTenantIdAndHomeOrderByHomeCatOrder(Integer tenantId, Integer home);


  AllCat findByTenantIdAndName(Integer tenantId, String name);

  List<AllCat> findByTenantId(Integer tenantId);


  List<AllCat> findByTenantIdAndCreatedViaLink(Integer tenantId, Boolean createdViaLink);


  @Query("SELECT DISTINCT a.tenantId FROM AllCat a")
  List<Integer> findAllDistinctTenantIds();

  @Query("SELECT ac FROM AllCat ac WHERE ac.tenantId = :tenantId AND ac.name IN :names")
  List<AllCat> findByTenantIdAndNameIn(
      @Param("tenantId") Integer tenantId,
      @Param("names") List<String> names
  );
} 