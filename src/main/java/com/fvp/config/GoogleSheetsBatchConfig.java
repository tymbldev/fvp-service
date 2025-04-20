package com.fvp.config;

import com.fvp.document.LinkDocument;
import com.fvp.repository.LinkDocumentRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSheetsBatchConfig {

    private final LinkDocumentRepository linkDocumentRepository;

    @Value("${google.sheets.spreadsheet.id}")
    private String spreadsheetId;

    @Value("${google.sheets.api.key}")
    private String apiKey;

    @Value("${google.sheets.application.name}")
    private String applicationName;

    @Value("${google.sheets.sheet-names:Sheet1}")
    private List<String> sheetNames;

    @Value("${google.sheets.batch-size:100}")
    private int batchSize;

    @Value("${google.sheets.cron-expression:0 0 */6 * * *}")
    private String cronExpression;

    @Scheduled(cron = "${google.sheets.cron-expression:0 0 */6 * * *}")
    public void processGoogleSheets() {
        log.info("Starting Google Sheets processing");
        try {
            List<LinkDocument> allLinks = new ArrayList<>();
            for (String sheetName : sheetNames) {
                try {
                    List<LinkDocument> sheetLinks = fetchAndProcessSheet(sheetName);
                    allLinks.addAll(sheetLinks);
                } catch (IOException | GeneralSecurityException e) {
                    log.error("Error processing sheet {}: {}", sheetName, e.getMessage());
                }
            }
            if (!allLinks.isEmpty()) {
                saveLinksInBatches(allLinks);
                log.info("Completed Google Sheets processing. Total links processed: {}", allLinks.size());
            } else {
                log.warn("No links were processed from any sheet");
            }
        } catch (Exception e) {
            log.error("Error in Google Sheets processing", e);
            throw new RuntimeException("Failed to process Google Sheets", e);
        }
    }

    private List<LinkDocument> fetchAndProcessSheet(String sheetName) throws IOException, GeneralSecurityException {
        log.info("Processing sheet: {}", sheetName);
        Sheets sheetsService = getSheetsService();
        String range = sheetName + "!A2:Z";
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .setKey(apiKey)
                .execute();

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            log.warn("No data found in sheet: {}", sheetName);
            return new ArrayList<>();
        }

        return values.stream()
                .map(row -> createLinkDocument(row, sheetName))
                .collect(Collectors.toList());
    }

    private void saveLinksInBatches(List<LinkDocument> links) {
        for (int i = 0; i < links.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, links.size());
            List<LinkDocument> batch = links.subList(i, endIndex);
            linkDocumentRepository.saveAll(batch);
            log.info("Saved batch of {} links", batch.size());
        }
    }

    private LinkDocument createLinkDocument(List<Object> row, String sheetName) {
        LinkDocument doc = new LinkDocument();
        doc.setId(String.valueOf(row.get(0)));
        doc.setTenantId(Integer.parseInt(String.valueOf(row.get(1))));
        doc.setTitle(String.valueOf(row.get(2)));
        doc.setCategories(parseCategories(String.valueOf(row.get(3))));
        doc.setDuration(Integer.parseInt(String.valueOf(row.get(4))));
        doc.setThumbnail(String.valueOf(row.get(5)));
        doc.setThumbPath(String.valueOf(row.get(6)));
        doc.setSheetName(String.valueOf(row.get(7)));
        doc.setLink(String.valueOf(row.get(8)));
        doc.setSource(String.valueOf(row.get(9)));
        doc.setStars(Integer.parseInt(String.valueOf(row.get(10))));
        doc.setCreatedAt(new Date());
        doc.setTrailer(String.valueOf(row.get(12)));
        doc.setSearchableText(generateSearchableText(doc));
        return doc;
    }

    private List<String> parseCategories(String categoriesStr) {
        if (categoriesStr == null || categoriesStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(categoriesStr.split(","));
    }

    private String generateSearchableText(LinkDocument doc) {
        return String.format("%s %s %s",
                doc.getTitle(),
                String.join(" ", doc.getCategories()),
                doc.getSource());
    }

    private Sheets getSheetsService() throws GeneralSecurityException, IOException {
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                null)
                .setApplicationName(applicationName)
                .build();
    }
} 