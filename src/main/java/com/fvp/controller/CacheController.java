package com.fvp.controller;

import com.fvp.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearAllCache() {
        cacheService.clearAllCaches();
        return ResponseEntity.ok("All caches cleared successfully");
    }

    @DeleteMapping("/clear/{cacheName}")
    public ResponseEntity<String> clearCache(@PathVariable String cacheName) {
        cacheService.clearCache(cacheName);
        return ResponseEntity.ok("Cache '" + cacheName + "' cleared successfully");
    }
} 