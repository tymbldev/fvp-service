package com.fvp.controller;

import com.fvp.service.LinkCategoryMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/migration")
public class LinkCategoryMigrationController {

    @Autowired
    private LinkCategoryMigrationService migrationService;

    @PostMapping("/link-category/all")
    public ResponseEntity<?> migrateAllLinkCategories() {
        int migrated = migrationService.migrateAllData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed");
        response.put("recordsMigrated", migrated);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-category/tenant/{tenantId}")
    public ResponseEntity<?> migrateLinkCategoriesByTenant(@PathVariable Integer tenantId) {
        int migrated = migrationService.migrateByTenant(tenantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed for tenant " + tenantId);
        response.put("recordsMigrated", migrated);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-category/category/{categoryName}")
    public ResponseEntity<?> migrateLinkCategoriesByCategory(@PathVariable String categoryName) {
        Map<String, Object> result = migrationService.migrateByCategory(categoryName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed for category '" + categoryName + "'");
        response.put("details", result);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-category/category/{categoryName}/chunk-size/{chunkSize}")
    public ResponseEntity<?> migrateLinkCategoriesByCategoryWithChunkSize(
            @PathVariable String categoryName,
            @PathVariable int chunkSize) {
        
        if (chunkSize <= 0) {
            return ResponseEntity.badRequest().body("Chunk size must be greater than zero");
        }
        
        Map<String, Object> result = migrationService.migrateByCategory(categoryName, chunkSize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed for category '" + categoryName + "' with chunk size " + chunkSize);
        response.put("details", result);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/link-category/chunk-size")
    public ResponseEntity<?> getChunkSize() {
        int chunkSize = migrationService.getCategoryChunkSize();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("chunkSize", chunkSize);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-category/chunk-size/{chunkSize}")
    public ResponseEntity<?> setChunkSize(@PathVariable int chunkSize) {
        if (chunkSize <= 0) {
            return ResponseEntity.badRequest().body("Chunk size must be greater than zero");
        }
        
        migrationService.setCategoryChunkSize(chunkSize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Chunk size updated");
        response.put("chunkSize", chunkSize);
        
        return ResponseEntity.ok(response);
    }

    @Async
    @GetMapping("/link-category/categories-shard-info")
    public ResponseEntity<?> getCategoriesWithShardInfo() {
        List<Map<String, Object>> categoriesInfo = migrationService.getAllCategoriesWithShardInfo();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("categoriesCount", categoriesInfo.size());
        response.put("categories", categoriesInfo);
        
        return ResponseEntity.ok(response);
    }
} 