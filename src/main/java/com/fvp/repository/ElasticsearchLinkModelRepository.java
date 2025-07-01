package com.fvp.repository;

import com.fvp.document.ModelDocument;
import com.fvp.document.LinkDocument;
import java.util.List;
import java.util.Optional;

public interface ElasticsearchLinkModelRepository {
    Optional<ModelDocument> findRandomModelByName(Integer tenantId, String modelName);
    Long countByTenantIdAndModel(Integer tenantId, String modelName);
    List<ModelDocument> findByTenantIdAndModel(Integer tenantId, String modelName);
    List<LinkDocument> findByModelWithFiltersPageable(
        Integer tenantId,
        String modelName,
        Integer minAge,
        Integer maxAge,
        String country,
        int offset,
        int limit
    );
    List<String> findAllDistinctModels(Integer tenantId);
    Optional<LinkDocument> findRandomLinkByModel(Integer tenantId, String modelName);
    List<LinkDocument> findByModelWithFiltersExcludingLinkPageable(Integer tenantId, String modelName, Integer maxDuration, String quality, Integer excludeId, int offset, int limit);
    List<LinkDocument> findByModelWithFiltersPageable(Integer tenantId, String modelName, Integer maxDuration, String quality, int offset, int limit);
    Long countByModelWithFilters(Integer tenantId, String modelName, Integer maxDuration, String quality);
} 