package com.fvp.controller;

import com.fvp.service.LinkService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class LinkCountController {

  @Autowired
  private LinkService linkService;

  @GetMapping("/count")
  public ResponseEntity<Map<String, Object>> getTotalLinkCount(
      @RequestHeader("X-Tenant-Id") Integer tenantId) {

    Map<String, Object> response = new HashMap<>();
    response.put("totalLinks", linkService.getTotalLinkCount(tenantId));

    return ResponseEntity.ok(response);
  }
} 