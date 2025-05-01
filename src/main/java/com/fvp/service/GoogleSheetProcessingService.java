package com.fvp.service;

import com.fvp.document.LinkDocument;
import com.fvp.entity.Link;
import com.fvp.entity.ProcessedSheet;
import com.fvp.repository.ProcessedSheetRepository;
import com.fvp.repository.LinkRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class GoogleSheetProcessingService {

  private static final Logger logger = LoggerFactory.getLogger(GoogleSheetProcessingService.class);
  private static final String STATUS_SHEET_NAME = "status";
  private static final String VET_STATUS_COLUMN = "Validated";
  private static final String SHEET_NAME_COLUMN = "SheetName";
  private static final String APPROVED_STATUS = "Yes";

  private final String dateFormatPattern;
  private final String spreadsheetId;
  private final String apiKey;
  private final String applicationName;
  private final int batchSize;
  private final LinkProcessingService linkProcessingService;
  private final ProcessedSheetRepository processedSheetRepository;
  private final LinkRepository linkRepository;
  private final ElasticsearchClientService elasticsearchClientService;
  private final Map<String, Boolean> sheetStatusCache = new HashMap<>();
  private final Pattern datePattern;

  @Autowired
  public GoogleSheetProcessingService(
      @Value("${google.sheets.date-format-pattern:dd.MM.yy}") String dateFormatPattern,
      @Value("${google.sheets.spreadsheet.id}") String spreadsheetId,
      @Value("${google.sheets.api.key}") String apiKey,
      @Value("${google.sheets.application.name}") String applicationName,
      @Value("${google.sheets.batch-size:100}") int batchSize,
      LinkProcessingService linkProcessingService,
      ProcessedSheetRepository processedSheetRepository,
      LinkRepository linkRepository,
      ElasticsearchClientService elasticsearchClientService) {
    this.dateFormatPattern = dateFormatPattern;
    this.spreadsheetId = spreadsheetId;
    this.apiKey = apiKey;
    this.applicationName = applicationName;
    this.batchSize = batchSize;
    this.linkProcessingService = linkProcessingService;
    this.processedSheetRepository = processedSheetRepository;
    this.linkRepository = linkRepository;
    this.elasticsearchClientService = elasticsearchClientService;

    // Convert SimpleDateFormat pattern to regex pattern
    String regexPattern = dateFormatPattern
        .replace("dd", "\\d{2}")
        .replace("MM", "\\d{2}")
        .replace("yy", "\\d{2}")
        .replace(".", "\\.");

    this.datePattern = Pattern.compile(regexPattern);
    logger.info("Sheet date pattern set to: {}", regexPattern);
  }

  @Scheduled(cron = "${google.sheets.cron-expression:0 0 */6 * * *}")
  public void processGoogleSheets() {
    logger.info("Starting Google Sheets processing");
    try {
      Sheets sheetsService = getSheetsService();
      // First, get the status of all sheets to identify which ones to process
      Map<String, Boolean> sheetApprovalStatus = fetchSheetStatuses(sheetsService);
      
      // Process each sheet that is approved and hasn't been processed yet
      processApprovedSheets(sheetsService, sheetApprovalStatus);
      
      logger.info("Completed Google Sheets processing");
    } catch (Exception e) {
      logger.error("Error in Google Sheets processing", e);
      throw new RuntimeException("Failed to process Google Sheets", e);
    }
  }
  
  private Map<String, Boolean> fetchSheetStatuses(Sheets sheetsService) throws IOException {
    Map<String, Boolean> sheetStatuses = new HashMap<>();
    
    // Fetch the status sheet
    List<Map<String, String>> statusRows = fetchSheet(sheetsService, STATUS_SHEET_NAME, true);
    
    // Process status sheet to determine which sheets are approved
    for (Map<String, String> row : statusRows) {
      String sheetName = row.get(SHEET_NAME_COLUMN);
      String vetStatus = row.get(VET_STATUS_COLUMN);
      
      if (sheetName == null || vetStatus == null || sheetName.equals(STATUS_SHEET_NAME)) {
        continue;
      }
      
      boolean isApproved = vetStatus.toLowerCase().equals(APPROVED_STATUS.toLowerCase());
      sheetStatuses.put(sheetName, isApproved);
      
      // Cache the status
      sheetStatusCache.put(sheetName.toLowerCase(), isApproved);
    }
    
    return sheetStatuses;
  }
  
  private void processApprovedSheets(Sheets sheetsService, Map<String, Boolean> sheetApprovalStatus) throws IOException {
    List<Link> allLinks = new ArrayList<>();
    
    // Process each approved sheet that hasn't been processed yet
    for (Map.Entry<String, Boolean> entry : sheetApprovalStatus.entrySet()) {
      String sheetName = entry.getKey();
      boolean isApproved = entry.getValue();
      
      // Skip if not approved
      if (!isApproved) {
        logger.info("Skipping unapproved sheet: {}", sheetName);
        
        // Record that we've checked this sheet and it's not approved
        markSheetAsProcessed(sheetName, false, 0);
        continue;
      }
      
      // Skip if not in date format
      if (!isDateFormattedSheet(sheetName)) {
        logger.info("Skipping non-date-formatted sheet: {}", sheetName);
        continue;
      }
      
      // Skip if already processed
      if (isSheetAlreadyProcessed(sheetName)) {
        logger.info("Skipping already processed sheet: {}", sheetName);
        continue;
      }
      
      // Fetch and process the sheet
      logger.info("Processing approved sheet: {}", sheetName);
      List<Map<String, String>> rows = fetchSheet(sheetsService, sheetName, false);
      List<Link> sheetLinks = processSheet(spreadsheetId, sheetName, rows);
      allLinks.addAll(sheetLinks);
      
      // Mark sheet as processed
      markSheetAsProcessed(sheetName, true, rows.size());
    }
    
    // Save all links in batches
    if (!allLinks.isEmpty()) {
      saveLinksInBatches(allLinks);
      logger.info("Saved {} links in batches", allLinks.size());
    }
  }

  private boolean isSheetAlreadyProcessed(String sheetName) {
    return processedSheetRepository.existsBySheetNameAndWorkbookId(sheetName, spreadsheetId);
  }
  
  private void markSheetAsProcessed(String sheetName, boolean isApproved, int recordsProcessed) {
    ProcessedSheet processedSheet = new ProcessedSheet(
        sheetName,
        isApproved,
        recordsProcessed,
        1, // Default tenant ID
        spreadsheetId
    );
    processedSheetRepository.save(processedSheet);
    processedSheetRepository.flush(); // Explicitly flush to ensure it's written to DB
    logger.info("Marked sheet {} as processed with {} records", sheetName, recordsProcessed);
  }

  private List<Map<String, String>> fetchSheet(Sheets sheetsService, String sheetName,
      boolean statusSheet) throws IOException {
    String range = sheetName + "!A2:Z";
    ValueRange response = sheetsService.spreadsheets().values()
        .get(spreadsheetId, range)
        .setKey(apiKey)
        .execute();

    List<List<Object>> values = response.getValues();
    if (values == null || values.isEmpty()) {
      logger.warn("No data found in sheet: {}", sheetName);
      return new ArrayList<>();
    }
    if (statusSheet) {
      return values.stream()
          .map(row -> {
            try {
              return convertStatusRowToMap(row);
            } catch (Exception e) {
              logger.warn("Error converting status row in sheet {}: {}", sheetName, e.getMessage());
              return null;
            }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } else {
      return values.stream()
          .map(row -> {
            try {
              return convertRowToMap(row);
            } catch (Exception e) {
              logger.warn("Error converting row in sheet {}: {}", sheetName, e.getMessage());
              return null;
            }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
  }

  private Map<String, String> convertStatusRowToMap(List<Object> row) {
    Map<String, String> map = new HashMap<>();
    // Assuming columns are in order: title, link, thumbnail, duration, quality, category, star
    if (row.size() > 0) {
      map.put("SheetName", String.valueOf(row.get(0)));
    }
    if (row.size() > 1) {
      map.put("Validated", String.valueOf(row.get(1)));
    }
    return map;
  }

  private Map<String, String> convertRowToMap(List<Object> row) {
    Map<String, String> map = new HashMap<>();
    // Assuming columns are in order: title, link, thumbnail, duration, quality, category, star
    if (row.size() > 0) {
      map.put("link", String.valueOf(row.get(0)));
    }
    if (row.size() > 1) {
      map.put("source", String.valueOf(row.get(1)));
    }
    if (row.size() > 2) {
      map.put("thumbnail", String.valueOf(row.get(2)));
    }
    if (row.size() > 3) {
      map.put("title", String.valueOf(row.get(3)));
    }
    if (row.size() > 4) {
      map.put("category", String.valueOf(row.get(4)));
    }
    if (row.size() > 5) {
      map.put("star", String.valueOf(row.get(5)));
    }
    if (row.size() > 6) {
      map.put("duration", String.valueOf(row.get(6)));
    }
    if (row.size() > 7) {
      String tenantId;
      try {
        Object value = row.get(7);
        tenantId = (value == null || "".equalsIgnoreCase(value.toString())) ? "1"
            : String.valueOf(Integer.parseInt(value.toString()));
      } catch (NumberFormatException e) {
        tenantId = "1";
      }
      map.put("tenantId", tenantId);
    }
    return map;
  }

  private boolean isDateFormattedSheet(String sheetName) {
    return datePattern.matcher(sheetName).matches();
  }

  private boolean isSheetApproved(String sheetName) {
    return Optional.ofNullable(sheetStatusCache.get(sheetName.toLowerCase()))
        .orElse(false);
  }

  private List<Link> processSheet(String workbookId, String sheetName,
      List<Map<String, String>> rows) {
    logger.info("Processing approved sheet: {} with {} rows", sheetName, rows.size());
    List<Link> links = new ArrayList<>();
    
    // Track total processing time
    long totalProcessingTimeMs = 0;
    int successCount = 0;
    int failureCount = 0;
    
    // Process each row individually
    for (Map<String, String> row : rows) {
      long startTimeMs = System.currentTimeMillis();
      
      try {
        Link link = createLinkFromRow(workbookId, sheetName, row);
        if (link != null) {
          // Extract categories and models from the row
          String categories = row.get("category");
          String models = row.get("star");

          // Process the link with its categories and models
          linkProcessingService.processLink(link, categories, models);
          links.add(link);
          
          // Calculate and log time taken for this row
          long endTimeMs = System.currentTimeMillis();
          long processingTimeMs = endTimeMs - startTimeMs;
          totalProcessingTimeMs += processingTimeMs;
          successCount++;
          
          logger.info("Row processed successfully in {} ms: {} (title: {})", 
              processingTimeMs, link.getLink(), link.getTitle());
        }
      } catch (Exception e) {
        // Calculate time even for failures
        long endTimeMs = System.currentTimeMillis();
        long processingTimeMs = endTimeMs - startTimeMs;
        totalProcessingTimeMs += processingTimeMs;
        failureCount++;
        
        logger.error("Error processing row in sheet {} (took {} ms): {}", 
            sheetName, processingTimeMs, e.getMessage());
      }
    }
    
    // Log overall statistics
    if (rows.size() > 0) {
      double avgProcessingTimeMs = rows.size() > 0 ? (double) totalProcessingTimeMs / rows.size() : 0;
      logger.info("Sheet {} processing complete: {} rows processed ({} success, {} failures) in {} ms (avg: {:.2f} ms/row)",
          sheetName, rows.size(), successCount, failureCount, totalProcessingTimeMs, avgProcessingTimeMs);
    }
    
    return links;
  }

  private Link createLinkFromRow(String workbookId, String sheetName, Map<String, String> row) {
    try {
      Link link = new Link();
      int tenantId;
      try {
        tenantId = Integer.parseInt(row.get("tenantId"));
      } catch (Exception e) {
        tenantId = 1;
      }
      link.setTenantId(tenantId);
      link.setTitle(row.get("title"));
      link.setLink(row.get("link"));
      link.setThumbnail(row.get("thumbnail"));
      link.setDuration(parseDuration(row.get("duration")));
      link.setQuality(row.get("quality"));
      link.setSheetName(sheetName);
      
      // Set source field with either the value from the row or a default value
      String source = row.get("source");
      if (source == null || source.isEmpty() || "null".equalsIgnoreCase(source)) {
        source = "google_sheets";  // Default source value
        logger.info("Using default source value for link: {}", link.getLink());
      }
      link.setSource(source);
      
      // Set default thumbPath if it's required and not provided
      link.setThumbpath(link.getSheetName() + "/" + UUID.randomUUID().toString());
      
      // Set createdOn timestamp
      link.setCreatedOn(LocalDateTime.now());
      
      // Set random order for sorting
      link.setRandomOrder(new Random().nextInt(10000));
      
      return link;
    } catch (Exception e) {
      logger.error("Error creating link from row: {}", e.getMessage());
      return null;
    }
  }

  private void saveLinksInBatches(List<Link> links) {
    if (links.isEmpty()) {
      return;
    }
    
    try {
      List<Link> savedLinks = linkRepository.saveAll(links);
      linkRepository.flush(); // Explicitly flush to ensure they're written to DB
      logger.info("Saved {} links to database", savedLinks.size());
    
      // Save to Elasticsearch in batches
      for (Link link : savedLinks) {
        try {
          LinkDocument doc = createLinkDocument(link);
          elasticsearchClientService.saveLinkDocument(doc);
        } catch (Exception e) {
          logger.error("Error saving document to Elasticsearch: {}", e.getMessage());
        }
      }
      logger.info("Saved {} documents to Elasticsearch", links.size());
    } catch (Exception e) {
      logger.error("Error saving links in batch: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to save links batch", e);
    }
  }

  private LinkDocument createLinkDocument(Link link) {
    LinkDocument doc = new LinkDocument();
    doc.setId(link.getId().toString());
    doc.setTenantId(link.getTenantId());
    doc.setTitle(link.getTitle());
    doc.setLink(link.getLink());
    doc.setThumbnail(link.getThumbnail());
    doc.setDuration(link.getDuration());
    doc.setSheetName(link.getSheetName());
    doc.setCreatedAt(new Date());
    doc.setSearchableText(generateSearchableText(link));
    return doc;
  }

  private String generateSearchableText(Link link) {
    return String.format("%s %s", link.getTitle(), link.getSheetName());
  }

  private Integer parseDuration(String duration) {
    try {
      return duration != null ? Integer.parseInt(duration) : null;
    } catch (NumberFormatException e) {
      logger.warn("Invalid duration format: {}", duration);
      return null;
    }
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