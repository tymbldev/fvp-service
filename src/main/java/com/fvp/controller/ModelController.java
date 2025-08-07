package com.fvp.controller;

import com.fvp.dto.ModelWithLinkDTO;
import com.fvp.dto.ModelLinksResponseDTO;
import com.fvp.dto.ModelWithoutLinkDTO;
import com.fvp.service.ModelService;
import com.fvp.service.ModelUtilService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
public class ModelController {

  private static final Logger logger = LoggerFactory.getLogger(ModelController.class);

  @Autowired
  private ModelService modelService;

  @Autowired
  private ModelUtilService modelUtilService;

  @GetMapping("/all")
  public ResponseEntity<List<ModelWithoutLinkDTO>> getHomeModels(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    logger.info("Received request for all models with tenantId: {}", tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      List<ModelWithoutLinkDTO> models = modelService.getAllModels(tenantId);
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} models for tenantId: {} in {} ms", 
          models.size(), tenantId, duration);
      return ResponseEntity.ok(models);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving all models for tenantId: {} after {} ms: {}", 
          tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }

  @GetMapping("/{modelName}/first")
  public ResponseEntity<ModelWithLinkDTO> getModelFirstLink(
      @PathVariable String modelName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    logger.info("Received request for first link of model: {} with tenantId: {}", modelName, tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      ModelWithLinkDTO model = modelService.getModelFirstLink(tenantId, modelName);
      long duration = System.currentTimeMillis() - startTime;
      if (model != null) {
        logger.info("Successfully retrieved first link for model: {} (tenantId: {}) in {} ms", 
            modelName, tenantId, duration);
      } else {
        logger.warn("No first link found for model: {} (tenantId: {}) in {} ms", 
            modelName, tenantId, duration);
      }
      return ResponseEntity.ok(model);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving first link for model: {} (tenantId: {}) after {} ms: {}", 
          modelName, tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }

  @GetMapping("/{modelName}/links")
  public ResponseEntity<Page<ModelLinksResponseDTO>> getModelLinks(
      @PathVariable String modelName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId,
      @RequestParam(required = false) Integer maxDuration,
      @RequestParam(required = false) String quality,
      @PageableDefault(size = 20, sort = "randomOrder") Pageable pageable) {
    logger.info("Received request for model links - model: {}, tenantId: {}, maxDuration: {}, quality: {}, page: {}, size: {}", 
        modelName, tenantId, maxDuration, quality, pageable.getPageNumber(), pageable.getPageSize());
    long startTime = System.currentTimeMillis();
    
    try {
      Page<ModelLinksResponseDTO> links = modelUtilService.getModelLinks(tenantId, modelName, pageable,
          maxDuration, quality);
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} model links for model: {} (tenantId: {}) in {} ms. Total elements: {}, total pages: {}", 
          links.getContent().size(), modelName, tenantId, duration, links.getTotalElements(), links.getTotalPages());
      return ResponseEntity.ok(links);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving model links for model: {} (tenantId: {}) after {} ms: {}", 
          modelName, tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }

}