package com.fvp.controller;

import com.fvp.entity.Link;
import com.fvp.repository.ElasticsearchLinkCategoryRepository;
import com.fvp.repository.LinkRepository;
import com.fvp.repository.LinkModelRepository;
import com.fvp.service.LinkCategoryService;
import com.fvp.util.LoggingUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cleanup")
public class CleanupController {

  private static final Logger logger = LoggingUtil.getLogger(CleanupController.class);

  private final LinkRepository linkRepository;
  private final LinkModelRepository linkModelRepository;
  private final LinkCategoryService linkCategoryService;
  private final ElasticsearchLinkCategoryRepository elasticsearchLinkCategoryRepository;
  private final String thumbsDirectory;

  @Autowired
  public CleanupController(
      LinkRepository linkRepository,
      LinkModelRepository linkModelRepository,
      LinkCategoryService linkCategoryService,
      ElasticsearchLinkCategoryRepository elasticsearchLinkCategoryRepository,
      @Value("${fvp.thumbs.directory}") String thumbsDirectory) {
    this.linkRepository = linkRepository;
    this.linkModelRepository = linkModelRepository;
    this.linkCategoryService = linkCategoryService;
    this.elasticsearchLinkCategoryRepository = elasticsearchLinkCategoryRepository;
    this.thumbsDirectory = thumbsDirectory;
  }

  /**
   * Cleanup endpoint to remove a link by ID from database, Elasticsearch, and thumbnail file
   * @param linkId The ID of the link to cleanup
   * @return ResponseEntity with cleanup results
   */
  @DeleteMapping("/link/{linkId}")
  public ResponseEntity<Map<String, Object>> cleanupLink(@PathVariable Integer linkId) {
    logger.info("Received cleanup request for link ID: {}", linkId);
    long startTime = System.currentTimeMillis();
    
    Map<String, Object> response = new HashMap<>();
    Map<String, Boolean> cleanupResults = new HashMap<>();
    
    try {
      // First, get the link to extract thumbnail information
      Optional<Link> linkOptional = linkRepository.findById(linkId);
      if (!linkOptional.isPresent()) {
        logger.warn("Link with ID {} not found in database", linkId);
        response.put("success", false);
        response.put("message", "Link with ID " + linkId + " not found");
        response.put("cleanupResults", cleanupResults);
        return ResponseEntity.notFound().build();
      }
      
      Link link = linkOptional.get();
      logger.info("Found link: ID={}, Title='{}', Thumbpath='{}'", 
          link.getId(), link.getTitle(), link.getThumbpath());
      
      // 1. Cleanup related entities (LinkCategory and LinkModel)
      boolean relatedEntitiesCleaned = cleanupRelatedEntities(linkId);
      cleanupResults.put("relatedEntities", relatedEntitiesCleaned);
      
      // 2. Cleanup Elasticsearch document
      boolean elasticsearchCleaned = cleanupElasticsearchDocument(linkId);
      cleanupResults.put("elasticsearch", elasticsearchCleaned);
      
      // 3. Cleanup thumbnail file
      boolean thumbnailCleaned = cleanupThumbnailFile(link);
      cleanupResults.put("thumbnailFile", thumbnailCleaned);
      
      // 4. Finally, delete the main Link entity
      boolean linkDeleted = cleanupMainLinkEntity(linkId);
      cleanupResults.put("mainLinkEntity", linkDeleted);
      
      long duration = System.currentTimeMillis() - startTime;
      
      // Determine overall success
      boolean overallSuccess = relatedEntitiesCleaned && elasticsearchCleaned && 
                              thumbnailCleaned && linkDeleted;
      
      response.put("success", overallSuccess);
      response.put("message", overallSuccess ? 
          "Link cleanup completed successfully" : 
          "Link cleanup completed with some failures");
      response.put("linkId", linkId);
      response.put("linkTitle", link.getTitle());
      response.put("cleanupResults", cleanupResults);
      response.put("duration", duration + " ms");
      
      logger.info("Cleanup completed for link ID {} in {} ms. Results: {}", 
          linkId, duration, cleanupResults);
      
      return ResponseEntity.ok(response);
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logger.error("Error during cleanup of link ID {} after {} ms: {}", 
          linkId, duration, e.getMessage(), e);
      
      response.put("success", false);
      response.put("message", "Error during cleanup: " + e.getMessage());
      response.put("linkId", linkId);
      response.put("cleanupResults", cleanupResults);
      response.put("duration", duration + " ms");
      
      return ResponseEntity.status(500).body(response);
    }
  }

  /**
   * Cleanup related entities (LinkCategory and LinkModel)
   */
  private boolean cleanupRelatedEntities(Integer linkId) {
    try {
      logger.info("Cleaning up related entities for link ID: {}", linkId);
      
      // Delete LinkModel entities
      linkModelRepository.deleteByLinkId(linkId);
      logger.info("Deleted LinkModel entities for link ID: {}", linkId);
      
      // Delete LinkCategory entities
      linkCategoryService.deleteByLinkId(linkId);
      logger.info("Deleted LinkCategory entities for link ID: {}", linkId);
      
      return true;
    } catch (Exception e) {
      logger.error("Error cleaning up related entities for link ID {}: {}", 
          linkId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Cleanup Elasticsearch document
   */
  private boolean cleanupElasticsearchDocument(Integer linkId) {
    try {
      logger.info("Cleaning up Elasticsearch document for link ID: {}", linkId);
      elasticsearchLinkCategoryRepository.deleteByLinkId(linkId);
      logger.info("Deleted Elasticsearch document for link ID: {}", linkId);
      return true;
    } catch (Exception e) {
      logger.error("Error cleaning up Elasticsearch document for link ID {}: {}", 
          linkId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Cleanup thumbnail file
   */
  private boolean cleanupThumbnailFile(Link link) {
    try {
      String thumbpath = link.getThumbpath();
      if (thumbpath == null || thumbpath.isEmpty() || "NA".equals(thumbpath)) {
        logger.info("No thumbnail file to cleanup for link ID: {} (thumbpath: '{}')", 
            link.getId(), thumbpath);
        return true; // No file to delete, consider it successful
      }
      
      // Construct the file path
      Path thumbnailPath = Paths.get(thumbsDirectory, thumbpath);
      File thumbnailFile = thumbnailPath.toFile();
      
      if (!thumbnailFile.exists()) {
        logger.info("Thumbnail file does not exist for link ID: {} (path: '{}')", 
            link.getId(), thumbnailPath);
        return true; // File doesn't exist, consider it successful
      }
      
      boolean deleted = thumbnailFile.delete();
      if (deleted) {
        logger.info("Successfully deleted thumbnail file for link ID: {} (path: '{}')", 
            link.getId(), thumbnailPath);
      } else {
        logger.warn("Failed to delete thumbnail file for link ID: {} (path: '{}')", 
            link.getId(), thumbnailPath);
      }
      
      return deleted;
    } catch (Exception e) {
      logger.error("Error cleaning up thumbnail file for link ID {}: {}", 
          link.getId(), e.getMessage(), e);
      return false;
    }
  }

  /**
   * Cleanup main Link entity
   */
  private boolean cleanupMainLinkEntity(Integer linkId) {
    try {
      logger.info("Deleting main Link entity for link ID: {}", linkId);
      linkRepository.deleteById(linkId);
      logger.info("Successfully deleted Link entity for link ID: {}", linkId);
      return true;
    } catch (Exception e) {
      logger.error("Error deleting main Link entity for link ID {}: {}", 
          linkId, e.getMessage(), e);
      return false;
    }
  }

}
