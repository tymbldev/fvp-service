package com.fvp.service;

import com.fvp.controller.CategoryController;
import com.fvp.controller.ThumbPathGenerationController;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import com.fvp.util.LoggingUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class SchedulerService {

  private static final Logger logger = LoggingUtil.getLogger(SchedulerService.class);

  private final GoogleSheetProcessingService googleSheetProcessingService;
  private final ThumbPathGenerationController thumbPathGenerationController;
  private final CategoryController categoryController;
  private static AtomicBoolean status = new AtomicBoolean(false);

  @Value("${scheduler.enabled:true}")
  private boolean schedulerEnabled;

  public SchedulerService(
      GoogleSheetProcessingService googleSheetProcessingService,
      ThumbPathGenerationController thumbPathGenerationController,
      CategoryController categoryController) {
    this.googleSheetProcessingService = googleSheetProcessingService;
    this.thumbPathGenerationController = thumbPathGenerationController;
    this.categoryController = categoryController;
  }

  public void processGoogleSheetsAndThumbPaths() {
    logger.info("Starting parallel processing of Google Sheets and Thumb Paths");
    googleSheetProcessingService.processGoogleSheets();
  }

  public void processOnlyThumbPath() {
    logger.info("Starting thumb path processing");
    thumbPathGenerationController.processAllThumbPaths();
    categoryController.buildSystemCache();
    fedBuildReRun();
  }


  private void fedBuildReRun() {
    // Execute deployment script with root privileges
    try {
      logger.info("Starting deployment script execution with root privileges");
      ProcessBuilder processBuilder = new ProcessBuilder(
          "sudo", "/apps/fvp/devops/fvp-devops/sync.sh");
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

      logger.info("Deployment script executed successfully with root privileges");
    } catch (IOException | InterruptedException e) {
      logger.error("Error executing deployment script with root privileges: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to execute deployment script with root privileges", e);
    }
  }
} 