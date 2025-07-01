package com.fvp.service;

import com.fvp.controller.CacheController;
import com.fvp.controller.ThumbPathGenerationController;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class SchedulerService {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

  private final GoogleSheetProcessingService googleSheetProcessingService;
  private final ThumbPathGenerationController thumbPathGenerationController;
  private final CacheController cacheController;
  private static AtomicBoolean status = new AtomicBoolean(false);

  public SchedulerService(
      GoogleSheetProcessingService googleSheetProcessingService,
      ThumbPathGenerationController thumbPathGenerationController,
      CacheController cacheController) {
    this.googleSheetProcessingService = googleSheetProcessingService;
    this.thumbPathGenerationController = thumbPathGenerationController;
    this.cacheController = cacheController;
  }

  @PostConstruct
  public void init() {
    new Thread(() -> {
      while (true) {
        try {
          if (status.get() == true) {
            logger.info("Execution already in progress, skipping this cycle");
            continue;
          }
          status.set(true);
          logger.info(
              "Post Contruct Starting scheduled task for Google Sheets and Thumb Paths processing");
          cacheController.clearAllCache();
          logger.info("Running FED Build Re-Run");
          fedBuildReRun();
          Thread.sleep(1000 * 60 * 60 * 24); // Sleep for 1 day
          status.set(false);
        } catch (Exception e) {
          logger.error("Error executing post contruct {}", e.getMessage(), e);
          status.set(false);
        }
      }
    }).start();
  }

  public void processGoogleSheetsAndThumbPaths() {
    logger.info("Starting parallel processing of Google Sheets and Thumb Paths");
    if (googleSheetProcessingService.processGoogleSheets()) {
      thumbPathGenerationController.processAllThumbPaths();
      cacheController.clearAllCache();
      fedBuildReRun();
    }
  }

  private void fedBuildReRun() {
    // Execute deployment script
    try {
      logger.info("Starting deployment script execution");
      ProcessBuilder processBuilder = new ProcessBuilder(
          "/apps/fvp/devops/fvp-devops/sync.sh");
      processBuilder.inheritIO(); // This will show the output in the application logs
      Process process = processBuilder.start();

      // Wait for the process to complete with a timeout of 5 minutes
      boolean completed = process.waitFor(5, TimeUnit.MINUTES);
      if (!completed) {
        process.destroyForcibly();
        throw new RuntimeException("Deployment script execution timed out after 5 minutes");
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new RuntimeException("Deployment script failed with exit code: " + exitCode);
      }

      logger.info("Deployment script executed successfully");
    } catch (IOException | InterruptedException e) {
      logger.error("Error executing deployment script: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to execute deployment script", e);
    }
  }
} 