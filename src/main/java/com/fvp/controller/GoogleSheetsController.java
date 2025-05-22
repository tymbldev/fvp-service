package com.fvp.controller;

import com.fvp.entity.ProcessedSheet;
import com.fvp.repository.ProcessedSheetRepository;
import com.fvp.service.GoogleSheetProcessingService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@Slf4j
@RestController
@RequestMapping("/api/google-sheets")
@RequiredArgsConstructor
public class GoogleSheetsController {

  private final GoogleSheetProcessingService googleSheetProcessingService;
  private final ProcessedSheetRepository processedSheetRepository;
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);

  @GetMapping("/process")
  @Async
  @Scheduled(fixedRate = 600000) // 10 minutes
  public ResponseEntity<String> processGoogleSheets() {
    // Check if processing is already running
    if (!isProcessing.compareAndSet(false, true)) {
      log.info("Google Sheets processing is already in progress");
      return ResponseEntity.ok("Google Sheets processing is already in progress");
    }

    try {
      log.info("Starting Google Sheets processing");
      googleSheetProcessingService.processGoogleSheets();
      return ResponseEntity.ok("Google Sheets processing started successfully");
    } catch (Exception e) {
      log.error("Error triggering Google Sheets processing", e);
      return ResponseEntity.status(500)
          .body("Error processing Google Sheets: " + e.getMessage());
    } finally {
      // Always reset the processing flag when done
      isProcessing.set(false);
      log.info("Google Sheets processing completed");
    }
  }

  /**
   * Get all processed sheets with detailed information.
   * This endpoint returns a list of all processed sheets with their complete details including:
   * - Sheet name
   * - Processing date
   * - Approval status
   * - Number of records processed
   * - Tenant ID
   * - Workbook ID
   *
   * @param tenantId The tenant ID from the X-Tenant-Id header
   * @return ResponseEntity containing a map with total count and list of processed sheets
   */
  @GetMapping("/processed")
  public ResponseEntity<Map<String, Object>> getProcessedSheets(
      @RequestHeader(value = "X-Tenant-Id", required = false, defaultValue = "1") Integer tenantId) {
    try {
      List<ProcessedSheet> sheets = processedSheetRepository.findAll();
      
      Map<String, Object> response = new HashMap<>();
      response.put("totalSheets", sheets.size());
      response.put("sheets", sheets);
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error fetching processed sheets", e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "Error fetching processed sheets: " + e.getMessage());
      return ResponseEntity.status(500).body(errorResponse);
    }
  }

  @GetMapping("/processed/{workbookId}")
  public ResponseEntity<List<ProcessedSheet>> getProcessedSheetsByWorkbook(
      @PathVariable String workbookId) {
    return ResponseEntity.ok(processedSheetRepository.findByWorkbookId(workbookId));
  }
} 