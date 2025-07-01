package com.fvp.controller;

import com.fvp.service.CacheService;
import com.fvp.service.LinkProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

  private final CacheService cacheService;
  private final CategoryController categoryController;
  private final LinkProcessingService linkProcessingService;
  private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

  public CacheController(
      CacheService cacheService, 
      CategoryController categoryController,
      LinkProcessingService linkProcessingService) {
    this.cacheService = cacheService;
    this.categoryController = categoryController;
    this.linkProcessingService = linkProcessingService;
  }



  @GetMapping("/clear")
  public ResponseEntity<String> clearAllCache() {
    try {

      logger.info("Successfully updated random_order in link table");
      // Trigger cache build
      logger.info("Triggering cache build...");
      buildCache();
      logger.info("Successfully triggered cache build");
      
      return ResponseEntity.ok("Cache refresh completed successfully");
    } catch (Exception e) {
      logger.error("Error during cache refresh process: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error during cache refresh: " + e.getMessage());
    }
  }


  @GetMapping("/build-cache")
  public ResponseEntity<String> buildCache() {
    try {
      logger.info("Starting cache build process...");
      // Call CategoryController's buildSystemCache method with default tenant ID
      ResponseEntity<String> response = categoryController.buildSystemCache();
      logger.info("Successfully completed cache build");
      return response;
    } catch (Exception e) {
      logger.error("Error building cache: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error building cache: " + e.getMessage());
    }
  }

  @GetMapping("/clear-async")
  public ResponseEntity<String> clearAllCacheAsync() {
    new Thread(() -> {
      try {
        logger.info("Starting async cache refresh process...");
        clearAllCache();
        logger.info("Async cache refresh completed successfully");
      } catch (Exception e) {
        logger.error("Error during async cache refresh process: {}", e.getMessage(), e);
      }
    }).start();

    return ResponseEntity.ok("Cache refresh started in background");
  }

  @GetMapping("/build-cache-async")
  public ResponseEntity<String> buildCacheAsync() {
    new Thread(() -> {
      try {
        logger.info("Starting async cache build process...");
        buildCache();
        logger.info("Async cache build completed successfully");
      } catch (Exception e) {
        logger.error("Error during async cache build process: {}", e.getMessage(), e);
      }
    }).start();

    return ResponseEntity.ok("Cache build started in background");
  }
} 