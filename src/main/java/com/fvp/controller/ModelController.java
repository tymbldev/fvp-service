package com.fvp.controller;

import com.fvp.dto.ModelWithLinkDTO;
import com.fvp.dto.ModelLinksResponseDTO;
import com.fvp.dto.ModelWithoutLinkDTO;
import com.fvp.service.ModelService;
import com.fvp.service.ModelUtilService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
public class ModelController {

  private static final Logger logger = LoggerFactory.getLogger(ModelController.class);

  @Autowired
  private ModelService modelService;

  @Autowired
  private ModelUtilService modelUtilService;

  @GetMapping("/all")
  public ResponseEntity<List<ModelWithoutLinkDTO>> getHomeModels(
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    List<ModelWithoutLinkDTO> models = modelService.getAllModels(tenantId);
    return ResponseEntity.ok(models);
  }

  @GetMapping("/{modelName}/first")
  public ResponseEntity<ModelWithLinkDTO> getModelFirstLink(
      @PathVariable String modelName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId) {
    ModelWithLinkDTO model = modelService.getModelFirstLink(tenantId, modelName);
    return ResponseEntity.ok(model);
  }

  @GetMapping("/{modelName}/links")
  public ResponseEntity<Page<ModelLinksResponseDTO>> getModelLinks(
      @PathVariable String modelName,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Integer tenantId,
      @RequestParam(required = false) Integer maxDuration,
      @RequestParam(required = false) String quality,
      @PageableDefault(size = 20, sort = "randomOrder") Pageable pageable) {
    Page<ModelLinksResponseDTO> links = modelUtilService.getModelLinks(tenantId, modelName, pageable,
        maxDuration, quality);
    return ResponseEntity.ok(links);
  }

}