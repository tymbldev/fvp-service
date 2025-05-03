package com.fvp.controller;

import com.fvp.entity.Link;
import com.fvp.repository.LinkRepository;
import com.fvp.service.LinkProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/links/batch")
public class LinkBatchProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(LinkBatchProcessingController.class);
    private static final int BATCH_SIZE = 100;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkProcessingService linkProcessingService;

    @GetMapping("/process-categories-models")
    public ResponseEntity<Map<String, Object>> processCategoriesAndModels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        logger.info("Starting batch processing of categories and models for links");
        
        Page<Link> linkPage = linkRepository.findAll(PageRequest.of(page, size));
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        linkPage.getContent().forEach(link -> {
            try {
                processedCount.incrementAndGet();
                
                // Process categories if present
                if (link.getCategory() != null && !link.getCategory().isEmpty()) {
                    logger.info("Processing categories for link ID: {}", link.getId());
                    linkProcessingService.processCategories(link, link.getCategory());
                }
                
                // Process models if present
                if (link.getStar() != null && !link.getStar().isEmpty()) {
                    logger.info("Processing models for link ID: {}", link.getId());
                    linkProcessingService.processModels(link, link.getStar());
                }
                
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.error("Error processing link ID: {} - {}", link.getId(), e.getMessage());
            }
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalProcessed", processedCount.get());
        response.put("successCount", successCount.get());
        response.put("errorCount", errorCount.get());
        response.put("currentPage", page);
        response.put("totalPages", linkPage.getTotalPages());
        response.put("totalElements", linkPage.getTotalElements());
        
        logger.info("Completed batch processing: {} processed, {} successful, {} errors", 
                processedCount.get(), successCount.get(), errorCount.get());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/process-all-categories-models")
    @Async
    public ResponseEntity<Map<String, Object>> processAllCategoriesAndModels() {
        logger.info("Starting background processing of all links for categories and models");
        
        // Start the background processing
        processAllCategoriesAndModelsAsync();
        
        // Return immediate response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "Background processing of all links for categories and models has been started");
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Process all links for categories and models in the background
     */
    @Async("taskExecutor")
    protected void processAllCategoriesAndModelsAsync() {
        logger.info("Background processing of all links for categories and models started");
        
        long totalLinks = linkRepository.count();
        int totalPages = (int) Math.ceil((double) totalLinks / BATCH_SIZE);
        
        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalError = new AtomicInteger(0);
        
        for (int page = 0; page < totalPages; page++) {
            logger.info("Processing page {} of {}", page + 1, totalPages);
            
            Page<Link> linkPage = linkRepository.findAll(PageRequest.of(page, BATCH_SIZE));
            
            linkPage.getContent().forEach(link -> {
                try {
                    totalProcessed.incrementAndGet();
                    
                    // Process categories if present
                    if (link.getCategory() != null && !link.getCategory().isEmpty()) {
                        logger.info("Processing categories for link ID: {}", link.getId());
                        linkProcessingService.processCategories(link, link.getCategory());
                    }
                    
                    // Process models if present
                    if (link.getStar() != null && !link.getStar().isEmpty()) {
                        logger.info("Processing models for link ID: {}", link.getId());
                        linkProcessingService.processModels(link, link.getStar());
                    }
                    
                    totalSuccess.incrementAndGet();
                } catch (Exception e) {
                    totalError.incrementAndGet();
                    logger.error("Error processing link ID: {} - {}", link.getId(), e.getMessage());
                }
            });
            
            logger.info("Completed page {}: {} processed, {} successful, {} errors", 
                    page + 1, totalProcessed.get(), totalSuccess.get(), totalError.get());
        }
        
        logger.info("Completed background processing of all links: {} processed, {} successful, {} errors", 
                totalProcessed.get(), totalSuccess.get(), totalError.get());
    }
    
    @GetMapping("/process-by-tenant/{tenantId}")
    public ResponseEntity<Map<String, Object>> processByTenantId(
            @PathVariable Integer tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        logger.info("Starting background processing of categories and models for tenant ID: {}", tenantId);
        
        // Start the background processing
        processByTenantIdAsync(tenantId, page, size);
        
        // Return immediate response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("tenantId", tenantId);
        response.put("message", "Background processing for tenant " + tenantId + " has been started");
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Process links for a specific tenant in the background
     */
    @Async("taskExecutor")
    protected void processByTenantIdAsync(Integer tenantId, int page, int size) {
        logger.info("Background processing for tenant ID: {} started", tenantId);
        
        Page<Link> linkPage = linkRepository.findByTenantId(tenantId, PageRequest.of(page, size));
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        linkPage.getContent().forEach(link -> {
            try {
                processedCount.incrementAndGet();
                
                // Process categories if present
                if (link.getCategory() != null && !link.getCategory().isEmpty()) {
                    logger.info("Processing categories for link ID: {}", link.getId());
                    linkProcessingService.processCategories(link, link.getCategory());
                }
                
                // Process models if present
                if (link.getStar() != null && !link.getStar().isEmpty()) {
                    logger.info("Processing models for link ID: {}", link.getId());
                    linkProcessingService.processModels(link, link.getStar());
                }
                
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.error("Error processing link ID: {} - {}", link.getId(), e);
            }
        });
        
        logger.info("Completed background processing for tenant {}: {} processed, {} successful, {} errors", 
                tenantId, processedCount.get(), successCount.get(), errorCount.get());
    }
    
    @GetMapping("/process-by-category")
    public ResponseEntity<Map<String, Object>> processByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        logger.info("Starting background processing of models for links with category: {}", category);
        
        // Start the background processing
        processByCategoryAsync(category, page, size);
        
        // Return immediate response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("category", category);
        response.put("message", "Background processing for category " + category + " has been started");
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Process links with a specific category in the background
     */
    @Async("taskExecutor")
    protected void processByCategoryAsync(String category, int page, int size) {
        logger.info("Background processing for category {} started", category);
        
        // Find links that have the specified category in their category JSON field
        Page<Link> linkPage = linkRepository.findByCategoryContaining(category, PageRequest.of(page, size));
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        linkPage.getContent().forEach(link -> {
            try {
                processedCount.incrementAndGet();
                
                // Process models if present
                if (link.getStar() != null && !link.getStar().isEmpty()) {
                    logger.info("Processing models for link ID: {}", link.getId());
                    linkProcessingService.processModels(link, link.getStar());
                }
                
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.error("Error processing link ID: {} - {}", link.getId(), e.getMessage());
            }
        });
        
        logger.info("Completed background processing for category {}: {} processed, {} successful, {} errors", 
                category, processedCount.get(), successCount.get(), errorCount.get());
    }
} 