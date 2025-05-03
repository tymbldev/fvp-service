package com.fvp.service;

import com.fvp.entity.BaseLinkCategory;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.LinkCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class LinkCategoryMigrationService {
    private static final Logger logger = LoggerFactory.getLogger(LinkCategoryMigrationService.class);
    private static final int BATCH_SIZE = 1000;
    
    @Value("${migration.category.chunkSize:100}")
    private int categoryChunkSize;
    
    @Autowired
    private LinkCategoryRepository linkCategoryRepository;
    
    @Autowired
    private LinkCategoryShardingService shardingService;
    
    /**
     * Migrates all data from the original LinkCategory table to the sharded tables
     * @return the number of records migrated
     */

    public int migrateAllData() {
        AtomicInteger totalMigrated = new AtomicInteger(0);
        int page = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            logger.info("Processing batch {} (size: {})", page, BATCH_SIZE);
            Page<LinkCategory> batch = linkCategoryRepository.findAll(PageRequest.of(page, BATCH_SIZE));
            
            if (batch.hasContent()) {
                int migrated = processBatch(batch.getContent());
                totalMigrated.addAndGet(migrated);
                page++;
            } else {
                hasMore = false;
            }
            
            if (!batch.hasNext()) {
                hasMore = false;
            }
        }
        
        logger.info("Migration completed. Total records migrated: {}", totalMigrated.get());
        return totalMigrated.get();
    }
    
    /**
     * Processes a batch of LinkCategory entities
     * @param batch the batch of entities to process
     * @return the number of records migrated
     */

    public int processBatch(List<LinkCategory> batch) {
        int count = 0;
        List<Integer> migratedIds = new ArrayList<>();
        
        for (LinkCategory linkCategory : batch) {
            try {
                BaseLinkCategory shardEntity = shardingService.convertToShardEntity(linkCategory);
                shardingService.save(shardEntity);
                migratedIds.add(linkCategory.getId());
                count++;
            } catch (Exception e) {
                logger.error("Error migrating LinkCategory with ID: " + linkCategory.getId(), e);
            }
        }
        
        // Delete successfully migrated records from original table
        if (!migratedIds.isEmpty()) {
            try {
                int deletedCount = linkCategoryRepository.deleteByIdIn(migratedIds);
                logger.info("Deleted {} records from original LinkCategory table", deletedCount);
            } catch (Exception e) {
                logger.error("Error deleting migrated records from original table", e);
                throw e; // Re-throw to ensure transaction rollback
            }
        }
        
        logger.info("Migrated {} records in batch", count);
        return count;
    }
    
    /**
     * Migrates LinkCategory entities for a specific tenant
     * @param tenantId the tenant ID to migrate
     * @return the number of records migrated
     */
    @Transactional
    public int migrateByTenant(Integer tenantId) {
        AtomicInteger totalMigrated = new AtomicInteger(0);
        List<LinkCategory> categories = linkCategoryRepository.findByTenantId(tenantId);
        
        categories.forEach(linkCategory -> {
            try {
                BaseLinkCategory shardEntity = shardingService.convertToShardEntity(linkCategory);
                shardingService.save(shardEntity);
                totalMigrated.incrementAndGet();
            } catch (Exception e) {
                logger.error("Error migrating LinkCategory with ID: " + linkCategory.getId(), e);
            }
        });
        
        logger.info("Migration completed for tenant {}. Total records migrated: {}", 
                 tenantId, totalMigrated.get());
        return totalMigrated.get();
    }
    
    /**
     * Migrates LinkCategory entities by category name in chunks
     * @param categoryName the category name to migrate
     * @return the number of records migrated and details about where they were migrated
     */
    @Transactional
    public Map<String, Object> migrateByCategory(String categoryName) {
        return migrateByCategory(categoryName, categoryChunkSize);
    }
    
    /**
     * Migrates LinkCategory entities by category name in chunks
     * @param categoryName the category name to migrate
     * @param chunkSize the size of chunks to process at a time
     * @return the number of records migrated and details about where they were migrated
     */
    @Transactional
    public Map<String, Object> migrateByCategory(String categoryName, int chunkSize) {
        AtomicInteger totalMigrated = new AtomicInteger(0);
        int shardNumber = shardingService.getShardNumber(categoryName);
        Map<String, Object> result = new ConcurrentHashMap<>();
        
        logger.info("Starting migration for category '{}' to shard {} with chunk size {}", 
                  categoryName, shardNumber, chunkSize);
        
        // Count total records for this category
        long totalRecords = linkCategoryRepository.findByCategoryAndTenantIdIsNotNull(categoryName).size();
        
        if (totalRecords == 0) {
            logger.info("No records found for category '{}'", categoryName);
            result.put("categoryName", categoryName);
            result.put("recordsMigrated", 0);
            result.put("shardNumber", shardNumber);
            result.put("status", "success");
            result.put("message", "No records found for this category");
            return result;
        }
        
        // Calculate number of chunks needed
        int totalChunks = (int) Math.ceil((double) totalRecords / chunkSize);
        
        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            int offset = chunkIndex * chunkSize;
            
            // Get records for this chunk using native query with LIMIT and OFFSET
            @SuppressWarnings("unchecked")
            List<LinkCategory> chunk = linkCategoryRepository.findCategoryChunk(categoryName, offset, chunkSize);
            
            logger.info("Processing chunk {}/{} for category '{}' (size: {})", 
                      chunkIndex + 1, totalChunks, categoryName, chunk.size());
            
            // Process the chunk
            for (LinkCategory linkCategory : chunk) {
                try {
                    BaseLinkCategory shardEntity = shardingService.convertToShardEntity(linkCategory);
                    shardingService.save(shardEntity);
                    totalMigrated.incrementAndGet();
                } catch (Exception e) {
                    logger.error("Error migrating LinkCategory with ID: " + linkCategory.getId(), e);
                }
            }
            
            logger.info("Completed chunk {}/{} for category '{}' - migrated {} records", 
                      chunkIndex + 1, totalChunks, categoryName, chunk.size());
        }
        
        int count = totalMigrated.get();
        logger.info("Migration completed for category '{}'. Total records migrated: {} to shard {}", 
                  categoryName, count, shardNumber);
        
        result.put("categoryName", categoryName);
        result.put("recordsMigrated", count);
        result.put("totalRecords", totalRecords);
        result.put("shardNumber", shardNumber);
        result.put("chunksProcessed", totalChunks);
        result.put("chunkSize", chunkSize);
        result.put("status", "success");
        
        return result;
    }
    
    /**
     * Gets all distinct categories and their target shards, along with record counts
     * @return a list of maps containing category info and target shard
     */
    public List<Map<String, Object>> getAllCategoriesWithShardInfo() {
        logger.info("Fetching all distinct categories and their shard information");
        
        // Get all distinct categories across all tenants
        @SuppressWarnings("unchecked")
        List<String> allCategories = linkCategoryRepository.findAll().stream()
            .map(LinkCategory::getCategory)
            .distinct()
            .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (String category : allCategories) {
            Map<String, Object> categoryInfo = new HashMap<>();
            int shardNumber = shardingService.getShardNumber(category);
            long recordCount = linkCategoryRepository.findByCategoryAndTenantIdIsNotNull(category).size();
            
            categoryInfo.put("categoryName", category);
            categoryInfo.put("targetShard", shardNumber);
            categoryInfo.put("recordCount", recordCount);
            
            result.add(categoryInfo);
        }
        
        logger.info("Found {} distinct categories", result.size());
        return result;
    }
    
    /**
     * Get the current chunk size for category migration
     * @return the chunk size
     */
    public int getCategoryChunkSize() {
        return categoryChunkSize;
    }
    
    /**
     * Set the chunk size for category migration
     * @param chunkSize the new chunk size
     */
    public void setCategoryChunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
        this.categoryChunkSize = chunkSize;
        logger.info("Category migration chunk size set to {}", chunkSize);
    }
} 