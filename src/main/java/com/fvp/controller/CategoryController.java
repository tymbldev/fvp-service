package com.fvp.controller;

import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/home")
    public ResponseEntity<List<CategoryWithLinkDTO>> getHomeCategories(
            @RequestHeader(value = "X-Tenant-Id", required = false) Integer tenantId) {
        // If tenantId is not provided in header, use default value 0
        if (tenantId == null) {
            tenantId = 0;
        }
        
        List<CategoryWithLinkDTO> categories = categoryService.getHomeCategoriesWithLinks(tenantId);
        return ResponseEntity.ok(categories);
    }
} 