package com.fvp.controller;

import com.fvp.entity.ProcessedSheet;
import com.fvp.repository.ProcessedSheetRepository;
import com.fvp.service.GoogleSheetProcessingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/google-sheets")
@RequiredArgsConstructor
public class GoogleSheetsController {

  private final GoogleSheetProcessingService googleSheetProcessingService;
  private final ProcessedSheetRepository processedSheetRepository;

  @GetMapping("/process")
  @Async
  public ResponseEntity<String> processGoogleSheets() {
    try {
      googleSheetProcessingService.processGoogleSheets();
      return ResponseEntity.ok("Google Sheets processing started successfully");
    } catch (Exception e) {
      log.error("Error triggering Google Sheets processing", e);
      return ResponseEntity.status(500)
          .body("Error processing Google Sheets: " + e.getMessage());
    }
  }

  @GetMapping("/processed")
  public ResponseEntity<List<ProcessedSheet>> getProcessedSheets() {
    return ResponseEntity.ok(processedSheetRepository.findAll());
  }

  @GetMapping("/processed/{workbookId}")
  public ResponseEntity<List<ProcessedSheet>> getProcessedSheetsByWorkbook(
      @PathVariable String workbookId) {
    return ResponseEntity.ok(processedSheetRepository.findByWorkbookId(workbookId));
  }
} 