package com.fvp.repository;

import com.fvp.document.LinkDocument;
import java.util.List;
import java.util.Optional;

public interface ElasticsearchLinkCategoryRepository {
    Optional<LinkDocument> findRandomRecentLinkByCategory(Integer tenantId, String category, Long recentDays);
    Optional<LinkDocument> findRandomLinkByCategory(Integer tenantId, String category);
    Long countByTenantIdAndCategory(Integer tenantId, String category);
    List<LinkDocument> findByTenantIdAndLinkId(Integer tenantId, Integer linkId);
    List<String> findAllDistinctCategories(Integer tenantId);
    List<LinkDocument> findByTenantId(Integer tenantId);
    List<LinkDocument> findByTenantIdAndCategory(Integer tenantId, String category);
    List<LinkDocument> findByTenantIdAndCategoryOrderByRandomOrder(Integer tenantId, String category);
    List<LinkDocument> findByLinkId(Integer linkId);
    List<LinkDocument> findByCategoryAndTenantId(String category, Integer tenantId);
    List<LinkDocument> findByLinkIdAndCategory(Integer linkId, String category);
    void deleteByLinkId(Integer linkId);
    List<LinkDocument> findByCategoryWithFiltersPageable(
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
    List<LinkDocument> findByCategoryWithFiltersExcludingLinkPageable(
        Integer tenantId,
        String category,
        Integer minDuration,
        Integer maxDuration,
        String quality,
        Integer excludeId,
        int offset,
        int limit
    );
    List<LinkDocument> findRandomLinksByCategoryNames(Integer tenantId, List<String> categoryNames);
    List<Object[]> countByTenantIdAndCategories(Integer tenantId, List<String> categoryNames);
} 