package com.fvp.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;
import com.fvp.entity.BaseLinkCategory;

@NoRepositoryBean
public interface ShardedLinkCategoryRepository<T extends BaseLinkCategory, ID> extends JpaRepository<T, ID> {

  Optional<T> findRandomRecentLinkByCategory(Integer tenantId, String category, Long recentDays);

  Optional<T> findRandomLinkByCategory(Integer tenantId, String category);

  Long countByTenantIdAndCategory(Integer tenantId, String category);

  List<T> findByTenantIdAndLinkId(Integer tenantId, Integer linkId);

  List<String> findAllDistinctCategories(Integer tenantId);

  List<T> findByTenantId(Integer tenantId);

  List<T> findByTenantIdAndCategory(Integer tenantId, String category);

  List<T> findByTenantIdAndCategoryOrderByRandomOrder(Integer tenantId, String category);

  List<T> findByLinkId(Integer linkId);

  List<T> findByCategoryAndTenantId(String category, Integer tenantId);

  @Transactional
  void deleteByLinkId(Integer linkId);

  List<T> findByCategoryWithFiltersPageable(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality,
      int offset,
      int limit
  );

  Long countByCategoryWithFilters(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality
  );

  List<T> findByCategoryWithFiltersExcludingLinkPageable(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality,
      Integer excludeId,
      int offset,
      int limit
  );

  List<T> findRandomLinksByCategoryNames(Integer tenantId, List<String> categoryNames);

  List<Object[]> countByTenantIdAndCategories(Integer tenantId, List<String> categoryNames);
} 