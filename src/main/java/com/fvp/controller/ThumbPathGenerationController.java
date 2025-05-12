package com.fvp.controller;

import com.fvp.entity.Link;
import com.fvp.repository.LinkRepository;
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

@RestController
@RequestMapping("/api/thumbs")
public class ThumbPathGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(ThumbPathGenerationController.class);
    private static final String THUMBS_DIR = "/apps/fvp/thumbs";
    
    private final LinkRepository linkRepository;
    private final LinkProcessingService linkProcessingService;
    private final String thumbsDir;
    
    @Autowired
    public ThumbPathGenerationController(
            LinkRepository linkRepository, 
            LinkProcessingService linkProcessingService,
            @Value("${fvp.thumbs.directory}") String thumbsDir) {
        this.linkRepository = linkRepository;
        this.linkProcessingService = linkProcessingService;
        this.thumbsDir = thumbsDir;
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
        
        final int BATCH_SIZE = 20;
        int currentPage = 0;
        int totalProcessed = 0;
        int totalFailed = 0;
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
            
            int batchProcessed = 0;
            int batchFailed = 0;
            
            logger.info("Processing batch {} with {} records", currentPage, linksPage.getContent().size());
            
            // Process each link in the current batch
            for (Link link : linksPage.getContent()) {
                try {
                    boolean success = processLink(link);
                    if (success) {
                        batchProcessed++;
                    } else {
                        batchFailed++;
                    }
                } catch (Exception e) {
                    logger.error("Error processing link ID {}: {}", link.getId(), e.getMessage(), e);
                    batchFailed++;
                }
            }
            
            totalProcessed += batchProcessed;
            totalFailed += batchFailed;
            
            logger.info("Batch {} completed. Processed: {}, Failed: {}, Total progress: {}/{}",
                    currentPage, batchProcessed, batchFailed, totalProcessed + totalFailed, totalRecords);
            
            // Check if we've processed all records
            if (totalProcessed + totalFailed >= totalRecords || linksPage.isLast()) {
                hasMoreRecords = false;
            } else {
                currentPage++;
            }
        }
        
        long totalDuration = System.currentTimeMillis() - globalStartTime;
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Processing completed");
        response.put("totalRecords", totalRecords);
        response.put("processed", totalProcessed);
        response.put("failed", totalFailed);
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
            String savedImagePath =   filename;
            
            // Download and process the image
            byte[] imageData = downloadImage(thumbUrl);
            if (imageData == null || imageData.length == 0) {
                logger.error("Failed to download image from URL: {}", thumbUrl);
                return false;
            }
            
            // Resize and save the image
            boolean saved = resizeAndSaveImage(imageData, localImagePath.toString());
            if (!saved) {
                logger.error("Failed to resize and save image for link ID {}", link.getId());
                return false;
            }
            
            // Update link in database
            link.setThumbpath(savedImagePath);
            link.setThumbPathProcessed(1);
            linkRepository.save(link);
            
            // Update Elasticsearch document
            linkProcessingService.updateElasticsearchDocument(link);
            
            logger.info("Successfully processed thumbpath for link ID {}: {}", link.getId(), savedImagePath);
            return true;
            
        } catch (Exception e) {
            logger.error("Error processing thumbnail for link ID {}: {}", link.getId(), e.getMessage(), e);
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
            
            // Resize the image (simple resize, ideally you'd use a library like Thumbnailator)
            java.awt.Image resizedImage = originalImage.getScaledInstance(320, 180, java.awt.Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
            outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);
            
            // Save the image
            File outputFile = new File(outputPath);
            return ImageIO.write(outputImage, "jpg", outputFile);
            
        } catch (IOException e) {
            logger.error("Error processing image: {}", e.getMessage(), e);
            return false;
        }
    }
} 