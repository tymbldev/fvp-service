package com.fvp.controller;

import com.fvp.document.CategoryDocument;
import com.fvp.document.LinkDocument;
import com.fvp.document.ModelDocument;
import com.fvp.service.ElasticsearchClientService;
import com.fvp.dto.AutosuggestItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class ElasticsearchSearchController {

    private final ElasticsearchClientService elasticsearchService;

    @Autowired
    public ElasticsearchSearchController(ElasticsearchClientService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }
    
    /**
     * Search for links by category (uses the links index)
     * @param category The category to search for
     * @return List of matching links
     */
    @GetMapping("/links/category/{category}")
    public ResponseEntity<List<LinkDocument>> searchLinksByCategory(@PathVariable String category) {
        return ResponseEntity.ok(elasticsearchService.findByCategoriesContaining(category));
    }
    
    /**
     * Search for categories by name (uses the categories index)
     * @param name The category name to search for
     * @return List of matching categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDocument>> searchCategories(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(elasticsearchService.findCategories(name));
    }
    
    /**
     * Search for categories by exact name (uses the categories index)
     * @param category The exact category name
     * @return List of matching categories
     */
    @GetMapping("/categories/{category}")
    public ResponseEntity<List<CategoryDocument>> searchCategoryByName(@PathVariable String category) {
        return ResponseEntity.ok(elasticsearchService.findCategories(category));
    }
    
    /**
     * Search for models by name (uses the models index)
     * @param name The model name to search for
     * @return List of matching models
     */
    @GetMapping("/models")
    public ResponseEntity<List<ModelDocument>> searchModels(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(elasticsearchService.findModels(name));
    }
    
    /**
     * Search for models by exact name (uses the models index)
     * @param model The exact model name
     * @return List of matching models
     */
    @GetMapping("/models/{model}")
    public ResponseEntity<List<ModelDocument>> searchModelByName(@PathVariable String model) {
        return ResponseEntity.ok(elasticsearchService.findModels(model));
    }
    
    /**
     * Perform autosuggest search across all indices (links, categories, and models)
     * Each result includes a 'type' field indicating if it's a link, category, or model
     * @param query The search query
     * @return List of matching items across all indices with type identification
     */
    @GetMapping("/autosuggest")
    public ResponseEntity<List<AutosuggestItem>> autosuggest(@RequestParam String query) {
        return ResponseEntity.ok(elasticsearchService.autosuggest(query));
    }
} 