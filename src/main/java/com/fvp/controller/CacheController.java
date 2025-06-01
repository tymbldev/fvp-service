package com.fvp.controller;

import com.fvp.service.CacheService;
import com.fvp.repository.LinkRepository;
import com.fvp.service.LinkCategoryShardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

  private final CacheService cacheService;
  private final LinkRepository linkRepository;
  private final LinkCategoryShardingService linkCategoryShardingService;
  private final CategoryController categoryController;
  private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

  public CacheController(
      CacheService cacheService, 
      LinkRepository linkRepository,
      LinkCategoryShardingService linkCategoryShardingService,
      CategoryController categoryController) {
    this.cacheService = cacheService;
    this.linkRepository = linkRepository;
    this.linkCategoryShardingService = linkCategoryShardingService;
    this.categoryController = categoryController;
  }

  @GetMapping("/clear")
  public ResponseEntity<String> clearAllCache() {
    try {
      logger.info("Starting cache refresh process...");
      
      // Update random_order for all links using native query
      logger.info("Updating random_order in link table...");
      linkRepository.updateRandomOrderForAllLinks();
      logger.info("Successfully updated random_order in link table");
      
      // Update random_order in all link category shards
      logger.info("Updating random_order in all link category shards...");
      linkCategoryShardingService.updateRandomOrderInAllShards();
      logger.info("Successfully updated random_order in all link category shards");

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
} 