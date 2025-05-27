package com.fvp.service;

import com.fvp.controller.ThumbPathGenerationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    
    private final GoogleSheetProcessingService googleSheetProcessingService;
    private final ThumbPathGenerationController thumbPathGenerationController;

    public SchedulerService(
            GoogleSheetProcessingService googleSheetProcessingService,
            ThumbPathGenerationController thumbPathGenerationController) {
        this.googleSheetProcessingService = googleSheetProcessingService;
        this.thumbPathGenerationController = thumbPathGenerationController;
    }

    public void processGoogleSheetsAndThumbPaths() {
        logger.info("Starting parallel processing of Google Sheets and Thumb Paths");
        
        // Create CompletableFuture for each task
        CompletableFuture<Void> googleSheetsFuture = CompletableFuture.runAsync(() -> {
            try {
                googleSheetProcessingService.processGoogleSheets();
            } catch (Exception e) {
                logger.error("Error in Google Sheets processing", e);
                throw new CompletionException(e);
            }
        });

        CompletableFuture<Void> thumbPathsFuture = CompletableFuture.runAsync(() -> {
            try {
                thumbPathGenerationController.processAllThumbPaths();
            } catch (Exception e) {
                logger.error("Error in Thumb Paths processing", e);
                throw new CompletionException(e);
            }
        });

        // Wait for both tasks to complete
        CompletableFuture.allOf(googleSheetsFuture, thumbPathsFuture).join();
        
        logger.info("Completed parallel processing of Google Sheets and Thumb Paths");
    }
} 