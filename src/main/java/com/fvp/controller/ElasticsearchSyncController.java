package com.fvp.controller;

import com.fvp.service.ElasticsearchSyncService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elasticsearch/sync")
public class ElasticsearchSyncController {

  private final ElasticsearchSyncService elasticsearchSyncService;

  @Autowired
  public ElasticsearchSyncController(ElasticsearchSyncService elasticsearchSyncService) {
    this.elasticsearchSyncService = elasticsearchSyncService;
  }

  @PostMapping("/links")
  public ResponseEntity<Map<String, Object>> syncLinks() {
    Map<String, Object> response = new HashMap<>();
    try {
      elasticsearchSyncService.syncAllLinksToElasticsearch();
      response.put("status", "success");
    } catch (Exception e) {
      response.put("status", "error");
      response.put("message", "Error syncing links: " + e.getMessage());
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping("/categories")
  public ResponseEntity<Map<String, Object>> syncCategories() {
    Map<String, Object> response = new HashMap<>();
    try {
      String result = elasticsearchSyncService.syncAllCategoriesToElasticsearch().get();
      response.put("status", "success");
      response.put("message", result);
    } catch (Exception e) {
      response.put("status", "error");
      response.put("message", "Error syncing categories: " + e.getMessage());
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping("/models")
  public ResponseEntity<Map<String, Object>> syncModels() {
    Map<String, Object> response = new HashMap<>();
    try {
      String result = elasticsearchSyncService.syncAllModelsToElasticsearch().get();
      response.put("status", "success");
      response.put("message", result);
    } catch (Exception e) {
      response.put("status", "error");
      response.put("message", "Error syncing models: " + e.getMessage());
    }
    return ResponseEntity.ok(response);
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getSyncStatus() {
    Map<String, Object> response = new HashMap<>();
    response.put("links", elasticsearchSyncService.getLinkSyncStatus());
    response.put("categories", elasticsearchSyncService.getCategorySyncStatus());
    response.put("models", elasticsearchSyncService.getModelSyncStatus());
    return ResponseEntity.ok(response);
  }
} 