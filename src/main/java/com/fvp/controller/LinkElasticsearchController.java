package com.fvp.controller;

import com.fvp.document.LinkDocument;
import com.fvp.service.ElasticsearchClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elasticsearch/links")
public class LinkElasticsearchController {

    private final ElasticsearchClientService elasticsearchService;

    public LinkElasticsearchController(ElasticsearchClientService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<LinkDocument>> searchLinks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String searchableText) {
        return ResponseEntity.ok(elasticsearchService.searchByTitleOrText(title, searchableText));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<LinkDocument>> searchByCategory(@PathVariable String category) {
        return ResponseEntity.ok(elasticsearchService.findByCategoriesContaining(category));
    }
} 