package com.fvp.repository;

import com.fvp.entity.LinkCategoryShard42;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkCategoryShard42Repository extends
    ShardedLinkCategoryRepository<LinkCategoryShard42, Integer> {

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
      "AND lc.created_on >= DATE_SUB(NOW(), INTERVAL 3 DAY) " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY RAND() LIMIT 1",
      nativeQuery = true)
  Optional<LinkCategoryShard42> findRandomRecentLinkByCategory(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category
  );

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY RAND() LIMIT 1",
      nativeQuery = true)
  Optional<LinkCategoryShard42> findRandomLinkByCategory(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category
  );

  @Query(value =
      "SELECT COUNT(lc.id) AS count FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id "
          +
          "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
          "AND l.thumb_path_processed = 1",
      nativeQuery = true)
  Long countByTenantIdAndCategory(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category
  );

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId AND lc.link_id = :linkId " +
      "AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkCategoryShard42> findByTenantIdAndLinkId(
      @Param("tenantId") Integer tenantId,
      @Param("linkId") Integer linkId
  );

  @Query(value = "SELECT DISTINCT lc.category FROM link_category_shard_42 lc " +
      "WHERE lc.tenant_id = :tenantId",
      nativeQuery = true)
  List<String> findAllDistinctCategories(
      @Param("tenantId") Integer tenantId
  );

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
      "AND l.thumb_path_processed = 1",
      nativeQuery = true)
  List<LinkCategoryShard42> findByTenantIdAndCategory(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category
  );

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY lc.random_order",
      nativeQuery = true)
  List<LinkCategoryShard42> findByTenantIdAndCategoryOrderByRandomOrder(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category
  );

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
      "AND (:minDuration IS NULL OR l.duration >= :minDuration) " +
      "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
      "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY lc.random_order LIMIT :limit OFFSET :offset",
      nativeQuery = true)
  List<LinkCategoryShard42> findByCategoryWithFiltersPageable(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category,
      @Param("minDuration") Integer minDuration,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Query(value =
      "SELECT COUNT(lc.id) AS count FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id "
          +
          "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
          "AND (:minDuration IS NULL OR l.duration >= :minDuration) " +
          "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
          "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
          "AND l.thumb_path_processed = 1",
      nativeQuery = true)
  Long countByCategoryWithFilters(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category,
      @Param("minDuration") Integer minDuration,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality
  );

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId AND lc.category = :category " +
      "AND (:minDuration IS NULL OR l.duration >= :minDuration) " +
      "AND (:maxDuration IS NULL OR l.duration <= :maxDuration) " +
      "AND (:quality IS NULL OR :quality = '' OR l.quality = :quality) " +
      "AND l.id != :excludeId " +
      "AND l.thumb_path_processed = 1 " +
      "ORDER BY lc.random_order LIMIT :limit OFFSET :offset",
      nativeQuery = true)
  List<LinkCategoryShard42> findByCategoryWithFiltersExcludingLinkPageable(
      @Param("tenantId") Integer tenantId,
      @Param("category") String category,
      @Param("minDuration") Integer minDuration,
      @Param("maxDuration") Integer maxDuration,
      @Param("quality") String quality,
      @Param("excludeId") Integer excludeId,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Query(value = "SELECT lc.* FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id " +
      "WHERE lc.tenant_id = :tenantId " +
      "AND lc.category IN :categoryNames " +
      "AND l.thumb_path_processed = 1 " +
      "GROUP BY lc.category " +
      "ORDER BY RAND()",
      nativeQuery = true)
  List<LinkCategoryShard42> findRandomLinksByCategoryNames(
      @Param("tenantId") Integer tenantId,
      @Param("categoryNames") List<String> categoryNames
  );

  @Query(value =
      "SELECT lc.category, COUNT(lc.id) as count FROM link_category_shard_42 lc JOIN link l ON lc.link_id = l.id "
          +
          "WHERE lc.tenant_id = :tenantId AND lc.category IN :categoryNames " +
          "AND l.thumb_path_processed = 1 " +
          "GROUP BY lc.category",
      nativeQuery = true)
  List<Object[]> countByTenantIdAndCategories(
      @Param("tenantId") Integer tenantId,
      @Param("categoryNames") List<String> categoryNames
  );
}