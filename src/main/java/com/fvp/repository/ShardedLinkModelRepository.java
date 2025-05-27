package com.fvp.repository;

import com.fvp.entity.BaseLinkModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface ShardedLinkModelRepository<T extends BaseLinkModel> extends
    JpaRepository<T, Integer> {

  List<T> findByTenantId(Integer tenantId);

  List<T> findByLinkId(Integer linkId);

  List<T> findByModelAndTenantId(String model, Integer tenantId);

  List<T> findByTenantIdAndModel(Integer tenantId, String model);

  Optional<T> findRandomLinkByModel(Integer tenantId, String model);

  List<T> findByTenantIdAndModelOrderByRandomOrder(Integer tenantId, String model);

  Long countByTenantIdAndModel(Integer tenantId, String model);

  List<T> findRandomLinksByModelNames(Integer tenantId, List<String> modelNames);

  List<Object[]> countByTenantIdAndModels(Integer tenantId, List<String> modelNames);

  List<T> findByLinkIdAndModel(Integer linkId, String model);

  @Transactional
  void deleteByLinkId(Integer linkId);

  Long countByModelWithFilters(Integer tenantId, String model, Integer maxDuration, String quality);

  List<T> findByModelWithFiltersExcludingLinkPageable(Integer tenantId, String model,
      Integer maxDuration, String quality, Integer excludeId, int offset, int limit);

  List<T> findByModelWithFiltersPageable(Integer tenantId, String model, Integer maxDuration,
      String quality, int offset, int limit);

  List<String> findAllDistinctModels(Integer tenantId);
} 