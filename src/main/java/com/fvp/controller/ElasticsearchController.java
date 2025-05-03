package com.fvp.controller;

import com.fvp.document.CategoryDocument;
import com.fvp.document.LinkDocument;
import com.fvp.document.ModelDocument;
import com.fvp.dto.AutosuggestItem;
import com.fvp.service.ElasticsearchClientService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elasticsearch")
public class ElasticsearchController {

  private final ElasticsearchClientService elasticsearchService;

  @Autowired
  public ElasticsearchController(ElasticsearchClientService elasticsearchService) {
    this.elasticsearchService = elasticsearchService;
  }

  // Model endpoints
  @GetMapping("/models/search")
  public ResponseEntity<List<ModelDocument>> searchModels(
      @RequestParam(required = false) String query) {
    return ResponseEntity.ok(elasticsearchService.findModels(query));
  }

  // Category endpoints
  @GetMapping("/categories/search")
  public ResponseEntity<List<CategoryDocument>> searchCategories(
      @RequestParam(required = false) String query) {
    return ResponseEntity.ok(elasticsearchService.findCategories(query));
  }

  // Link endpoints
  @GetMapping("/links/search")
  public ResponseEntity<Map<String, Object>> searchLinks(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<LinkDocument> results = elasticsearchService.searchLinks(query, pageable);

    Map<String, Object> response = new HashMap<>();
    response.put("totalElements", results.getTotalElements());
    response.put("totalPages", results.getTotalPages());
    response.put("currentPage", results.getNumber());
    response.put("size", results.getSize());
    response.put("content", results.getContent());

    return ResponseEntity.ok(response);
  }

  // Autosuggest endpoint
  @GetMapping("/autosuggest")
  public ResponseEntity<List<AutosuggestItem>> autosuggest(@RequestParam String query) {
    return ResponseEntity.ok(elasticsearchService.autosuggest(query));
  }
} 