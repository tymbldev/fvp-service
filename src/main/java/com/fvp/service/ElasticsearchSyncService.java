package com.fvp.service;

import com.fvp.config.ElasticsearchSyncConfig;
import com.fvp.document.LinkDocument;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.LinkRepository;
import com.fvp.util.LoggingUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ElasticsearchSyncService {

    private static final Logger logger = LoggingUtil.getLogger(ElasticsearchSyncService.class);
    
    private final LinkRepository linkRepository;
    private final LinkCategoryRepository linkCategoryRepository;
    private final ElasticsearchClientService elasticsearchClientService;
    private final ElasticsearchSyncConfig syncConfig;
    private final ExecutorService executorService;
    
    @Autowired
    public ElasticsearchSyncService(
            LinkRepository linkRepository,
            LinkCategoryRepository linkCategoryRepository,
            ElasticsearchClientService elasticsearchClientService,
            ElasticsearchSyncConfig syncConfig) {
        this.linkRepository = linkRepository;
        this.linkCategoryRepository = linkCategoryRepository;
        this.elasticsearchClientService = elasticsearchClientService;
        this.syncConfig = syncConfig;
        this.executorService = Executors.newFixedThreadPool(syncConfig.getThreadPoolSize());
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
                
                // Process in batches
                int batchSize = syncConfig.getBatchSize();
                int processedCount = 0;
                
                for (int offset = 0; offset < totalLinks; offset += batchSize) {
                    List<Link> links = linkRepository.findAllWithPagination(offset, batchSize);
                    processLinksBatch(links);
                    processedCount += links.size();
                    
                    logger.info("Processed {}/{} links ({}%)", 
                            processedCount, totalLinks, 
                            Math.round((processedCount * 100.0) / totalLinks));
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
        for (Link link : links) {
            try {
                LinkDocument doc = createLinkDocument(link);
                elasticsearchClientService.saveLinkDocument(doc);
            } catch (Exception e) {
                logger.error("Error processing link ID {}: {}", link.getId(), e.getMessage(), e);
            }
        }
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
} 