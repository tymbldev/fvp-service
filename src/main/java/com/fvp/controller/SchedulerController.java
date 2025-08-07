package com.fvp.controller;

import com.fvp.service.CategoryUtilService;
import com.fvp.service.SchedulerService;
import com.fvp.util.LoggingUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import com.fvp.util.LoggingUtil;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

  private static final Logger logger = LoggingUtil.getLogger(SchedulerController.class);
  
  private final SchedulerService schedulerService;


  public SchedulerController(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @GetMapping("/trigger")
  public ResponseEntity<String> trigger() {

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

  @GetMapping("/trigger-async")
  public ResponseEntity<String> triggerAsync() {
    new Thread(() -> {
      try {
        logger.info("flag is off..");
        logger.info("Starting scheduled task in background...");
        schedulerService.processGoogleSheetsAndThumbPaths();
        logger.info("Background scheduled task completed successfully");
      } catch (Exception e) {
        logger.error("Error in background scheduled task: {}", e.getMessage(), e);
      }
    }).start();

    return ResponseEntity.ok("Scheduled task started in background");
  }

  @GetMapping("/trigger-thumb-async")
  public ResponseEntity<String> processOnlyThumbPath() {
    new Thread(() -> {
      try {
        logger.info("Starting scheduled task in background...");
        schedulerService.processOnlyThumbPath();
        logger.info("Background scheduled task completed successfully");
      } catch (Exception e) {
        logger.error("Error in background scheduled task: {}", e.getMessage(), e);
      }
    }).start();

    return ResponseEntity.ok("Scheduled task started in background");
  }

} 