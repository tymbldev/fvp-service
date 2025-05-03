package com.fvp.controller;

import com.fvp.dto.CategoryWithCountDTO;
import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.service.CategoryService;
import com.fvp.service.CategoryUtilService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  private final CategoryService categoryService;
  private final CategoryUtilService categoryUtilService;
  private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

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

  @Async
  @GetMapping("/build-cache")
  public ResponseEntity<String> buildSystemCache(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    logger.info("Starting system cache build for tenant {}", tenantId);

    categoryService.getAllCategoriesWithLinkCounts(tenantId);
    logger.info("Built All categories cache");

    // Build home categories cache
    List<CategoryWithLinkDTO> homeCategories = categoryService.getHomeCategoriesWithLinks(tenantId);
    logger.info("Built home categories cache with {} categories", homeCategories.size());

    // Build home SEO categories cache
    List<CategoryWithLinkDTO> homeSeoCategories = categoryService.getHomeSeoCategories(tenantId);
    logger.info("Built home SEO categories cache with {} categories", homeSeoCategories.size());

    // Get all distinct category names
    Set<String> allCategoryNames = new HashSet<>();
    allCategoryNames.addAll(
        homeCategories.stream().map(CategoryWithLinkDTO::getName).collect(Collectors.toSet()));
    allCategoryNames.addAll(
        homeSeoCategories.stream().map(CategoryWithLinkDTO::getName).collect(Collectors.toSet()));

    // Build cache for each category's first page
    Pageable firstPage = PageRequest.of(0, 20, Sort.by("randomOrder"));
    for (String categoryName : allCategoryNames) {
      try {
        categoryUtilService.getCategoryLinks(tenantId, categoryName, firstPage, null, null, null);
        logger.info("Built cache for category: {}", categoryName);
      } catch (Exception e) {
        logger.error("Error building cache for category {}: {}", categoryName, e.getMessage());
      }
    }

    String message = String.format("System cache built successfully. Processed %d categories",
        allCategoryNames.size());
    logger.info(message);
    return ResponseEntity.ok(message);
  }

  @GetMapping("/all")
  public ResponseEntity<List<CategoryWithCountDTO>> getAllCategories(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    List<CategoryWithCountDTO> categories = categoryService.getAllCategoriesWithLinkCounts(
        tenantId);
    return ResponseEntity.ok(categories);
  }
} 