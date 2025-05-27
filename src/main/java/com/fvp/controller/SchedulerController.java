package com.fvp.controller;

import com.fvp.service.GoogleSheetProcessingService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

  private final GoogleSheetProcessingService googleSheetProcessingService;
  private final ThumbPathGenerationController thumbPathGenerationController;
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);

  @GetMapping("/trigger")
  @Scheduled(fixedRate = 1000 * 60 * 60 * 8) // 8 hours
  public ResponseEntity<String> trigger() {
    // Check if processing is already running
    if (!isProcessing.compareAndSet(false, true)) {
      log.info("Processing is already in progress");
      return ResponseEntity.ok("Processing is already in progress");
    }

    try {
      log.info("Starting parallel processing of Google Sheets and Thumb Paths");
      
      // Create CompletableFuture for each task
      CompletableFuture<Void> googleSheetsFuture = CompletableFuture.runAsync(() -> {
        try {
          googleSheetProcessingService.processGoogleSheets();
        } catch (Exception e) {
          log.error("Error in Google Sheets processing", e);
          throw new CompletionException(e);
        }
      });

      CompletableFuture<Void> thumbPathsFuture = CompletableFuture.runAsync(() -> {
        try {
          thumbPathGenerationController.processAllThumbPaths();
        } catch (Exception e) {
          log.error("Error in Thumb Paths processing", e);
          throw new CompletionException(e);
        }
      });

      // Wait for both tasks to complete
      CompletableFuture.allOf(googleSheetsFuture, thumbPathsFuture).join();
      
      log.info("Completed parallel processing of Google Sheets and Thumb Paths");
      return ResponseEntity.ok("Processing completed successfully");
    } catch (Exception e) {
      log.error("Error in parallel processing", e);
      return ResponseEntity.status(500)
          .body("Error during processing: " + e.getMessage());
    } finally {
      isProcessing.set(false);
    }
  }
} 