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
import org.slf4j.LoggerFactory;
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

  private static final Logger logger = LoggerFactory.getLogger(ThumbPathGenerationController.class);
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

  private boolean processLink(Link link) {
    if (link.getThumbnail() == null || link.getThumbnail().isEmpty()) {
      logger.warn("Link ID {} has no thumbnail URL", link.getId());
      return false;
    }

    String thumbUrl = link.getThumbnail();
    try {
      // Generate checksum for the thumbnail URL
      String checksum = generateMd5Checksum(thumbUrl);
      String filename = checksum + ".jpg";

      // Create the local file path
      Path localImagePath = Paths.get(thumbsDir, filename);
      String savedImagePath = filename;

      // Download and process the image
      byte[] imageData = downloadImage(thumbUrl);
      if (imageData == null || imageData.length == 0) {
        logger.error("Failed to download image from URL: {}", thumbUrl);
        link.setThumbPathProcessed(3);
        linkRepository.save(link);
        return false;
      }

      // Resize and save the image
      boolean saved = resizeAndSaveImage(imageData, localImagePath.toString());
      if (!saved) {
        logger.error("Failed to resize and save image for link ID {}", link.getId());
        link.setThumbPathProcessed(3);
        linkRepository.save(link);
        return false;
      }

      // Update link in database
      link.setThumbpath(savedImagePath);
      link.setThumbPathProcessed(1);
      linkRepository.save(link);

      // Update Elasticsearch document
      linkProcessingService.updateElasticsearchDocument(link);

      logger.info("Successfully processed thumbpath for link ID {}: {}", link.getId(),
          savedImagePath);
      return true;

    } catch (Exception e) {
      logger.error("Error processing thumbnail for link ID {}: {}", link.getId(), e.getMessage(),
          e);
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
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet request = new HttpGet(imageUrl);

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode != 200) {
          logger.error("Failed to download image, status code: {}", statusCode);
          return null;
        }

        HttpEntity entity = response.getEntity();
        if (entity != null) {
          return EntityUtils.toByteArray(entity);
        }
      }

      return null;
    } catch (Exception e) {
      logger.error("Error downloading image: {}", e.getMessage(), e);
      return null;
    }
  }

  private boolean resizeAndSaveImage(byte[] imageData, String outputPath) {
    try {
      // Read the image
      BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
      if (originalImage == null) {
        logger.error("Could not read image data");
        return false;
      }

      // Use Thumbnailator to resize and save the image
      Thumbnails.of(originalImage)
                .size(320, 180)
                .outputQuality(0.8)
                .toFile(outputPath);
      return true;
    } catch (IOException e) {
      logger.error("Error processing image: {}", e.getMessage(), e);
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