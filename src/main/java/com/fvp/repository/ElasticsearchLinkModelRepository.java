package com.fvp.repository;

import com.fvp.document.ModelDocument;
import com.fvp.document.LinkDocument;
import java.util.List;
import java.util.Optional;

public interface ElasticsearchLinkModelRepository {
    Optional<ModelDocument> findRandomModelByTenantId(Integer tenantId);
    Optional<ModelDocument> findRandomModelByName(Integer tenantId, String modelName);
    Long countByTenantId(Integer tenantId);
    Long countByTenantIdAndModel(Integer tenantId, String modelName);
    List<ModelDocument> findByTenantId(Integer tenantId);
    List<ModelDocument> findByTenantIdAndModel(Integer tenantId, String modelName);
    List<ModelDocument> findByModelName(String modelName);
    List<LinkDocument> findByModelWithFiltersPageable(
        Integer tenantId,
        String modelName,
        Integer minAge,
        Integer maxAge,
        String country,
        int offset,
        int limit
    );
    Long countByModelWithFilters(
        Integer tenantId,
        String modelName,
        Integer minAge,
        Integer maxAge,
        String country
    );
    List<LinkDocument> findByModelWithFiltersExcludingModelPageable(
        Integer tenantId,
        String modelName,
        Integer minAge,
        Integer maxAge,
        String country,
        String excludeModelName,
        int offset,
        int limit
    );
    List<ModelDocument> findRandomModelsByNames(Integer tenantId, List<String> modelNames);
    List<Object[]> countByTenantIdAndModels(Integer tenantId, List<String> modelNames);
    List<String> findAllDistinctModels(Integer tenantId);
    void deleteByModelName(String modelName);
    Optional<LinkDocument> findRandomLinkByModel(Integer tenantId, String modelName);
    List<LinkDocument> findByModelWithFiltersExcludingLinkPageable(Integer tenantId, String modelName, Integer maxDuration, String quality, Integer excludeId, int offset, int limit);
    List<LinkDocument> findByModelWithFiltersPageable(Integer tenantId, String modelName, Integer maxDuration, String quality, int offset, int limit);
    Long countByModelWithFilters(Integer tenantId, String modelName, Integer maxDuration, String quality);
} 