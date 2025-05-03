package com.fvp.controller;

import com.fvp.service.LinkModelMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/migration")
public class LinkModelMigrationController {

    @Autowired
    private LinkModelMigrationService migrationService;

    @PostMapping("/link-model/all")
    @Async
    public ResponseEntity<?> migrateAllLinkModels() {
        int migrated = migrationService.migrateAllData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed");
        response.put("recordsMigrated", migrated);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-model/tenant/{tenantId}")
    public ResponseEntity<?> migrateLinkModelsByTenant(@PathVariable Integer tenantId) {
        int migrated = migrationService.migrateByTenant(tenantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed for tenant " + tenantId);
        response.put("recordsMigrated", migrated);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-model/model/{modelName}")
    public ResponseEntity<?> migrateLinkModelsByModel(@PathVariable String modelName) {
        Map<String, Object> result = migrationService.migrateByModel(modelName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed for model '" + modelName + "'");
        response.put("details", result);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-model/model/{modelName}/chunk-size/{chunkSize}")
    public ResponseEntity<?> migrateLinkModelsByModelWithChunkSize(
            @PathVariable String modelName,
            @PathVariable int chunkSize) {
        
        if (chunkSize <= 0) {
            return ResponseEntity.badRequest().body("Chunk size must be greater than zero");
        }
        
        Map<String, Object> result = migrationService.migrateByModel(modelName, chunkSize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Migration completed for model '" + modelName + "' with chunk size " + chunkSize);
        response.put("details", result);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/link-model/chunk-size")
    public ResponseEntity<?> getChunkSize() {
        int chunkSize = migrationService.getModelChunkSize();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("chunkSize", chunkSize);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/link-model/chunk-size/{chunkSize}")
    public ResponseEntity<?> setChunkSize(@PathVariable int chunkSize) {
        if (chunkSize <= 0) {
            return ResponseEntity.badRequest().body("Chunk size must be greater than zero");
        }
        
        migrationService.setModelChunkSize(chunkSize);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Chunk size updated");
        response.put("chunkSize", chunkSize);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/link-model/models-shard-info")
    public ResponseEntity<?> getModelsWithShardInfo() {
        List<Map<String, Object>> modelsInfo = migrationService.getAllModelsWithShardInfo();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("modelsCount", modelsInfo.size());
        response.put("models", modelsInfo);
        
        return ResponseEntity.ok(response);
    }
} 