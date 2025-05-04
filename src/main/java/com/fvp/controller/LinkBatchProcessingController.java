package com.fvp.controller;

import com.fvp.entity.Link;
import com.fvp.repository.LinkRepository;
import com.fvp.service.CategoryProcessingService;
import com.fvp.service.LinkProcessingService;
import com.fvp.service.ModelProcessingService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links/batch")
public class LinkBatchProcessingController {

  private static final Logger logger = LoggerFactory.getLogger(LinkBatchProcessingController.class);
  private static final int BATCH_SIZE = 100;

  @Autowired
  private LinkRepository linkRepository;

  @Autowired
  private LinkProcessingService linkProcessingService;

  @Autowired
  CategoryProcessingService categoryProcessingService;

  @Autowired
  ModelProcessingService modelProcessingService;

  @GetMapping("/process-all-categories-models")
  @Async
  public ResponseEntity<Map<String, Object>> processAllCategoriesAndModels() {
    logger.info("Starting background processing of all links for categories and models");

    // Start the background processing
    processAllCategoriesAndModelsAsync();

    // Return immediate response
    Map<String, Object> response = new HashMap<>();
    response.put("status", "started");
    response.put("message",
        "Background processing of all links for categories and models has been started");

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
      Page<Link> linkPage = linkRepository.findAll(PageRequest.of(page, BATCH_SIZE));
      for (Link link : linkPage.getContent()) {
        try {
          logger.info("Processing category for page {} of {}", page + 1, totalPages);
          categoryProcessingService.processCategories(link, link.getCategory());
          logger.info("Processing page1 for page {} of {}", page + 1, totalPages);
          modelProcessingService.processModels(link, link.getStar());
        } catch (Exception e) {
          logger.error("Error processing Category for link ID: {} - {}", link.getId(),
              e.getMessage());
        }
      }
    }

    for (int page = 0; page < totalPages; page++) {
      logger.info("Processing page {} of {}", page + 1, totalPages);

      Page<Link> linkPage = linkRepository.findAll(PageRequest.of(page, BATCH_SIZE));
      linkPage.getContent().forEach(link -> {
        try {
          totalProcessed.incrementAndGet();
          linkProcessingService.processLink(link);
          totalSuccess.incrementAndGet();
        } catch (Exception e) {
          totalError.incrementAndGet();
          logger.error("Error processing link ID: {} - {}", link.getId(), e.getMessage());
        }
      });

      logger.info("Completed page {}: {} processed, {} successful, {} errors",
          page + 1, totalProcessed.get(), totalSuccess.get(), totalError.get());
    }

    logger.info(
        "Completed background processing of all links: {} processed, {} successful, {} errors",
        totalProcessed.get(), totalSuccess.get(), totalError.get());
  }

} 