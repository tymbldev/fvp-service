package com.fvp.service;

import com.fvp.document.LinkDocument;
import com.fvp.document.ModelDocument;
import com.fvp.repository.ElasticsearchLinkModelRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LinkModelService {

  private static final Logger logger = LoggerFactory.getLogger(LinkModelService.class);
  private static final String LINK_MODEL_CACHE = "linkModel";
  private static final String MODEL_COUNT_CACHE = "modelCount";
  private static final String MODELS_CACHE = "models";
  private static final int CACHE_EXPIRY_MINUTES = 60;

  @Autowired
  private ElasticsearchLinkModelRepository elasticsearchLinkModelRepository;

  @Autowired
  private CacheService cacheService;

  public LinkModelService(ElasticsearchLinkModelRepository elasticsearchLinkModelRepository,
      CacheService cacheService) {
    this.elasticsearchLinkModelRepository = elasticsearchLinkModelRepository;
    this.cacheService = cacheService;
  }

  /**
   * Find all models for a tenant
   *
   * @param tenantId the tenant ID
   * @return a list of distinct model names
   */
  public List<String> findAllDistinctModels(Integer tenantId) {
    String cacheKey = generateCacheKey(tenantId, "distinct");
    return cacheService.getCollectionFromCache(
            MODELS_CACHE,
            cacheKey,
            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            })
        .orElseGet(() -> {
          List<String> result = elasticsearchLinkModelRepository.findAllDistinctModels(tenantId);
          cacheService.putInCache(MODELS_CACHE, cacheKey, result);
          return result;
        });
  }

  /**
   * Find links by model name for a tenant
   *
   * @param tenantId the tenant ID
   * @param modelName the model name
   * @return a list of matching LinkModel entities
   */
  public List<ModelDocument> findByTenantIdAndModel(Integer tenantId, String modelName) {
    String cacheKey = generateCacheKey(tenantId, "model:" + modelName);
    return cacheService.getCollectionFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            new com.fasterxml.jackson.core.type.TypeReference<List<ModelDocument>>() {
            })
        .orElseGet(() -> {
          List<ModelDocument> models = elasticsearchLinkModelRepository.findByTenantIdAndModel(
              tenantId, modelName);
          cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, models);
          return models;
        });
  }

  /**
   * Find a random link by model for a tenant
   *
   * @param tenantId the tenant ID
   * @param modelName the model name
   * @return an optional containing a random LinkModel if found
   */
  public Optional<LinkDocument> findRandomLinkByModel(Integer tenantId, String modelName) {
    String cacheKey = generateCacheKey(tenantId, "random:" + modelName);
    Optional<LinkDocument> cachedModel = cacheService.getFromCache(
        LINK_MODEL_CACHE,
        cacheKey,
        LinkDocument.class
    );
    if (cachedModel.isPresent()) {
      return cachedModel;
    }
    Optional<LinkDocument> result = elasticsearchLinkModelRepository.findRandomLinkByModel(
        tenantId, modelName);
    result.ifPresent(model -> cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, model));
    return result;
  }

  /**
   * Count links by model for a tenant
   *
   * @param tenantId the tenant ID
   * @param modelName the model name
   * @return the count of matching links
   */
  public Long countByTenantIdAndModel(Integer tenantId, String modelName) {
    String cacheKey = generateCacheKey(tenantId, "count:" + modelName);
    return cacheService.getFromCache(
        MODEL_COUNT_CACHE,
        cacheKey,
        Long.class
    ).orElseGet(() -> {
      Long count = elasticsearchLinkModelRepository.countByTenantIdAndModel(tenantId, modelName);
      cacheService.putInCache(MODEL_COUNT_CACHE, cacheKey, count);
      return count;
    });
  }

  public List<LinkDocument> findByModelWithFiltersPageable(
      Integer tenantId,
      String modelName,
      Integer minAge,
      Integer maxAge,
      String country,
      int offset,
      int limit) {
    String cacheKey = generateCacheKey(tenantId,
        String.format("modelFilters:%s:%d:%d:%s:%d:%d",
            modelName, minAge, maxAge, country, offset, limit));
    return cacheService.getCollectionFromCache(
            LINK_MODEL_CACHE,
            cacheKey,
            new com.fasterxml.jackson.core.type.TypeReference<List<LinkDocument>>() {
            })
        .orElseGet(() -> {
          List<LinkDocument> models = elasticsearchLinkModelRepository.findByModelWithFiltersPageable(
              tenantId, modelName, minAge, maxAge, country, offset, limit);
          cacheService.putInCache(LINK_MODEL_CACHE, cacheKey, models);
          return models;
        });
  }

  private String generateCacheKey(Integer tenantId, String suffix) {
    return (tenantId != null ? tenantId + ":" : "") + suffix;
  }

}