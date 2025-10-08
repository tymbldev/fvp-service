package com.fvp.controller;

import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.dto.ModelWithLinkDTO;
import com.fvp.entity.AllCat;
import com.fvp.entity.Model;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.ModelRepository;
import com.fvp.service.CategoryService;
import com.fvp.service.ModelService;
import com.fvp.util.LoggingUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/similar")
public class SimilarContentController {

  private static final Logger logger = LoggingUtil.getLogger(SimilarContentController.class);

  private final AllCatRepository allCatRepository;
  private final ModelRepository modelRepository;
  private final CategoryService categoryService;
  private final ModelService modelService;

  // JVM Cache for similar content mappings
  private final Map<String, List<String>> similarCategoriesCache = new ConcurrentHashMap<>();
  private final Map<String, List<String>> similarModelsCache = new ConcurrentHashMap<>();

  @Autowired
  public SimilarContentController(
      AllCatRepository allCatRepository,
      ModelRepository modelRepository,
      CategoryService categoryService,
      ModelService modelService) {
    this.allCatRepository = allCatRepository;
    this.modelRepository = modelRepository;
    this.categoryService = categoryService;
    this.modelService = modelService;
  }

  @PostConstruct
  public void initializeSimilarContentCache() {
    logger.info("=== Starting similar content cache initialization ===");
    long startTime = System.currentTimeMillis();
    
    try {
      // Load all categories and build similar categories cache
      List<AllCat> allCategories = allCatRepository.findAll();
      logger.info("Found {} categories to process for similar content", allCategories.size());
      
      for (AllCat category : allCategories) {
        if (category.getSimContent() != null && !category.getSimContent().trim().isEmpty()) {
          String[] similarCategories = category.getSimContent().split("\\|\\|");
          List<String> similarCategoryList = Arrays.stream(similarCategories)
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .collect(Collectors.toList());
          
          if (!similarCategoryList.isEmpty()) {
            similarCategoriesCache.put(category.getName().toLowerCase(), similarCategoryList);
            logger.debug("Cached {} similar categories for category: {}", 
                similarCategoryList.size(), category.getName());
          }
        }
      }
      
      // Load all models and build similar models cache
      List<Model> allModels = modelRepository.findAll();
      logger.info("Found {} models to process for similar content", allModels.size());
      
      for (Model model : allModels) {
        if (model.getSimContent() != null && !model.getSimContent().trim().isEmpty()) {
          String[] similarModels = model.getSimContent().split("\\|\\|");
          List<String> similarModelList = Arrays.stream(similarModels)
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .collect(Collectors.toList());
          
          if (!similarModelList.isEmpty()) {
            similarModelsCache.put(model.getName().toLowerCase(), similarModelList);
            logger.debug("Cached {} similar models for model: {}", 
                similarModelList.size(), model.getName());
          }
        }
      }
      
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Similar content cache initialization completed in {} ms. " +
          "Cached {} category->category mappings and {} model->model mappings", 
          duration, similarCategoriesCache.size(), similarModelsCache.size());
      
    } catch (Exception e) {
      logger.error("Error initializing similar content cache: {}", e.getMessage(), e);
    }
  }

  /**
   * Get similar categories for a given category name
   * @param categoryName The category name to find similar categories for
   * @param tenantId The tenant ID
   * @return List of similar categories with link data
   */
  @GetMapping("/categories/{categoryName}")
  public ResponseEntity<List<CategoryWithLinkDTO>> getSimilarCategories(
      @PathVariable String categoryName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    
    logger.info("Received request for similar categories for category: {} with tenantId: {}", 
        categoryName, tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      // Get similar category names from cache
      List<String> similarCategoryNames = similarCategoriesCache.get(categoryName.toLowerCase());
      
      if (similarCategoryNames == null || similarCategoryNames.isEmpty()) {
        logger.info("No similar categories found for category: {}", categoryName);
        return ResponseEntity.ok(Collections.emptyList());
      }
      
      logger.info("Found {} similar categories for category: {}", 
          similarCategoryNames.size(), categoryName);
      
      // Get all categories with links for the tenant
      List<CategoryWithLinkDTO> allCategories = categoryService.getHomeCategoriesWithLinks(tenantId);
      
      // Filter to only include similar categories
      List<CategoryWithLinkDTO> similarCategories = allCategories.stream()
          .filter(category -> similarCategoryNames.contains(category.getName()))
          .collect(Collectors.toList());
      
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} similar categories for category: {} in {} ms", 
          similarCategories.size(), categoryName, duration);
      
      return ResponseEntity.ok(similarCategories);
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving similar categories for category: {} after {} ms: {}", 
          categoryName, duration, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Get similar models for a given model name
   * @param modelName The model name to find similar models for
   * @param tenantId The tenant ID
   * @return List of similar models with link data
   */
  @GetMapping("/models/{modelName}")
  public ResponseEntity<List<ModelWithLinkDTO>> getSimilarModels(
      @PathVariable String modelName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    
    logger.info("Received request for similar models for model: {} with tenantId: {}", 
        modelName, tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      // Get similar model names from cache
      List<String> similarModelNames = similarModelsCache.get(modelName.toLowerCase());
      
      if (similarModelNames == null || similarModelNames.isEmpty()) {
        logger.info("No similar models found for model: {}", modelName);
        return ResponseEntity.ok(Collections.emptyList());
      }
      
      logger.info("Found {} similar models for model: {}", 
          similarModelNames.size(), modelName);
      
      // Get all models for the tenant
      List<com.fvp.dto.ModelWithoutLinkDTO> allModels = modelService.getAllModels(tenantId);
      
      // Filter to only include similar models and convert to ModelWithLinkDTO
      List<ModelWithLinkDTO> similarModels = new ArrayList<>();
      
      for (String similarModelName : similarModelNames) {
        allModels.stream()
            .filter(model -> model.getName().equalsIgnoreCase(similarModelName))
            .findFirst()
            .ifPresent(model -> {
              // Convert ModelWithoutLinkDTO to ModelWithLinkDTO
              ModelWithLinkDTO modelWithLink = new ModelWithLinkDTO();
              modelWithLink.setId(model.getId());
              modelWithLink.setTenantId(model.getTenantId());
              modelWithLink.setName(model.getName());
              modelWithLink.setDescription(model.getDescription());
              modelWithLink.setCountry(model.getCountry());
              modelWithLink.setThumbnail(model.getThumbnail());
              modelWithLink.setThumbPath(model.getThumbPath());
              modelWithLink.setAge(model.getAge());
              
              // Get first link for this model
              try {
                ModelWithLinkDTO modelWithFirstLink = modelService.getModelFirstLink(tenantId, similarModelName);
                if (modelWithFirstLink != null) {
                  modelWithLink.setLink(modelWithFirstLink.getLink());
                  modelWithLink.setLinkTitle(modelWithFirstLink.getLinkTitle());
                  modelWithLink.setLinkThumbnail(modelWithFirstLink.getLinkThumbnail());
                  modelWithLink.setLinkThumbPath(modelWithFirstLink.getLinkThumbPath());
                  modelWithLink.setLinkDuration(modelWithFirstLink.getLinkDuration());
                  modelWithLink.setLinkId(modelWithFirstLink.getLinkId());
                  modelWithLink.setLinkSource(modelWithFirstLink.getLinkSource());
                  modelWithLink.setLinkTrailer(modelWithFirstLink.getLinkTrailer());
                }
              } catch (Exception e) {
                logger.warn("Error getting first link for model {}: {}", similarModelName, e.getMessage());
              }
              
              similarModels.add(modelWithLink);
            });
      }
      
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} similar models for model: {} in {} ms", 
          similarModels.size(), modelName, duration);
      
      return ResponseEntity.ok(similarModels);
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving similar models for model: {} after {} ms: {}", 
          modelName, duration, e.getMessage(), e);
      throw e;
    }
  }

}
