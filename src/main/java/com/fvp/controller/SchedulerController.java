package com.fvp.controller;

import com.fvp.service.CategoryUtilService;
import com.fvp.service.SchedulerService;
import com.fvp.util.LoggingUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  public ResponseEntity<String> processOnlyThumbPath(@RequestParam(required = false) Integer linkId) {
    new Thread(() -> {
      try {
        if (linkId != null) {
          logger.info("Starting scheduled task in background for link ID: {}", linkId);
          schedulerService.processOnlyThumbPath(linkId);
          logger.info("Background scheduled task completed successfully for link ID: {}", linkId);
        } else {
          logger.info("Starting scheduled task in background for all links");
          schedulerService.processOnlyThumbPath();
          logger.info("Background scheduled task completed successfully for all links");
        }
      } catch (Exception e) {
        logger.error("Error in background scheduled task: {}", e.getMessage(), e);
      }
    }).start();

    if (linkId != null) {
      return ResponseEntity.ok("Scheduled task started in background for link ID: " + linkId);
    } else {
      return ResponseEntity.ok("Scheduled task started in background for all links");
    }
  }

  @GetMapping("/trigger-thumb-sync")
  public ResponseEntity<String> processOnlyThumbPathSync(@RequestParam(required = false) Integer linkId) {
    try {
      if (linkId != null) {
        logger.info("Starting synchronous thumb path processing for link ID: {}", linkId);
        schedulerService.processOnlyThumbPath(linkId);
        logger.info("Synchronous thumb path processing completed successfully for link ID: {}", linkId);
        return ResponseEntity.ok("Thumb path processing completed successfully for link ID: " + linkId);
      } else {
        logger.info("Starting synchronous thumb path processing for all links");
        schedulerService.processOnlyThumbPath();
        logger.info("Synchronous thumb path processing completed successfully for all links");
        return ResponseEntity.ok("Thumb path processing completed successfully for all links");
      }
    } catch (Exception e) {
      logger.error("Error in synchronous thumb path processing: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body("Error during processing: " + e.getMessage());
    }
  }

} 