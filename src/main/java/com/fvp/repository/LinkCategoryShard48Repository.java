package com.fvp.repository;

import com.fvp.entity.LinkCategoryShard48;
import com.fvp.util.SpringContextUtil;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LinkCategoryShard48Repository extends
    ShardedLinkCategoryRepository<LinkCategoryShard48, Integer> {

  @Override
  default Optional<LinkCategoryShard48> findRandomRecentLinkByCategory(
      Integer tenantId,
      String category,
      Long recentDays
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findRandomRecentLinkByCategory(
          LinkCategoryShard48.class, tenantId, category, recentDays
      );
  }

  @Override
  default Optional<LinkCategoryShard48> findRandomLinkByCategory(
      Integer tenantId,
      String category
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findRandomLinkByCategory(
          LinkCategoryShard48.class, tenantId, category
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
          LinkCategoryShard48.class, tenantId, category
      );
  }

  @Override
  default List<LinkCategoryShard48> findByTenantIdAndLinkId(
      Integer tenantId,
      Integer linkId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantIdAndLinkId(
          LinkCategoryShard48.class, tenantId, linkId
      );
  }

  @Override
  default List<String> findAllDistinctCategories(
      Integer tenantId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findAllDistinctCategories(
          LinkCategoryShard48.class, tenantId
      );
  }

  @Override
  default List<LinkCategoryShard48> findByTenantId(
      Integer tenantId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantId(
          LinkCategoryShard48.class, tenantId
      );
  }

  @Override
  default List<LinkCategoryShard48> findByTenantIdAndCategory(
      Integer tenantId,
      String category
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantIdAndCategory(
          LinkCategoryShard48.class, tenantId, category
      );
  }

  @Override
  default List<LinkCategoryShard48> findByTenantIdAndCategoryOrderByRandomOrder(
      Integer tenantId,
      String category
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByTenantIdAndCategoryOrderByRandomOrder(
          LinkCategoryShard48.class, tenantId, category
      );
  }

  @Override
  default List<LinkCategoryShard48> findByLinkId(
      Integer linkId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByLinkId(
          LinkCategoryShard48.class, linkId
      );
  }

  @Override
  default List<LinkCategoryShard48> findByCategoryAndTenantId(
      String category,
      Integer tenantId
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findByCategoryAndTenantId(
          LinkCategoryShard48.class, category, tenantId
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
          LinkCategoryShard48.class, linkId
      );
  }

  @Override
  default List<LinkCategoryShard48> findRandomLinksByCategoryNames(
      Integer tenantId,
      List<String> categoryNames
  ) {
      // Get the query builder from Spring context
      LinkCategoryDynamicQueryBuilder queryBuilder = SpringContextUtil.getBean(LinkCategoryDynamicQueryBuilder.class);
      return queryBuilder.findRandomLinksByCategoryNames(
          LinkCategoryShard48.class, tenantId, categoryNames
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
          LinkCategoryShard48.class, tenantId, categoryNames
      );
  }

  /**
   * Default method implementation that uses the central dynamic query builder
   */
  @Override
  default List<LinkCategoryShard48> findByCategoryWithFiltersPageable(
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
          LinkCategoryShard48.class, tenantId, category, minDuration, maxDuration, quality, offset, limit
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
          LinkCategoryShard48.class, tenantId, category, minDuration, maxDuration, quality
      );
  }

  /**
   * Default method implementation that uses the central dynamic query builder
   */
  @Override
  default List<LinkCategoryShard48> findByCategoryWithFiltersExcludingLinkPageable(
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
          LinkCategoryShard48.class, tenantId, category, minDuration, maxDuration, quality, excludeId, offset, limit
      );
  }
}
