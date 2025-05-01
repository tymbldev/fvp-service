package com.fvp.controller;

import com.fvp.service.LinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/links")
public class LinkCountController {

    @Autowired
    private LinkService linkService;

    @GetMapping("/count")
    @Cacheable(value = "linkCounts", key = "#tenantId")
    public ResponseEntity<Map<String, Object>> getTotalLinkCount(
            @RequestHeader("X-Tenant-Id") Integer tenantId) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalLinks", linkService.getTotalLinkCount(tenantId));
        
        return ResponseEntity.ok(response);
    }
} 