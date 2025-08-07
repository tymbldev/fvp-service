package com.fvp.controller;

import com.fvp.dto.CategoryWithCountDTO;
import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.service.CategoryService;
import com.fvp.service.CategoryUtilService;
import com.fvp.util.CacheBypassUtil;
import java.util.HashSet;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  private final CategoryService categoryService;
  private final CategoryUtilService categoryUtilService;
  private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

  @Autowired
  private ExecutorService executorService;

  @Autowired
  public CategoryController(CategoryService categoryService,
      CategoryUtilService categoryUtilService) {
    this.categoryService = categoryService;
    this.categoryUtilService = categoryUtilService;
  }

  @GetMapping("/home")
  public ResponseEntity<List<CategoryWithLinkDTO>> getHomeCategories(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    logger.info("Received request for home categories with tenantId: {}", tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      List<CategoryWithLinkDTO> categories = categoryService.getHomeCategoriesWithLinks(tenantId);
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} home categories for tenantId: {} in {} ms", 
          categories.size(), tenantId, duration);
      return ResponseEntity.ok(categories);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving home categories for tenantId: {} after {} ms: {}", 
          tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }

  @GetMapping("/home-seo")
  public ResponseEntity<List<CategoryWithLinkDTO>> getHomeSeoCategories(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    logger.info("Received request for home SEO categories with tenantId: {}", tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      List<CategoryWithLinkDTO> categories = categoryService.getHomeSeoCategories(tenantId);
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} home SEO categories for tenantId: {} in {} ms", 
          categories.size(), tenantId, duration);
      return ResponseEntity.ok(categories);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving home SEO categories for tenantId: {} after {} ms: {}", 
          tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }

  @GetMapping("/{categoryName}/first")
  public ResponseEntity<CategoryWithLinkDTO> getCategoryFirstLink(
      @PathVariable String categoryName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    logger.info("Received request for first link of category: {} with tenantId: {}", categoryName, tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      CategoryWithLinkDTO category = categoryService.getCategoryFirstLink(tenantId, categoryName);
      long duration = System.currentTimeMillis() - startTime;
      if (category != null) {
        logger.info("Successfully retrieved first link for category: {} (tenantId: {}) in {} ms", 
            categoryName, tenantId, duration);
      } else {
        logger.warn("No first link found for category: {} (tenantId: {}) in {} ms", 
            categoryName, tenantId, duration);
      }
      return ResponseEntity.ok(category);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving first link for category: {} (tenantId: {}) after {} ms: {}", 
          categoryName, tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }

  @GetMapping("/{categoryName}/links")
  public ResponseEntity<Page<CategoryWithLinkDTO>> getCategoryLinks(
      @PathVariable String categoryName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId,
      @RequestParam(required = false) Integer minDuration,
      @RequestParam(required = false) Integer maxDuration,
      @RequestParam(required = false) String quality,
      @PageableDefault(size = 20, sort = "randomOrder") Pageable pageable) {
    logger.info("Received request for category links - category: {}, tenantId: {}, minDuration: {}, maxDuration: {}, quality: {}, page: {}, size: {}", 
        categoryName, tenantId, minDuration, maxDuration, quality, pageable.getPageNumber(), pageable.getPageSize());
    long startTime = System.currentTimeMillis();
    
    try {
      Page<CategoryWithLinkDTO> links = categoryUtilService.getCategoryLinks(tenantId, categoryName,
          pageable, minDuration, maxDuration, quality);
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} category links for category: {} (tenantId: {}) in {} ms. Total elements: {}, total pages: {}", 
          links.getContent().size(), categoryName, tenantId, duration, links.getTotalElements(), links.getTotalPages());
      return ResponseEntity.ok(links);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving category links for category: {} (tenantId: {}) after {} ms: {}", 
          categoryName, tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }

  @PostMapping("/build-cache")
  public ResponseEntity<String> buildSystemCache() {
    logger.info("Received request to build system cache");
    long startTime = System.currentTimeMillis();
    
    try {
      // Set cache bypass flag to true for this thread
      CacheBypassUtil.setCacheBypass(true);
      logger.debug("Cache bypass flag set to true for cache building");
      
      // Execute cache building in background thread with cache bypass flag propagated
      CacheBypassUtil.executeWithCacheBypass(executorService, () -> {
        logger.info("Starting cache building process in background thread");
        categoryService.buildSystemCache();
        logger.info("Cache building process completed successfully");
        return null;
      });
      
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Cache build request initiated successfully in {} ms", duration);
      return ResponseEntity.ok("Cache build started successfully");
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error initiating cache build after {} ms: {}", duration, e.getMessage(), e);
      throw e;
    } finally {
      // Clear the cache bypass flag
      CacheBypassUtil.clearCacheBypass();
      logger.debug("Cache bypass flag cleared");
    }
  }

  @GetMapping("/all")
  public ResponseEntity<List<CategoryWithCountDTO>> getAllCategories(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    logger.info("Received request for all categories with link counts for tenantId: {}", tenantId);
    long startTime = System.currentTimeMillis();
    
    try {
      List<CategoryWithCountDTO> categories = categoryService.getAllCategoriesWithLinkCounts(
          tenantId);
      long duration = System.currentTimeMillis() - startTime;
      logger.info("Successfully retrieved {} categories with link counts for tenantId: {} in {} ms", 
          categories.size(), tenantId, duration);
      return ResponseEntity.ok(categories);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error retrieving all categories for tenantId: {} after {} ms: {}", 
          tenantId, duration, e.getMessage(), e);
      throw e;
    }
  }
} 