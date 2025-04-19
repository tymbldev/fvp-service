package com.fvp.controller;

import com.fvp.document.CategoryDocument;
import com.fvp.service.CategoryElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryElasticsearchService categoryService;

    @Autowired
    public CategoryController(CategoryElasticsearchService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryDocument> createCategory(@RequestBody CategoryDocument category) {
        return ResponseEntity.ok(categoryService.save(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDocument> getCategoryById(@PathVariable String id) {
        return categoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<CategoryDocument>> getCategoriesByTenantId(@PathVariable Integer tenantId) {
        List<CategoryDocument> categories = categoryService.findByTenantId(tenantId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategoryDocument>> searchCategories(@RequestParam String name) {
        List<CategoryDocument> categories = categoryService.searchByName(name);
        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDocument> updateCategory(
            @PathVariable String id,
            @RequestBody CategoryDocument category) {
        return categoryService.findById(id)
                .map(existingCategory -> {
                    category.setId(id);
                    return ResponseEntity.ok(categoryService.save(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        if (categoryService.findById(id).isPresent()) {
            categoryService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Iterable<CategoryDocument>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }
} 