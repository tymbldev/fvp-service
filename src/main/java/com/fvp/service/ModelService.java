package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.ModelWithLinkDTO;
import com.fvp.entity.Model;
import com.fvp.repository.ModelRepository;
import com.fvp.util.LoggingUtil;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelService {

  private static final Logger logger = LoggingUtil.getLogger(ModelService.class);
  private static final String MODEL_CACHE_PREFIX = "model_";
  private static final int CACHE_EXPIRY_MINUTES = 60;

  @Autowired
  private ModelRepository modelRepository;

  @Autowired
  private ModelUtilService modelUtilService;

  @Autowired
  private CacheService cacheService;

  public List<ModelWithLinkDTO> getAllModels(Integer tenantId) {
    String cacheKey = generateCacheKey(tenantId, "home");

    return LoggingUtil.logOperationTime(logger, "get home models", () -> {
      Optional<List<ModelWithLinkDTO>> cachedModels = cacheService.getCollectionFromCache(
          CacheService.CACHE_NAME_MODELS,
          cacheKey,
          new TypeReference<List<ModelWithLinkDTO>>() {
          }
      );
      if (cachedModels.isPresent()) {
        logger.info("Cache hit for home models for tenant: {}", tenantId);
        return cachedModels.get();
      }

      logger.info("Cache miss for home models for tenant: {}", tenantId);
      List<Model> models = modelRepository.findByTenantId(tenantId);
      List<ModelWithLinkDTO> modelDTOs = modelUtilService.getFirstLinksForModels(tenantId,
          models.stream().map(Model::getName).collect(Collectors.toList()));

      if (!modelDTOs.isEmpty()) {
        cacheService.putInCacheWithExpiry(CacheService.CACHE_NAME_MODELS, cacheKey, modelDTOs,
            CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
      }
      return modelDTOs;
    });
  }

  public ModelWithLinkDTO getModelFirstLink(Integer tenantId, String modelName) {
    String cacheKey = generateCacheKey(tenantId, "first_" + modelName);

    return LoggingUtil.logOperationTime(logger, "get model first link", () -> {
      Optional<ModelWithLinkDTO> cachedModel = cacheService.getFromCache(
          CacheService.CACHE_NAME_MODELS,
          cacheKey,
          ModelWithLinkDTO.class
      );
      if (cachedModel.isPresent()) {
        logger.info("Cache hit for model first link: {}", modelName);
        return cachedModel.get();
      }

      logger.info("Cache miss for model first link: {}", modelName);
      ModelWithLinkDTO model = modelUtilService.getModelWithFirstLink(tenantId, modelName);
      if (model != null) {
        cacheService.putInCacheWithExpiry(CacheService.CACHE_NAME_MODELS, cacheKey, model,
            CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
      }
      return model;
    });
  }

  private String generateCacheKey(Integer tenantId, String suffix) {
    return MODEL_CACHE_PREFIX + (tenantId != null ? tenantId + "_" : "") + suffix;
  }
} 