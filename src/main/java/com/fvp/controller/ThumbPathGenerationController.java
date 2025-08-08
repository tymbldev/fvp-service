package com.fvp.controller;

import com.fvp.entity.Link;
import com.fvp.repository.LinkRepository;
import com.fvp.service.GoogleSheetProcessingService;
import com.fvp.service.LinkProcessingService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import com.fvp.util.LoggingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.coobird.thumbnailator.Thumbnails;

@RestController
@RequestMapping("/api/thumbs")
public class ThumbPathGenerationController {

  private static final Logger logger = LoggingUtil.getLogger(ThumbPathGenerationController.class);
  private static final String THUMBS_DIR = "/apps/fvp/thumbs";
  private static final int BATCH_SIZE = 50;
  private static final int NUM_THREADS = 20;

  private final LinkRepository linkRepository;
  private final LinkProcessingService linkProcessingService;
  private final String thumbsDir;
  private final ExecutorService executorService;
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);
  private final GoogleSheetProcessingService googleSheetProcessingService;


  @Autowired
  public ThumbPathGenerationController(
      LinkRepository linkRepository,
      LinkProcessingService linkProcessingService,
      GoogleSheetProcessingService googleSheetProcessingService,
      @Value("${fvp.thumbs.directory}") String thumbsDir) {
    this.linkRepository = linkRepository;
    this.linkProcessingService = linkProcessingService;
    this.googleSheetProcessingService = googleSheetProcessingService;
    this.thumbsDir = thumbsDir;
    this.executorService = Executors.newFixedThreadPool(NUM_THREADS);
  }


  @GetMapping("/process")
  public ResponseEntity<Map<String, Object>> processAllThumbPaths() {
    logger.info("Received request to process all thumbnails");
    long startTime = System.currentTimeMillis();
    
    // Create thumbs directory if it doesn't exist
    File thumbsDirectory = new File(thumbsDir);
    if (!thumbsDirectory.exists()) {
      boolean created = thumbsDirectory.mkdirs();
      if (!created) {
        logger.error("Failed to create thumbs directory at: {}", thumbsDir);
      } else {
        logger.info("Created thumbs directory at: {}", thumbsDirectory.getAbsolutePath());
      }
    } else {
      logger.info("Using existing thumbs directory at: {}", thumbsDirectory.getAbsolutePath());
    }

    logger.info("Starting full processing of all links with NA thumbpath values");

    // Get total count first
    long totalRecords = countLinksWithNAThumbPath();

    if (totalRecords == 0) {
      Map<String, Object> response = new HashMap<>();
      response.put("message", "No links with NA thumbpath found");
      response.put("processed", 0);
      response.put("failed", 0);
      return ResponseEntity.ok(response);
    }

    logger.info("Found {} links with NA thumbpath to process", totalRecords);

    int currentPage = 0;
    AtomicInteger totalProcessed = new AtomicInteger(0);
    AtomicInteger totalFailed = new AtomicInteger(0);
    long globalStartTime = System.currentTimeMillis();

    // Process in batches until all records are processed
    boolean hasMoreRecords = true;
    while (hasMoreRecords) {
      // Get a batch of records
      Pageable pageable = PageRequest.of(currentPage, BATCH_SIZE);
      Page<Link> linksPage = findLinksWithNAThumbPath(pageable);

      if (linksPage.isEmpty()) {
        hasMoreRecords = false;
        continue;
      }

      List<CompletableFuture<Void>> futures = new ArrayList<>();

      logger.info("Processing batch {} with {} records", currentPage,
          linksPage.getContent().size());

      // Process each link in the current batch in parallel
      for (Link link : linksPage.getContent()) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
          try {
            boolean success = processLink(link);
            if (success) {
              totalProcessed.incrementAndGet();
            } else {
              totalFailed.incrementAndGet();
            }
          } catch (Exception e) {
            logger.error("Error processing link ID {}: {}", link.getId(), e.getMessage(), e);
            totalFailed.incrementAndGet();
          }
        }, executorService);
        futures.add(future);
      }

      // Wait for all futures to complete
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      logger.info("Batch {} completed. Processed: {}, Failed: {}, Total progress: {}/{}",
          currentPage, totalProcessed.get(), totalFailed.get(),
          totalProcessed.get() + totalFailed.get(), totalRecords);

      // Check if we've processed all records
      if (totalProcessed.get() + totalFailed.get() >= totalRecords || linksPage.isLast()) {
        hasMoreRecords = false;
      } else {
        currentPage++;
      }
    }
    long totalDuration = System.currentTimeMillis() - globalStartTime;
    long requestDuration = System.currentTimeMillis() - startTime;
    
    logger.info("Thumbnail processing completed - Total records: {}, Processed: {}, Failed: {}, Total duration: {} seconds, Request duration: {} ms", 
        totalRecords, totalProcessed.get(), totalFailed.get(), 
        TimeUnit.MILLISECONDS.toSeconds(totalDuration), requestDuration);
    
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Processing completed");
    response.put("totalRecords", totalRecords);
    response.put("processed", totalProcessed.get());
    response.put("failed", totalFailed.get());
    response.put("totalDuration", TimeUnit.MILLISECONDS.toSeconds(totalDuration) + " seconds");

    return ResponseEntity.ok(response);

  }

  @GetMapping("/stats")
  public ResponseEntity<Map<String, Object>> getStats() {
    Map<String, Object> stats = new HashMap<>();

    long totalLinks = linkRepository.count();
    long naThumbpaths = countLinksWithNAThumbPath();
    long processedThumbpaths = totalLinks - naThumbpaths;

    stats.put("totalLinks", totalLinks);
    stats.put("processedThumbpaths", processedThumbpaths);
    stats.put("pendingThumbpaths", naThumbpaths);
    stats.put("progressPercentage",
        totalLinks > 0 ? (processedThumbpaths * 100.0 / totalLinks) : 0);

    return ResponseEntity.ok(stats);
  }

  private Page<Link> findLinksWithNAThumbPath(Pageable pageable) {
    return linkRepository.findByThumbpathAndThumbPathProcessed("NA", 0, pageable);
  }

  private long countLinksWithNAThumbPath() {
    return linkRepository.countByThumbpathAndThumbPathProcessed("NA", 0);
  }

  public boolean processLinkById(Integer linkId) {
    logger.info("Processing single link by ID: {}", linkId);
    
    try {
      Link link = linkRepository.findById(linkId).orElse(null);
      if (link == null) {
        logger.error("Link with ID {} not found", linkId);
        return false;
      }
      
      return processLink(link);
    } catch (Exception e) {
      logger.error("Error processing link ID {}: {}", linkId, e.getMessage(), e);
      return false;
    }
  }

  private boolean processLink(Link link) {
    long startTime = System.currentTimeMillis();
    logger.info("Starting to process link ID: {}, thumbnail URL: {}", link.getId(), link.getThumbnail());
    
    if (link.getThumbnail() == null || link.getThumbnail().isEmpty()) {
      logger.warn("Link ID {} has no thumbnail URL - skipping processing", link.getId());
      return false;
    }

    String thumbUrl = link.getThumbnail();
    try {
      // Generate checksum for the thumbnail URL
      long checksumStartTime = System.currentTimeMillis();
      String checksum = generateMd5Checksum(thumbUrl);
      String filename = checksum + ".jpg";
      long checksumDuration = System.currentTimeMillis() - checksumStartTime;
      
      logger.info("Generated checksum for link ID {}: {} (duration: {} ms)", 
          link.getId(), checksum, checksumDuration);

      // Create the local file path
      Path localImagePath = Paths.get(thumbsDir, filename);
      String savedImagePath = filename;
      
      logger.info("Local image path for link ID {}: {}", link.getId(), localImagePath.toString());

      // Download and process the image
      long downloadStartTime = System.currentTimeMillis();
      byte[] imageData = downloadImage(thumbUrl);
      long downloadDuration = System.currentTimeMillis() - downloadStartTime;
      
      if (imageData == null || imageData.length == 0) {
        logger.error("Failed to download image from URL: {} for link ID {} (download time: {} ms)", 
            thumbUrl, link.getId(), downloadDuration);
        link.setThumbPathProcessed(3);
        linkRepository.save(link);
        return false;
      }
      
      logger.info("Successfully downloaded image for link ID {}: {} bytes (download time: {} ms)", 
          link.getId(), imageData.length, downloadDuration);

      // Resize and save the image
      long resizeStartTime = System.currentTimeMillis();
      boolean saved = resizeAndSaveImage(imageData, localImagePath.toString());
      long resizeDuration = System.currentTimeMillis() - resizeStartTime;
      
      if (!saved) {
        logger.error("Failed to resize and save image for link ID {} (resize time: {} ms)", 
            link.getId(), resizeDuration);
        link.setThumbPathProcessed(3);
        linkRepository.save(link);
        return false;
      }
      
      logger.info("Successfully resized and saved image for link ID {} (resize time: {} ms)", 
          link.getId(), resizeDuration);

      // Update link in database
      long dbUpdateStartTime = System.currentTimeMillis();
      link.setThumbpath(savedImagePath);
      link.setThumbPathProcessed(1);
      linkRepository.save(link);
      long dbUpdateDuration = System.currentTimeMillis() - dbUpdateStartTime;
      
      logger.info("Updated database for link ID {} (db update time: {} ms)", 
          link.getId(), dbUpdateDuration);

      // Update Elasticsearch document
      long esUpdateStartTime = System.currentTimeMillis();
      try {
        linkProcessingService.updateElasticsearchDocument(link);
        long esUpdateDuration = System.currentTimeMillis() - esUpdateStartTime;
        logger.info("Updated Elasticsearch for link ID {} (ES update time: {} ms)", 
            link.getId(), esUpdateDuration);
      } catch (Exception esException) {
        long esUpdateDuration = System.currentTimeMillis() - esUpdateStartTime;
        logger.warn("Failed to update Elasticsearch for link ID {} (ES update time: {} ms): {}", 
            link.getId(), esUpdateDuration, esException.getMessage());
        // Don't fail the entire process if ES update fails
      }

      long totalDuration = System.currentTimeMillis() - startTime;
      logger.info("Successfully processed thumbpath for link ID {}: {} (total time: {} ms)", 
          link.getId(), savedImagePath, totalDuration);
      
      // Log performance metrics for slower operations
      if (totalDuration > 5000) {
        logger.warn("Slow link processing detected for link ID {} - Total time: {} ms", 
            link.getId(), totalDuration);
      }
      
      return true;

    } catch (Exception e) {
      long totalDuration = System.currentTimeMillis() - startTime;
      logger.error("Error processing thumbnail for link ID {} (total time: {} ms): {}", 
          link.getId(), totalDuration, e.getMessage(), e);
      
      // Try to update the link status to indicate failure
      try {
        link.setThumbPathProcessed(3);
        linkRepository.save(link);
        logger.info("Updated link ID {} status to failed (thumbPathProcessed=3)", link.getId());
      } catch (Exception dbException) {
        logger.error("Failed to update link ID {} status after processing error: {}", 
            link.getId(), dbException.getMessage());
      }
      
      return false;
    }
  }

  private String generateMd5Checksum(String input) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest = md.digest(input.getBytes());
    String base64 = Base64.getEncoder().encodeToString(digest);
    return base64.replaceAll("[^a-zA-Z0-9]", "").substring(0, 10);
  }

  private byte[] downloadImage(String imageUrl) {
    long startTime = System.currentTimeMillis();
    logger.info("Starting image download from URL: {}", imageUrl);
    
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet request = new HttpGet(imageUrl);
      
      // Add timeout and user agent headers
      request.setConfig(org.apache.http.client.config.RequestConfig.custom()
          .setConnectTimeout(10000)
          .setSocketTimeout(30000)
          .build());
      request.setHeader("User-Agent", "FVP-Thumbnail-Processor/1.0");

      long requestStartTime = System.currentTimeMillis();
      try (CloseableHttpResponse response = httpClient.execute(request)) {
        long responseTime = System.currentTimeMillis() - requestStartTime;
        int statusCode = response.getStatusLine().getStatusCode();
        
        logger.info("HTTP response received for URL: {} - Status: {}, Response time: {} ms", 
            imageUrl, statusCode, responseTime);

        if (statusCode != 200) {
          logger.error("Failed to download image from URL: {} - Status code: {}, Response time: {} ms", 
              imageUrl, statusCode, responseTime);
          return null;
        }

        HttpEntity entity = response.getEntity();
        if (entity != null) {
          long contentLength = entity.getContentLength();
          logger.info("Content length for URL {}: {} bytes", imageUrl, contentLength);
          
          long readStartTime = System.currentTimeMillis();
          byte[] imageData = EntityUtils.toByteArray(entity);
          long readDuration = System.currentTimeMillis() - readStartTime;
          
          long totalDuration = System.currentTimeMillis() - startTime;
          logger.info("Successfully downloaded image from URL: {} - Size: {} bytes, Read time: {} ms, Total time: {} ms", 
              imageUrl, imageData.length, readDuration, totalDuration);
          
          // Log warning for large images
          if (imageData.length > 10 * 1024 * 1024) { // 10MB
            logger.warn("Large image downloaded from URL: {} - Size: {} MB", 
                imageUrl, imageData.length / (1024 * 1024));
          }
          
          return imageData;
        } else {
          logger.error("No entity content received for URL: {}", imageUrl);
          return null;
        }
      }

    } catch (java.net.SocketTimeoutException e) {
      long totalDuration = System.currentTimeMillis() - startTime;
      logger.error("Timeout error downloading image from URL: {} - Duration: {} ms, Error: {}", 
          imageUrl, totalDuration, e.getMessage());
      return null;
    } catch (java.net.ConnectException e) {
      long totalDuration = System.currentTimeMillis() - startTime;
      logger.error("Connection error downloading image from URL: {} - Duration: {} ms, Error: {}", 
          imageUrl, totalDuration, e.getMessage());
      return null;
    } catch (Exception e) {
      long totalDuration = System.currentTimeMillis() - startTime;
      logger.error("Error downloading image from URL: {} - Duration: {} ms, Error: {}", 
          imageUrl, totalDuration, e.getMessage(), e);
      return null;
    }
  }

  private boolean resizeAndSaveImage(byte[] imageData, String outputPath) {
    long startTime = System.currentTimeMillis();
    logger.info("Starting image resize and save operation for output path: {}", outputPath);
    
    try {
      // Log input data size
      logger.info("Input image data size: {} bytes", imageData.length);
      
      // Read the image
      long readStartTime = System.currentTimeMillis();
      BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
      long readDuration = System.currentTimeMillis() - readStartTime;
      
      if (originalImage == null) {
        logger.error("Could not read image data - ImageIO.read returned null for output path: {}", outputPath);
        return false;
      }
      
      // Log original image details
      int originalWidth = originalImage.getWidth();
      int originalHeight = originalImage.getHeight();
      logger.info("Original image dimensions: {}x{} pixels, read time: {} ms", 
          originalWidth, originalHeight, readDuration);
      
      // Check if image is too small
      if (originalWidth < 320 || originalHeight < 180) {
        logger.warn("Original image is smaller than target size. Original: {}x{}, Target: 320x180", 
            originalWidth, originalHeight);
      }
      
      // Ensure output directory exists
      File outputFile = new File(outputPath);
      File outputDir = outputFile.getParentFile();
      if (!outputDir.exists()) {
        boolean dirCreated = outputDir.mkdirs();
        if (!dirCreated) {
          logger.error("Failed to create output directory: {}", outputDir.getAbsolutePath());
          return false;
        }
        logger.info("Created output directory: {}", outputDir.getAbsolutePath());
      }
      
      // Use Thumbnailator to resize and save the image
      long resizeStartTime = System.currentTimeMillis();
      Thumbnails.of(originalImage)
                .size(320, 180)
                .outputQuality(0.8)
                .toFile(outputPath);
      long resizeDuration = System.currentTimeMillis() - resizeStartTime;
      
      // Verify the output file was created
      File savedFile = new File(outputPath);
      if (!savedFile.exists()) {
        logger.error("Output file was not created at expected path: {}", outputPath);
        return false;
      }
      
      long fileSize = savedFile.length();
      long totalDuration = System.currentTimeMillis() - startTime;
      
      logger.info("Image processing completed successfully - Output file: {} bytes, resize time: {} ms, total time: {} ms", 
          fileSize, resizeDuration, totalDuration);
      
      // Log performance metrics for larger operations
      if (totalDuration > 1000) {
        logger.warn("Slow image processing detected - Total time: {} ms for file: {}", totalDuration, outputPath);
      }
      
      return true;
    } catch (IOException e) {
      long totalDuration = System.currentTimeMillis() - startTime;
      logger.error("Error processing image for output path: {} - Duration: {} ms, Error: {}", 
          outputPath, totalDuration, e.getMessage(), e);
      return false;
    } catch (Exception e) {
      long totalDuration = System.currentTimeMillis() - startTime;
      logger.error("Unexpected error processing image for output path: {} - Duration: {} ms, Error: {}", 
          outputPath, totalDuration, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Update links with thumbPathProcessed=2 to thumbPathProcessed=1 in both MySQL and Elasticsearch.
   * This endpoint uses a single native query to update all records at once.
   *
   * @return ResponseEntity containing the update results
   */
  @PostMapping("/update-processed-status")
  public ResponseEntity<Map<String, Object>> updateProcessedStatus() {
    // Check if processing is already running
    if (!isProcessing.compareAndSet(false, true)) {
      Map<String, Object> response = new HashMap<>();
      response.put("message", "Processing is already running");
      response.put("status", "in_progress");
      return ResponseEntity.ok(response);
    }

    try {
      logger.info("Starting update of links with thumbPathProcessed=2");

      // Get total count first
      long totalRecords = linkRepository.countByThumbPathProcessed(2);

      if (totalRecords == 0) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "No links found with thumbPathProcessed=2");
        response.put("processed", 0);
        response.put("failed", 0);
        return ResponseEntity.ok(response);
      }

      logger.info("Found {} links with thumbPathProcessed=2 to update", totalRecords);
      long startTime = System.currentTimeMillis();

      // Update MySQL using native query
      int updatedCount = linkRepository.updateThumbPathProcessedStatus(2, 1);

      // Update Elasticsearch for all affected records
      List<Link> updatedLinks = linkRepository.findByThumbPathProcessed(1);
      for (Link link : updatedLinks) {
        try {
          linkProcessingService.updateElasticsearchDocument(link);
        } catch (Exception e) {
          logger.error("Error updating Elasticsearch for link ID {}: {}", link.getId(),
              e.getMessage());
        }
      }

      long duration = System.currentTimeMillis() - startTime;
      Map<String, Object> response = new HashMap<>();
      response.put("message", "Processing completed");
      response.put("totalRecords", totalRecords);
      response.put("processed", updatedCount);
      response.put("failed", totalRecords - updatedCount);
      response.put("totalDuration", TimeUnit.MILLISECONDS.toSeconds(duration) + " seconds");

      return ResponseEntity.ok(response);
    } finally {
      isProcessing.set(false);
    }
  }

  @GetMapping("/process-async")
  public ResponseEntity<Map<String, Object>> processAllThumbPathsAsync() {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Thumbnail processing started in background");
    response.put("status", "processing");

    new Thread(() -> {
      try {
        processAllThumbPaths();
      } catch (Exception e) {
        logger.error("Error in async thumbnail processing: {}", e.getMessage(), e);
      }
    }).start();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/update-processed-status-async")
  public ResponseEntity<Map<String, Object>> updateProcessedStatusAsync() {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Status update started in background");
    response.put("status", "processing");

    new Thread(() -> {
      try {
        updateProcessedStatus();
      } catch (Exception e) {
        logger.error("Error in async status update: {}", e.getMessage(), e);
      }
    }).start();

    return ResponseEntity.ok(response);
  }

  /**
   * Dummy controller to resize an existing image file from the thumbs directory.
   * This endpoint takes a filename as query parameter and resizes it using Thumbnailator.
   *
   * @param filename the name of the file to resize
   * @return ResponseEntity containing the resize results
   */
  @GetMapping("/resize-file")
  public ResponseEntity<Map<String, Object>> resizeFile(@RequestParam String filename) {
    Map<String, Object> response = new HashMap<>();
    
    try {
      // Construct the input file path
      Path inputFilePath = Paths.get(thumbsDir, filename);
      File inputFile = inputFilePath.toFile();
      
      if (!inputFile.exists()) {
        response.put("success", false);
        response.put("message", "File not found: " + filename);
        response.put("inputPath", inputFilePath.toString());
        return ResponseEntity.badRequest().body(response);
      }
      
      // Construct the output file path with "_resized" suffix
      String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
      String extension = filename.substring(filename.lastIndexOf('.'));
      String resizedFilename = nameWithoutExt + "_resized" + extension;
      Path outputFilePath = Paths.get(thumbsDir, resizedFilename);
      
      logger.info("Resizing file: {} -> {}", inputFilePath, outputFilePath);
      
      // Use Thumbnailator to resize the image
      Thumbnails.of(inputFile)
                .size(320, 180)
                .outputQuality(0.8)
                .toFile(outputFilePath.toFile());
      
      response.put("success", true);
      response.put("message", "File resized successfully");
      response.put("originalFile", filename);
      response.put("resizedFile", resizedFilename);
      response.put("inputPath", inputFilePath.toString());
      response.put("outputPath", outputFilePath.toString());
      response.put("size", "320x180");
      response.put("quality", "0.8");
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      logger.error("Error resizing file {}: {}", filename, e.getMessage(), e);
      response.put("success", false);
      response.put("message", "Error resizing file: " + e.getMessage());
      response.put("originalFile", filename);
      return ResponseEntity.ok().body(response);
    }
  }
}