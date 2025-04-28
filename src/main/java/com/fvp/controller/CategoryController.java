package com.fvp.controller;

import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/home")
    public ResponseEntity<List<CategoryWithLinkDTO>> getHomeCategories(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
        List<CategoryWithLinkDTO> categories = categoryService.getHomeCategoriesWithLinks(tenantId);
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
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) String quality,
            @PageableDefault(size = 20, sort = "randomOrder") Pageable pageable) {
        Page<CategoryWithLinkDTO> links = categoryService.getCategoryLinks(tenantId, categoryName, pageable, maxDuration, quality);
        return ResponseEntity.ok(links);
    }
} 