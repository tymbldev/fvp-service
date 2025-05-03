package com.fvp.controller;

import com.fvp.config.ElasticsearchSyncConfig;
import com.fvp.service.ElasticsearchSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/elasticsearch")
public class ElasticsearchSyncController {

    private final ElasticsearchSyncService syncService;
    private final ElasticsearchSyncConfig syncConfig;
    private CompletableFuture<String> currentSyncOperation;
    private CompletableFuture<String> currentCategorySyncOperation;
    private CompletableFuture<String> currentModelSyncOperation;

    @Autowired
    public ElasticsearchSyncController(
            ElasticsearchSyncService syncService,
            ElasticsearchSyncConfig syncConfig) {
        this.syncService = syncService;
        this.syncConfig = syncConfig;
    }

    /**
     * Trigger a full sync of all links from MySQL to Elasticsearch
     * @return Response with sync status
     */
    @Async
    @GetMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncAllLinks() {
        Map<String, Object> response = new HashMap<>();
        
        if (!syncConfig.isEnabled()) {
            response.put("status", "disabled");
            response.put("message", "Elasticsearch sync is disabled in configuration");
            return ResponseEntity.ok(response);
        }
        
        if (currentSyncOperation != null && !currentSyncOperation.isDone()) {
            response.put("status", "in_progress");
            response.put("message", "A sync operation is already in progress");
            return ResponseEntity.ok(response);
        }
        
        currentSyncOperation = syncService.syncAllLinksToElasticsearch();
        
        response.put("status", "started");
        response.put("message", "Sync operation started in background");
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Get the status of the current sync operation
     * @return Response with sync status
     */
    @GetMapping("/sync/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> response = new HashMap<>();
        
        if (!syncConfig.isEnabled()) {
            response.put("status", "disabled");
            response.put("message", "Elasticsearch sync is disabled in configuration");
            return ResponseEntity.ok(response);
        }
        
        if (currentSyncOperation == null) {
            response.put("status", "not_started");
            response.put("message", "No sync operation has been started");
            return ResponseEntity.ok(response);
        }
        
        if (currentSyncOperation.isDone()) {
            try {
                String result = currentSyncOperation.get();
                response.put("status", "completed");
                response.put("message", result);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Error getting sync result: " + e.getMessage());
            }
        } else {
            response.put("status", "in_progress");
            response.put("message", "Sync operation is still running");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger a full sync of all categories from MySQL to Elasticsearch
     * @return Response with sync status
     */
    @GetMapping("/sync/categories")
    public ResponseEntity<Map<String, Object>> syncAllCategories() {
        Map<String, Object> response = new HashMap<>();
        
        if (!syncConfig.isEnabled()) {
            response.put("status", "disabled");
            response.put("message", "Elasticsearch sync is disabled in configuration");
            return ResponseEntity.ok(response);
        }
        
        if (currentCategorySyncOperation != null && !currentCategorySyncOperation.isDone()) {
            response.put("status", "in_progress");
            response.put("message", "A category sync operation is already in progress");
            return ResponseEntity.ok(response);
        }
        
        currentCategorySyncOperation = syncService.syncAllCategoriesToElasticsearch();
        
        response.put("status", "started");
        response.put("message", "Category sync operation started in background");
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Get the status of the current category sync operation
     * @return Response with sync status
     */
    @GetMapping("/sync/categories/status")
    public ResponseEntity<Map<String, Object>> getCategorySyncStatus() {
        Map<String, Object> response = new HashMap<>();
        
        if (!syncConfig.isEnabled()) {
            response.put("status", "disabled");
            response.put("message", "Elasticsearch sync is disabled in configuration");
            return ResponseEntity.ok(response);
        }
        
        if (currentCategorySyncOperation == null) {
            response.put("status", "not_started");
            response.put("message", "No category sync operation has been started");
            return ResponseEntity.ok(response);
        }
        
        if (currentCategorySyncOperation.isDone()) {
            try {
                String result = currentCategorySyncOperation.get();
                response.put("status", "completed");
                response.put("message", result);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Error getting category sync result: " + e.getMessage());
            }
        } else {
            response.put("status", "in_progress");
            response.put("message", "Category sync operation is still running");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Trigger a full sync of all models from MySQL to Elasticsearch
     * @return Response with sync status
     */
    @GetMapping("/sync/models")
    public ResponseEntity<Map<String, Object>> syncAllModels() {
        Map<String, Object> response = new HashMap<>();
        
        if (!syncConfig.isEnabled()) {
            response.put("status", "disabled");
            response.put("message", "Elasticsearch sync is disabled in configuration");
            return ResponseEntity.ok(response);
        }
        
        if (currentModelSyncOperation != null && !currentModelSyncOperation.isDone()) {
            response.put("status", "in_progress");
            response.put("message", "A model sync operation is already in progress");
            return ResponseEntity.ok(response);
        }
        
        currentModelSyncOperation = syncService.syncAllModelsToElasticsearch();
        
        response.put("status", "started");
        response.put("message", "Model sync operation started in background");
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Get the status of the current model sync operation
     * @return Response with sync status
     */
    @GetMapping("/sync/models/status")
    public ResponseEntity<Map<String, Object>> getModelSyncStatus() {
        Map<String, Object> response = new HashMap<>();
        
        if (!syncConfig.isEnabled()) {
            response.put("status", "disabled");
            response.put("message", "Elasticsearch sync is disabled in configuration");
            return ResponseEntity.ok(response);
        }
        
        if (currentModelSyncOperation == null) {
            response.put("status", "not_started");
            response.put("message", "No model sync operation has been started");
            return ResponseEntity.ok(response);
        }
        
        if (currentModelSyncOperation.isDone()) {
            try {
                String result = currentModelSyncOperation.get();
                response.put("status", "completed");
                response.put("message", result);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Error getting model sync result: " + e.getMessage());
            }
        } else {
            response.put("status", "in_progress");
            response.put("message", "Model sync operation is still running");
        }
        
        return ResponseEntity.ok(response);
    }
} 