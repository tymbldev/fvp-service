package com.fvp.service;

import com.fvp.config.ElasticsearchSyncConfig;
import com.fvp.document.CategoryDocument;
import com.fvp.document.LinkDocument;
import com.fvp.document.ModelDocument;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.LinkModelRepository;
import com.fvp.repository.LinkRepository;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
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
    private final LinkCategoryRepository linkCategoryRepository;
    private final LinkModelRepository linkModelRepository;
    private final ElasticsearchClientService elasticsearchClientService;
    private final ElasticsearchSyncConfig syncConfig;
    private final ExecutorService executorService;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public ElasticsearchSyncService(
            LinkRepository linkRepository,
            LinkCategoryRepository linkCategoryRepository,
            LinkModelRepository linkModelRepository,
            ElasticsearchClientService elasticsearchClientService,
            ElasticsearchSyncConfig syncConfig,
            JdbcTemplate jdbcTemplate) {
        this.linkRepository = linkRepository;
        this.linkCategoryRepository = linkCategoryRepository;
        this.linkModelRepository = linkModelRepository;
        this.elasticsearchClientService = elasticsearchClientService;
        this.syncConfig = syncConfig;
        this.executorService = Executors.newFixedThreadPool(syncConfig.getThreadPoolSize());
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Start a full sync of all links from MySQL to Elasticsearch
     * @return CompletableFuture with the result of the sync operation
     */
    @Async
    public CompletableFuture<String> syncAllLinksToElasticsearch() {
        return CompletableFuture.supplyAsync(() -> {
            if (!syncConfig.isEnabled()) {
                logger.info("Elasticsearch sync is disabled. Skipping sync operation.");
                return "Elasticsearch sync is disabled";
            }
            
            logger.info("Starting full sync of links to Elasticsearch");
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
                
                for (int offset = 0; offset < totalLinks; offset += batchSize) {
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
                            totalLinks, 
                            Math.round((processedCount * 100.0) / totalLinks),
                            Runtime.getRuntime().totalMemory() / (1024 * 1024));
                        lastLogTime = currentTime;
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                String result = String.format("Sync completed. Processed %d links in %d seconds", 
                        processedCount, duration / 1000);
                logger.info(result);
                return result;
                
            } catch (Exception e) {
                logger.error("Error during Elasticsearch sync: {}", e.getMessage(), e);
                return "Error during sync: " + e.getMessage();
            }
        }, executorService);
    }
    
    /**
     * Process a batch of links and sync them to Elasticsearch
     * @param links List of links to process
     */
    @Transactional(readOnly = true)
    private void processLinksBatch(List<Link> links) {
        List<LinkDocument> documents = new ArrayList<>(links.size());
        
        // First, create all documents
        for (Link link : links) {
            try {
                LinkDocument doc = createLinkDocument(link);
                documents.add(doc);
            } catch (Exception e) {
                logger.error("Error creating document for link ID {}: {}", link.getId(), e.getMessage(), e);
            }
        }
        
        // Then, save all documents in a single batch
        for (LinkDocument doc : documents) {
            try {
                elasticsearchClientService.saveLinkDocument(doc);
            } catch (Exception e) {
                logger.error("Error saving document for link ID {}: {}", doc.getId(), e.getMessage(), e);
            }
        }
        
        // Clear references
        documents.clear();
    }
    
    /**
     * Create an Elasticsearch document from a Link entity
     * @param link Link entity
     * @return LinkDocument for Elasticsearch
     */
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
        
        // Get categories for the link
        List<LinkCategory> linkCategories = linkCategoryRepository.findByLinkId(link.getId());
        List<String> categories = linkCategories.stream()
                .map(LinkCategory::getCategory)
                .collect(Collectors.toList());
        doc.setCategories(categories);
        
        return doc;
    }
    
    /**
     * Generate searchable text for a link
     * @param link Link entity
     * @return Searchable text string
     */
    private String generateSearchableText(Link link) {
        StringBuilder searchableText = new StringBuilder();
        searchableText.append(link.getTitle());
        
        if (link.getSheetName() != null && !link.getSheetName().isEmpty()) {
            searchableText.append(" ").append(link.getSheetName());
        }
        
        // Add categories to searchable text
        List<LinkCategory> linkCategories = linkCategoryRepository.findByLinkId(link.getId());
        for (LinkCategory linkCategory : linkCategories) {
            searchableText.append(" ").append(linkCategory.getCategory());
        }
        
        return searchableText.toString().trim();
    }
    
    /**
     * Start a full sync of all categories from MySQL to Elasticsearch
     * @return CompletableFuture with the result of the sync operation
     */
    @Async
    public CompletableFuture<String> syncAllCategoriesToElasticsearch() {
        return CompletableFuture.supplyAsync(() -> {
            if (!syncConfig.isEnabled()) {
                logger.info("Elasticsearch sync is disabled. Skipping category sync operation.");
                return "Elasticsearch sync is disabled";
            }
            
            logger.info("Starting full sync of categories to Elasticsearch");
            long startTime = System.currentTimeMillis();
            
            try {
                // Get distinct categories across all tenants
                List<String> distinctCategories = new ArrayList<>();
                
                // This query finds all distinct categories from the link_category table
                List<Object[]> categoryResults = jdbcTemplate.query(
                    "SELECT DISTINCT category, tenant_id FROM link_category ORDER BY category",
                    (rs, rowNum) -> new Object[] {
                        rs.getString("category"), 
                        rs.getInt("tenant_id")
                    }
                );
                
                int processedCount = 0;
                
                for (Object[] result : categoryResults) {
                    String category = (String) result[0];
                    Integer tenantId = (Integer) result[1];
                    
                    if (category == null || category.isEmpty()) {
                        continue;
                    }
                    
                    try {
                        // Get category count for statistics
                        Long count = linkCategoryRepository.countByTenantIdAndCategory(tenantId, category);
                        
                        CategoryDocument doc = new CategoryDocument();
                        doc.setId(tenantId + "_" + category.replace(" ", "_"));
                        doc.setTenantId(tenantId);
                        doc.setName(category);
                        elasticsearchClientService.saveCategoryDocument(doc);
                        
                        processedCount++;
                        
                        if (processedCount % 100 == 0) {
                            logger.info("Processed {} categories", processedCount);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing category '{}' for tenant {}: {}", 
                            category, tenantId, e.getMessage(), e);
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                String result = String.format("Category sync completed. Processed %d categories in %d seconds", 
                        processedCount, duration / 1000);
                logger.info(result);
                return result;
                
            } catch (Exception e) {
                logger.error("Error during category Elasticsearch sync: {}", e.getMessage(), e);
                return "Error during category sync: " + e.getMessage();
            }
        }, executorService);
    }
    
    /**
     * Sanitize a string for use in document IDs by removing or replacing illegal characters
     * @param input The string to sanitize
     * @return Sanitized string
     */
    private String sanitizeForDocumentId(String input) {
        if (input == null) {
            return "";
        }
        // Replace spaces with underscores and remove other special characters
        return input.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    @Async
    public CompletableFuture<String> syncAllModelsToElasticsearch() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int processedCount = 0;
            
            try {
                // Get all distinct models across all tenants
                String sql = "SELECT DISTINCT model, tenant_id FROM link_model WHERE model IS NOT NULL";
                List<Object[]> modelResults = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    return new Object[] { rs.getString("model"), rs.getInt("tenant_id") };
                });
                
                logger.info("Found {} distinct models to sync", modelResults.size());
                
                for (Object[] result : modelResults) {
                    String model = (String) result[0];
                    Integer tenantId = (Integer) result[1];
                    
                    if (model == null || model.isEmpty()) {
                        continue;
                    }
                    
                    try {
                        // Get link count for this model
                        Long count = linkModelRepository.countByTenantIdAndModel(tenantId, model);
                        
                        ModelDocument doc = new ModelDocument();
                        doc.setId(tenantId + "_" + sanitizeForDocumentId(model));
                        doc.setTenantId(tenantId);
                        doc.setName(model);
                        doc.setLinkCount(count.intValue());
                        doc.setCreatedAt(new Date());
                        
                        elasticsearchClientService.saveModelDocument(doc);
                        
                        processedCount++;
                        
                        if (processedCount % 100 == 0) {
                            logger.info("Processed {} models", processedCount);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing model '{}' for tenant {}: {}", 
                            model, tenantId, e.getMessage(), e);
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                String result = String.format("Model sync completed. Processed %d models in %d seconds", 
                        processedCount, duration / 1000);
                logger.info(result);
                return result;
                
            } catch (Exception e) {
                logger.error("Error during model Elasticsearch sync: {}", e.getMessage(), e);
                return "Error during model sync: " + e.getMessage();
            }
        }, executorService);
    }
} 