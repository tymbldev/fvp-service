package com.fvp.controller;

import com.fvp.document.LinkDocument;
import com.fvp.service.LinkElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/es/links")
public class LinkElasticsearchController {

    private final LinkElasticsearchService linkService;

    @Autowired
    public LinkElasticsearchController(LinkElasticsearchService linkService) {
        this.linkService = linkService;
    }

    @PostMapping
    public ResponseEntity<LinkDocument> createLink(@RequestBody LinkDocument link) {
        return ResponseEntity.ok(linkService.save(link));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LinkDocument> getLinkById(@PathVariable String id) {
        return linkService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<LinkDocument>> getLinksByCategory(@PathVariable String category) {
        List<LinkDocument> links = linkService.findByCategoriesContaining(category);
        return ResponseEntity.ok(links);
    }

    @GetMapping("/search")
    public ResponseEntity<List<LinkDocument>> searchLinks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String searchableText) {
        List<LinkDocument> links = linkService.searchByTitleOrText(
                title != null ? title : "",
                searchableText != null ? searchableText : "");
        return ResponseEntity.ok(links);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LinkDocument> updateLink(
            @PathVariable String id,
            @RequestBody LinkDocument link) {
        return linkService.findById(id)
                .map(existingLink -> {
                    link.setId(id);
                    return ResponseEntity.ok(linkService.save(link));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable String id) {
        if (linkService.findById(id).isPresent()) {
            linkService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Iterable<LinkDocument>> getAllLinks() {
        return ResponseEntity.ok(linkService.findAll());
    }
} 