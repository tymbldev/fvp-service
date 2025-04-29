package com.fvp.controller;

import com.fvp.config.ElasticsearchSyncConfig;
import com.fvp.service.ElasticsearchSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
} 