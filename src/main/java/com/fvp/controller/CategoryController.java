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
    List<CategoryWithLinkDTO> categories = categoryService.getHomeCategoriesWithLinks(tenantId);
    return ResponseEntity.ok(categories);
  }

  @GetMapping("/home-seo")
  public ResponseEntity<List<CategoryWithLinkDTO>> getHomeSeoCategories(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    List<CategoryWithLinkDTO> categories = categoryService.getHomeSeoCategories(tenantId);
    return ResponseEntity.ok(categories);
  }

  @GetMapping("/{categoryName}/first")
  public ResponseEntity<CategoryWithLinkDTO> getCategoryFirstLink(
      @PathVariable String categoryName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    CategoryWithLinkDTO category = categoryService.getCategoryFirstLink(tenantId, categoryName);
    return ResponseEntity.ok(category);
  }

  @GetMapping("/{categoryName}/links")
  public ResponseEntity<Page<CategoryWithLinkDTO>> getCategoryLinks(
      @PathVariable String categoryName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId,
      @RequestParam(required = false) Integer minDuration,
      @RequestParam(required = false) Integer maxDuration,
      @RequestParam(required = false) String quality,
      @PageableDefault(size = 20, sort = "randomOrder") Pageable pageable) {
    Page<CategoryWithLinkDTO> links = categoryUtilService.getCategoryLinks(tenantId, categoryName,
        pageable, minDuration, maxDuration, quality);
    return ResponseEntity.ok(links);
  }

  @PostMapping("/build-cache")
  public ResponseEntity<String> buildSystemCache() {
    try {
      // Set cache bypass flag to true for this thread
      CacheBypassUtil.setCacheBypass(true);
      
      // Execute cache building in background thread with cache bypass flag propagated
      CacheBypassUtil.executeWithCacheBypass(executorService, () -> {
        categoryService.buildSystemCache();
        return null;
      });
      
      return ResponseEntity.ok("Cache build started successfully");
    } finally {
      // Clear the cache bypass flag
      CacheBypassUtil.clearCacheBypass();
    }
  }

  @GetMapping("/all")
  public ResponseEntity<List<CategoryWithCountDTO>> getAllCategories(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    List<CategoryWithCountDTO> categories = categoryService.getAllCategoriesWithLinkCounts(
        tenantId);
    return ResponseEntity.ok(categories);
  }
} 