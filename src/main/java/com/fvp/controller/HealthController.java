package com.fvp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired(required = false)
    private HealthIndicator[] healthIndicators;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "FVP Service");
        
        // Add component health status if available
        if (healthIndicators != null) {
            Map<String, String> components = new HashMap<>();
            for (HealthIndicator indicator : healthIndicators) {
                Health health = indicator.health();
                components.put(indicator.getClass().getSimpleName(), health.getStatus().toString());
            }
            response.put("components", components);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
} 