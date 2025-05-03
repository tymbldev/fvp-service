package com.fvp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
public class LinkCategoryShardingServiceTest {

    @InjectMocks
    @Spy
    private LinkCategoryShardingService shardingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetShardNumber_ConsistentHashing() {
        // Test that the same category always maps to the same shard
        String category = "example-category";
        int shardNumber = shardingService.getShardNumber(category);
        
        // Call multiple times to ensure consistent results
        for (int i = 0; i < 10; i++) {
            assertEquals(shardNumber, shardingService.getShardNumber(category));
        }
    }

    @Test
    public void testGetShardNumber_Distribution() {
        // Test that categories are fairly distributed across shards
        Map<Integer, Integer> shardCounts = new HashMap<>();
        
        // Generate a large number of category names
        for (int i = 0; i < 1000; i++) {
            String category = "category-" + i;
            int shardNumber = shardingService.getShardNumber(category);
            
            shardCounts.put(shardNumber, shardCounts.getOrDefault(shardNumber, 0) + 1);
        }
        
        // Verify all shards are being used
        assertEquals(10, shardCounts.size(), "All 10 shards should be used");
        
        // Check distribution is relatively even (no shard has less than 5% or more than 15% of categories)
        for (int count : shardCounts.values()) {
            assertTrue(count >= 50, "Each shard should have at least 50 categories");
            assertTrue(count <= 150, "Each shard should have at most 150 categories");
        }
    }
} 