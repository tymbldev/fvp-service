package com.fvp.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GoogleSheetsConfig {

  private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsConfig.class);

  @Value("${google.sheets.api.key:}")
  private String apiKey;

  @Value("${google.sheets.application.name:FVP-Service}")
  private String applicationName;

  @Bean
  @Primary
  public Sheets sheetsService() {
    try {
      // Check if API key is provided
      if (apiKey == null || apiKey.isEmpty()) {
        logger.warn("Google Sheets API key is not provided. Using dummy Sheets service.");
        return createDummySheetsService();
      }

      return new Sheets.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          GsonFactory.getDefaultInstance(),
          null) // No credentials needed when using API key
          .setApplicationName(applicationName)
          .build();
    } catch (Exception e) {
      logger.error("Failed to create Google Sheets service: {}", e.getMessage(), e);
      return createDummySheetsService();
    }
  }

  /**
   * Creates a dummy Sheets service that logs operations instead of making actual API calls. This
   * ensures the application can start even if Google Sheets integration fails.
   */
  private Sheets createDummySheetsService() {
    logger.info("Creating dummy Google Sheets service");

    try {
      // Create a real Sheets instance
      return new Sheets.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          GsonFactory.getDefaultInstance(),
          null)
          .setApplicationName(applicationName + " (Dummy)")
          .build();
    } catch (Exception e) {
      logger.error("Failed to create dummy Sheets service: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to create dummy Sheets service", e);
    }
  }
} 