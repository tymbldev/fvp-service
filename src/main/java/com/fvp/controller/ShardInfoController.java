package com.fvp.controller;

import com.fvp.service.LinkCategoryShardingService;
import com.fvp.service.LinkModelShardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shard-info")
public class ShardInfoController {
    private static final Logger logger = LoggerFactory.getLogger(ShardInfoController.class);
    
    @Autowired
    private LinkCategoryShardingService categoryShardingService;
    
    @Autowired
    private LinkModelShardingService modelShardingService;
    
    /**
     * Get the shard number for a category
     * @param category the category name
     * @return the shard number and total shards
     */
    @GetMapping("/category")
    public ResponseEntity<Map<String, Object>> getCategoryShardInfo(@RequestParam String category) {
        logger.info("Getting shard info for category: {}", category);
        
        int shardNumber = categoryShardingService.getShardNumber(category);
        int totalShards = categoryShardingService.getTotalShards();
        
        Map<String, Object> response = new HashMap<>();
        response.put("category", category);
        response.put("shardNumber", shardNumber);
        response.put("totalShards", totalShards);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the shard number for a model
     * @param model the model name
     * @return the shard number and total shards
     */
    @GetMapping("/model")
    public ResponseEntity<Map<String, Object>> getModelShardInfo(@RequestParam String model) {
        logger.info("Getting shard info for model: {}", model);
        
        int shardNumber = modelShardingService.getShardNumber(model);
        int totalShards = 10; // Fixed total shards for models
        
        Map<String, Object> response = new HashMap<>();
        response.put("model", model);
        response.put("shardNumber", shardNumber);
        response.put("totalShards", totalShards);
        
        return ResponseEntity.ok(response);
    }
} 