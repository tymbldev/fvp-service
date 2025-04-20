package com.fvp.controller;

import com.fvp.config.GoogleSheetsBatchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/google-sheets")
@RequiredArgsConstructor
public class GoogleSheetsController {

    private final GoogleSheetsBatchConfig googleSheetsBatchConfig;

    @PostMapping("/process")
    public ResponseEntity<String> processGoogleSheets() {
        try {
            googleSheetsBatchConfig.processGoogleSheets();
            return ResponseEntity.ok("Google Sheets processing started successfully");
        } catch (Exception e) {
            log.error("Error triggering Google Sheets processing", e);
            return ResponseEntity.status(500)
                    .body("Error processing Google Sheets: " + e.getMessage());
        }
    }
} 