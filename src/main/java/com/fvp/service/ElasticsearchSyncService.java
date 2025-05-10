package com.fvp.service;

import com.fvp.config.ElasticsearchSyncConfig;
import com.fvp.document.CategoryDocument;
import com.fvp.document.ModelDocument;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.Model;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.LinkModelRepository;
import com.fvp.repository.LinkRepository;
import com.fvp.repository.ModelRepository;
import com.fvp.util.LoggingUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElasticsearchSyncService {

  private static final Logger logger = LoggingUtil.getLogger(ElasticsearchSyncService.class);

  private final LinkRepository linkRepository;
  private final LinkModelRepository linkModelRepository;
  private final LinkProcessingService linkProcessingService;
  private final ElasticsearchClientService elasticsearchClientService;
  private final ElasticsearchSyncConfig syncConfig;
  private final ExecutorService executorService;
  private final JdbcTemplate jdbcTemplate;
  private final LinkCategoryShardingService shardingService;
  private final AllCatRepository allCatRepository;
  private final ModelRepository modelRepository;
  // Track sync status
  private final AtomicReference<String> linkSyncStatus = new AtomicReference<>("not_started");
  private final AtomicReference<String> categorySyncStatus = new AtomicReference<>("not_started");
  private final AtomicReference<String> modelSyncStatus = new AtomicReference<>("not_started");
  @Autowired
  public ElasticsearchSyncService(
      LinkRepository linkRepository,
      LinkModelRepository linkModelRepository,
      LinkProcessingService linkProcessingService,
      ElasticsearchClientService elasticsearchClientService,
      ElasticsearchSyncConfig syncConfig,
      JdbcTemplate jdbcTemplate,
      LinkCategoryShardingService shardingService,
      AllCatRepository allCatRepository,
      ModelRepository modelRepository) {
    this.linkRepository = linkRepository;
    this.linkModelRepository = linkModelRepository;
    this.linkProcessingService = linkProcessingService;
    this.elasticsearchClientService = elasticsearchClientService;
    this.syncConfig = syncConfig;
    this.executorService = Executors.newFixedThreadPool(syncConfig.getThreadPoolSize());
    this.jdbcTemplate = jdbcTemplate;
    this.shardingService = shardingService;
    this.allCatRepository = allCatRepository;
    this.modelRepository = modelRepository;
  }

  /**
   * Start a full sync of all links from MySQL to Elasticsearch
   *
   * @param startIndex Optional start index for pagination. If null, starts from beginning.
   * @return CompletableFuture with the result of the sync operation
   */
  @Async
  public CompletableFuture<String> syncAllLinksToElasticsearch(Integer startIndex) {
    linkSyncStatus.set("in_progress");
    return CompletableFuture.supplyAsync(() -> {
      if (!syncConfig.isEnabled()) {
        linkSyncStatus.set("disabled");
        return "Elasticsearch sync is disabled";
      }

      logger.info("Starting sync of links to Elasticsearch" + (startIndex != null ? " from index " + startIndex : ""));
      long startTime = System.currentTimeMillis();

      try {
        // Get total count of links
        long totalLinks = linkRepository.count();
        logger.info("Found {} links to sync", totalLinks);

        if (totalLinks == 0) {
          return "No links found to sync";
        }

        // Use a smaller batch size for memory efficiency
        int batchSize = 1000; // Reduced from default to prevent OOM
        int processedCount = 0;
        long lastLogTime = System.currentTimeMillis();

        // Calculate starting offset
        int offset = startIndex != null ? startIndex : 0;
        if (offset >= totalLinks) {
          return "Start index " + offset + " is greater than total links " + totalLinks;
        }

        for (; offset < totalLinks; offset += batchSize) {
          // Clear any existing references to help GC
          System.gc();

          // Process batch
          List<Link> links = linkRepository.findAllWithPagination(offset, batchSize);
          processLinksBatch(links);
          processedCount += links.size();

          // Clear references after processing
          links.clear();

          // Log progress every 30 seconds or every 10,000 records
          long currentTime = System.currentTimeMillis();
          if (currentTime - lastLogTime > 30000 || processedCount % 10000 == 0) {
            logger.info("Processed {}/{} links ({}%) - Memory: {}MB",
                processedCount,
                totalLinks - (startIndex != null ? startIndex : 0),
                Math.round((processedCount * 100.0) / (totalLinks - (startIndex != null ? startIndex : 0))),
                Runtime.getRuntime().totalMemory() / (1024 * 1024));
            lastLogTime = currentTime;
          }
        }

        long duration = System.currentTimeMillis() - startTime;
        String result = String.format("Sync completed. Processed %d links in %d seconds",
            processedCount, duration / 1000);
        logger.info(result);
        linkSyncStatus.set("completed");
        return result;

      } catch (Exception e) {
        logger.error("Error during Elasticsearch sync: {}", e.getMessage(), e);
        linkSyncStatus.set("error");
        return "Error during sync: " + e.getMessage();
      }
    }, executorService);
  }

  /**
   * Start a full sync of all links from MySQL to Elasticsearch
   *
   * @return CompletableFuture with the result of the sync operation
   */
  @Async
  public CompletableFuture<String> syncAllLinksToElasticsearch() {
    return syncAllLinksToElasticsearch(null);
  }

  /**
   * Process a batch of links and sync them to Elasticsearch
   *
   * @param links List of links to process
   */
  @Transactional(readOnly = true)
  private void processLinksBatch(List<Link> links) {
    for (Link link : links) {
      try {
        linkProcessingService.updateElasticsearchDocument(link);
      } catch (Exception e) {
        logger.error("Error creating document for link ID {}: {}", link.getId(), e.getMessage(), e);
      }
    }
  }


  /**
   * Start a full sync of all categories from MySQL to Elasticsearch
   *
   * @return CompletableFuture with the result of the sync operation
   */
  @Async
  public CompletableFuture<String> syncAllCategoriesToElasticsearch() {
    categorySyncStatus.set("in_progress");
    return CompletableFuture.supplyAsync(() -> {
      if (!syncConfig.isEnabled()) {
        categorySyncStatus.set("disabled");
        return "Elasticsearch sync is disabled";
      }

      logger.info("Starting full sync of categories to Elasticsearch");
      long startTime = System.currentTimeMillis();

      try {
        int processedCount = 0;

        // Get all distinct tenant IDs
        List<Integer> tenantIds = allCatRepository.findAllDistinctTenantIds();

        // Process each tenant
        for (Integer tenantId : tenantIds) {
          try {
            // Get all categories for this tenant
            List<AllCat> categories = allCatRepository.findByTenantId(tenantId);

            for (AllCat category : categories) {
              try {
                if (category.getName() == null || category.getName().isEmpty()
                    || true == category.getCreatedViaLink()) {
                  continue;
                }

                CategoryDocument doc = new CategoryDocument();
                doc.setId(sanitizeForDocumentId(category.getName()));
                doc.setTenantId(category.getTenantId());
                doc.setName(category.getName());
                doc.setDescription(category.getDescription());
                doc.setHomeThumb(category.getHomeThumb());
                doc.setHeader(category.getHeader());
                doc.setHomeSEO(category.getHomeSEO());
                doc.setHomeCatOrder(category.getHomeCatOrder());
                doc.setHome(category.getHome());
                doc.setCreatedViaLink(category.getCreatedViaLink());
                doc.setCreatedAt(java.util.Date.from(
                    category.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));

                // Get link count for this category
                Long count = shardingService.getRepositoryForCategory(category.getName())
                    .countByTenantIdAndCategory(tenantId, category.getName());
                doc.setLinkCount(count);

                elasticsearchClientService.saveCategoryDocument(doc);
                processedCount++;

                if (processedCount % 100 == 0) {
                  logger.info("Processed {} categories", processedCount);
                }
              } catch (Exception e) {
                logger.error("Error processing category '{}' for tenant {}: {}",
                    category.getName(), tenantId, e.getMessage(), e);
              }
            }
          } catch (Exception e) {
            logger.error("Error processing tenant {}: {}", tenantId, e.getMessage(), e);
          }
        }

        long duration = System.currentTimeMillis() - startTime;
        String result = String.format(
            "Category sync completed. Processed %d categories in %d seconds",
            processedCount, duration / 1000);
        logger.info(result);
        categorySyncStatus.set("completed");
        return result;

      } catch (Exception e) {
        logger.error("Error during category Elasticsearch sync: {}", e.getMessage(), e);
        categorySyncStatus.set("error");
        return "Error during category sync: " + e.getMessage();
      }
    }, executorService);
  }

  /**
   * Sanitize a string for use in document IDs by removing or replacing illegal characters
   *
   * @param input The string to sanitize
   * @return Sanitized string
   */
  private String sanitizeForDocumentId(String input) {
    if (input == null) {
      return "";
    }
    // Remove backslashes and other special characters, replace spaces with underscores
    return input.replaceAll("[\\\\\"]", "").replaceAll("[^a-zA-Z0-9_]", "_");
  }

  /**
   * Clean model name by removing special characters and normalizing
   *
   * @param modelName The model name to clean
   * @return Cleaned model name
   */
  private String cleanModelName(String modelName) {
    if (modelName == null) {
      return "";
    }
    // Remove backslashes, quotes, and other special characters
    return modelName.replaceAll("[\\\\\"]", "").trim();
  }

  @Async
  public CompletableFuture<String> syncAllModelsToElasticsearch() {
    modelSyncStatus.set("in_progress");
    return CompletableFuture.supplyAsync(() -> {
      if (!syncConfig.isEnabled()) {
        modelSyncStatus.set("disabled");
        return "Elasticsearch sync is disabled";
      }

      logger.info("Starting full sync of models to Elasticsearch");
      long startTime = System.currentTimeMillis();

      try {
        // Get all models from the model table
        List<Model> models = modelRepository.findAll();
        logger.info("Found {} models to sync", models.size());

        // Use a map to track processed models and avoid duplicates
        Map<String, ModelDocument> processedModels = new HashMap<>();
        final int[] processedCount = {0};

        for (Model model : models) {
          if (model.getName() == null || model.getName().isEmpty() || model.getDataPresent() == 0) {
            continue;
          }

          try {
            // Clean the model name
            String cleanedModel = cleanModelName(model.getName());
            if (cleanedModel.isEmpty()) {
              continue;
            }

            // Create a unique key for the model
            String modelKey = model.getTenantId() + "_" + cleanedModel;

            // Skip if we've already processed this model
            if (processedModels.containsKey(modelKey)) {
              continue;
            }

            ModelDocument doc = new ModelDocument();
            doc.setId(model.getTenantId() + "_" + sanitizeForDocumentId(cleanedModel));
            doc.setTenantId(model.getTenantId());
            doc.setName(cleanedModel);
            doc.setDescription(model.getDescription());
            doc.setCountry(model.getCountry());
            doc.setThumbnail(model.getThumbnail());
            doc.setThumbPath(model.getThumbpath());
            doc.setAge(model.getAge());
            doc.setLinkCount(model.getDataPresent());
            doc.setCreatedAt(java.util.Date.from(
                model.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));

            // Save the document and track it
            elasticsearchClientService.saveModelDocument(doc);
            processedModels.put(modelKey, doc);

            processedCount[0]++;

            if (processedCount[0] % 100 == 0) {
              logger.info("Processed {} models", processedCount[0]);
            }
          } catch (Exception e) {
            logger.error("Error processing model '{}' for tenant {}: {}",
                model.getName(), model.getTenantId(), e.getMessage(), e);
          }
        }

        long duration = System.currentTimeMillis() - startTime;
        String result = String.format("Model sync completed. Processed %d models in %d seconds",
            processedCount[0], duration / 1000);
        logger.info(result);
        modelSyncStatus.set("completed");
        return result;

      } catch (Exception e) {
        logger.error("Error during model Elasticsearch sync: {}", e.getMessage(), e);
        modelSyncStatus.set("error");
        return "Error during model sync: " + e.getMessage();
      }
    }, executorService);
  }

  public String getLinkSyncStatus() {
    return linkSyncStatus.get();
  }

  public String getCategorySyncStatus() {
    return categorySyncStatus.get();
  }

  public String getModelSyncStatus() {
    return modelSyncStatus.get();
  }
} 