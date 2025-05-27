package com.fvp.controller;

import com.fvp.service.SchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerController.class);
  private final SchedulerService schedulerService;

  @Value("${scheduler.enabled}") // Default 8 hours in milliseconds
  private Boolean enabled;


  public SchedulerController(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @GetMapping("/trigger")
  //@Scheduled(fixedRate = 1000 * 60 * 60 * 8) // 8 hours
  public ResponseEntity<String> trigger() {

    // Only run in production environment
    if (!true == enabled) {
      return ResponseEntity.ok("Scheduler is disabled for non-production environment");
    }

    try {
      logger.info("Starting scheduled task...");
      schedulerService.processGoogleSheetsAndThumbPaths();
      logger.info("Scheduled task completed successfully");
      return ResponseEntity.ok("Processing completed successfully");
    } catch (Exception e) {
      logger.error("Error in scheduled task: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body("Error during processing: " + e.getMessage());
    }
  }
} 