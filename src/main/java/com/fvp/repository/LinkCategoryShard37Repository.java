package com.fvp.repository;

import com.fvp.entity.LinkCategoryShard37;
import com.fvp.util.SpringContextUtil;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LinkCategoryShard37Repository extends
    ShardedLinkCategoryRepository<LinkCategoryShard37, Integer> {

  @Override
  default Optional<LinkCategoryShard37> findRandomRecentLinkByCategory(
      Integer tenantId,
      String category,
      Long recentDays
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findRandomRecentLinkByCategory(
          LinkCategoryShard37.class, tenantId, category, recentDays
      );
  }

  @Override
  default Optional<LinkCategoryShard37> findRandomLinkByCategory(
      Integer tenantId,
      String category
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findRandomLinkByCategory(
          LinkCategoryShard37.class, tenantId, category
      );
  }

  @Override
  default Long countByTenantIdAndCategory(
      Integer tenantId,
      String category
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.countByTenantIdAndCategory(
          LinkCategoryShard37.class, tenantId, category
      );
  }

  @Override
  default List<LinkCategoryShard37> findByTenantIdAndLinkId(
      Integer tenantId,
      Integer linkId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantIdAndLinkId(
          LinkCategoryShard37.class, tenantId, linkId
      );
  }

  @Override
  default List<String> findAllDistinctCategories(
      Integer tenantId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findAllDistinctCategories(
          LinkCategoryShard37.class, tenantId
      );
  }

  @Override
  default List<LinkCategoryShard37> findByTenantId(
      Integer tenantId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantId(
          LinkCategoryShard37.class, tenantId
      );
  }

  @Override
  default List<LinkCategoryShard37> findByTenantIdAndCategory(
      Integer tenantId,
      String category
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantIdAndCategory(
          LinkCategoryShard37.class, tenantId, category
      );
  }

  @Override
  default List<LinkCategoryShard37> findByTenantIdAndCategoryOrderByRandomOrder(
      Integer tenantId,
      String category
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantIdAndCategoryOrderByRandomOrder(
          LinkCategoryShard37.class, tenantId, category
      );
  }

  @Override
  default List<LinkCategoryShard37> findByLinkId(
      Integer linkId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByLinkId(
          LinkCategoryShard37.class, linkId
      );
  }

  @Override
  default List<LinkCategoryShard37> findByCategoryAndTenantId(
      String category,
      Integer tenantId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByCategoryAndTenantId(
          LinkCategoryShard37.class, category, tenantId
      );
  }

  @Override
  @Transactional
  default void deleteByLinkId(
      Integer linkId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      queryBuilder.deleteByLinkId(
          LinkCategoryShard37.class, linkId
      );
  }

  @Override
  default List<LinkCategoryShard37> findRandomLinksByCategoryNames(
      Integer tenantId,
      List<String> categoryNames
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findRandomLinksByCategoryNames(
          LinkCategoryShard37.class, tenantId, categoryNames
      );
  }

  @Override
  default List<Object[]> countByTenantIdAndCategories(
      Integer tenantId,
      List<String> categoryNames
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.countByTenantIdAndCategories(
          LinkCategoryShard37.class, tenantId, categoryNames
      );
  }

  /**
   * Default method implementation that uses the central dynamic query builder
   */
  @Override
  default List<LinkCategoryShard37> findByCategoryWithFiltersPageable(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality,
      int offset,
      int limit
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByCategoryWithDynamicFilters(
          LinkCategoryShard37.class, tenantId, category, minDuration, maxDuration, quality, offset, limit
      );
  }

  /**
   * Default method implementation that uses the central dynamic query builder
   */
  @Override
  default Long countByCategoryWithFilters(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.countByCategoryWithDynamicFilters(
          LinkCategoryShard37.class, tenantId, category, minDuration, maxDuration, quality
      );
  }

  /**
   * Default method implementation that uses the central dynamic query builder
   */
  @Override
  default List<LinkCategoryShard37> findByCategoryWithFiltersExcludingLinkPageable(
      Integer tenantId,
      String category,
      Integer minDuration,
      Integer maxDuration,
      String quality,
      Integer excludeId,
      int offset,
      int limit
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByCategoryWithDynamicFiltersExcludingLink(
          LinkCategoryShard37.class, tenantId, category, minDuration, maxDuration, quality, excludeId, offset, limit
      );
  }
}
