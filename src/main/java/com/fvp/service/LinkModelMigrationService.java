package com.fvp.service;

import com.fvp.entity.BaseLinkModel;
import com.fvp.entity.LinkModel;
import com.fvp.repository.LinkModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class LinkModelMigrationService {
    private static final Logger logger = LoggerFactory.getLogger(LinkModelMigrationService.class);
    private static final int BATCH_SIZE = 1000;
    
    @Value("${migration.model.chunkSize:100}")
    private int modelChunkSize;
    
    @Autowired
    private LinkModelRepository linkModelRepository;
    
    @Autowired
    private LinkModelShardingService shardingService;
    
    /**
     * Migrates all data from the original LinkModel table to the sharded tables
     * @return the number of records migrated
     */
    @Transactional
    public int migrateAllData() {
        AtomicInteger totalMigrated = new AtomicInteger(0);
        int page = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            logger.info("Processing batch {} (size: {})", page, BATCH_SIZE);
            Page<LinkModel> batch = linkModelRepository.findAll(PageRequest.of(page, BATCH_SIZE));
            
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
     * Processes a batch of LinkModel entities
     * @param batch the batch of entities to process
     * @return the number of records migrated
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processBatch(List<LinkModel> batch) {
        long startTime = System.currentTimeMillis();
        int count = 0;
        List<Integer> migratedIds = new ArrayList<>();
        
        logger.info("Starting batch processing of {} records", batch.size());
        
        // Track time for conversion and saving
        long conversionTime = 0;
        long saveTime = 0;
        int conversionCount = 0;
        int saveCount = 0;
        
        // Convert all entities first
        List<BaseLinkModel> shardEntities = new ArrayList<>();
        for (LinkModel linkModel : batch) {
            try {
                // Track conversion time
                long conversionStart = System.currentTimeMillis();
                BaseLinkModel shardEntity = shardingService.convertToShardEntity(linkModel);
                conversionTime += System.currentTimeMillis() - conversionStart;
                conversionCount++;
                
                shardEntities.add(shardEntity);
                migratedIds.add(linkModel.getId());
                count++;
                
                // Log progress every 100 records
                if (count % 100 == 0) {
                    logger.info("Converted {} records in current batch", count);
                }
            } catch (Exception e) {
                logger.error("Error converting LinkModel with ID: " + linkModel.getId(), e);
            }
        }
        
        // Save all converted entities in batch
        if (!shardEntities.isEmpty()) {
            try {
                long saveStart = System.currentTimeMillis();
                shardingService.saveAll(shardEntities);
                saveTime = System.currentTimeMillis() - saveStart;
                saveCount = shardEntities.size();
                
                logger.info("Saved {} entities to sharded tables in {} ms", 
                    saveCount, saveTime);
            } catch (Exception e) {
                logger.error("Error saving batch to sharded tables", e);
                throw e;
            }
        }
        
        // Delete successfully migrated records from original table
        long deleteStart = System.currentTimeMillis();
        int deletedCount = 0;
        if (!migratedIds.isEmpty()) {
            try {
                deletedCount = linkModelRepository.deleteByIdIn(migratedIds);
                logger.info("Deleted {} records from original LinkModel table in {} ms", 
                    deletedCount, System.currentTimeMillis() - deleteStart);
            } catch (Exception e) {
                logger.error("Error deleting migrated records from original table", e);
                throw e; // Re-throw to ensure transaction rollback
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Batch processing completed in {} ms: {} records processed ({} converted, {} saved, {} deleted). " +
                   "Average times: conversion={} ms, save={} ms", 
                   totalTime, count, conversionCount, saveCount, deletedCount,
                   conversionCount > 0 ? conversionTime/conversionCount : 0,
                   saveCount > 0 ? saveTime/saveCount : 0);
        
        return count;
    }
    
    /**
     * Migrates LinkModel entities for a specific tenant
     * @param tenantId the tenant ID to migrate
     * @return the number of records migrated
     */
    @Transactional
    public int migrateByTenant(Integer tenantId) {
        AtomicInteger totalMigrated = new AtomicInteger(0);
        List<LinkModel> models = linkModelRepository.findByTenantId(tenantId);
        
        models.forEach(linkModel -> {
            try {
                BaseLinkModel shardEntity = shardingService.convertToShardEntity(linkModel);
                shardingService.save(shardEntity);
                totalMigrated.incrementAndGet();
            } catch (Exception e) {
                logger.error("Error migrating LinkModel with ID: " + linkModel.getId(), e);
            }
        });
        
        logger.info("Migration completed for tenant {}. Total records migrated: {}", 
                 tenantId, totalMigrated.get());
        return totalMigrated.get();
    }
    
    /**
     * Migrates LinkModel entities by model name in chunks
     * @param modelName the model name to migrate
     * @return the number of records migrated and details about where they were migrated
     */
    @Transactional
    public Map<String, Object> migrateByModel(String modelName) {
        return migrateByModel(modelName, modelChunkSize);
    }
    
    /**
     * Migrates LinkModel entities by model name in chunks
     * @param modelName the model name to migrate
     * @param chunkSize the size of chunks to process at a time
     * @return the number of records migrated and details about where they were migrated
     */
    @Transactional
    public Map<String, Object> migrateByModel(String modelName, int chunkSize) {
        AtomicInteger totalMigrated = new AtomicInteger(0);
        int shardNumber = shardingService.getShardNumber(modelName);
        Map<String, Object> result = new ConcurrentHashMap<>();
        
        logger.info("Starting migration for model '{}' to shard {} with chunk size {}", 
                  modelName, shardNumber, chunkSize);
        
        // Count total records for this model
        long totalRecords = linkModelRepository.findByModelAndTenantIdIsNotNull(modelName).size();
        
        if (totalRecords == 0) {
            logger.info("No records found for model '{}'", modelName);
            result.put("modelName", modelName);
            result.put("recordsMigrated", 0);
            result.put("shardNumber", shardNumber);
            result.put("status", "success");
            result.put("message", "No records found for this model");
            return result;
        }
        
        // Calculate number of chunks needed
        int totalChunks = (int) Math.ceil((double) totalRecords / chunkSize);
        
        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            int offset = chunkIndex * chunkSize;
            
            // Get records for this chunk using native query with LIMIT and OFFSET
            List<LinkModel> chunk = linkModelRepository.findModelChunk(modelName, offset, chunkSize);
            
            logger.info("Processing chunk {}/{} for model '{}' (size: {})", 
                      chunkIndex + 1, totalChunks, modelName, chunk.size());
            
            // Track time for conversion and saving
            long conversionTime = 0;
            long saveTime = 0;
            int conversionCount = 0;
            int saveCount = 0;
            
            // Convert all entities first
            List<BaseLinkModel> shardEntities = new ArrayList<>();
            List<Integer> migratedIds = new ArrayList<>();
            
            for (LinkModel linkModel : chunk) {
                try {
                    // Track conversion time
                    long conversionStart = System.currentTimeMillis();
                    BaseLinkModel shardEntity = shardingService.convertToShardEntity(linkModel);
                    conversionTime += System.currentTimeMillis() - conversionStart;
                    conversionCount++;
                    
                    shardEntities.add(shardEntity);
                    migratedIds.add(linkModel.getId());
                } catch (Exception e) {
                    logger.error("Error converting LinkModel with ID: " + linkModel.getId(), e);
                }
            }
            
            // Save all converted entities in batch
            if (!shardEntities.isEmpty()) {
                try {
                    long saveStart = System.currentTimeMillis();
                    shardingService.saveAll(shardEntities);
                    saveTime = System.currentTimeMillis() - saveStart;
                    saveCount = shardEntities.size();
                    
                    logger.info("Saved {} entities to sharded tables in {} ms", 
                        saveCount, saveTime);
                } catch (Exception e) {
                    logger.error("Error saving batch to sharded tables", e);
                    throw e;
                }
            }
            
            // Delete successfully migrated records from original table
            long deleteStart = System.currentTimeMillis();
            int deletedCount = 0;
            if (!migratedIds.isEmpty()) {
                try {
                    deletedCount = linkModelRepository.deleteByIdIn(migratedIds);
                    logger.info("Deleted {} records from original LinkModel table in {} ms", 
                        deletedCount, System.currentTimeMillis() - deleteStart);
                } catch (Exception e) {
                    logger.error("Error deleting migrated records from original table", e);
                    throw e; // Re-throw to ensure transaction rollback
                }
            }
            
            totalMigrated.addAndGet(deletedCount);
            
            logger.info("Chunk {}/{} completed: {} records processed ({} converted, {} saved, {} deleted). " +
                       "Average times: conversion={} ms, save={} ms", 
                       chunkIndex + 1, totalChunks, chunk.size(), conversionCount, saveCount, deletedCount,
                       conversionCount > 0 ? conversionTime/conversionCount : 0,
                       saveCount > 0 ? saveTime/saveCount : 0);
        }
        
        int count = totalMigrated.get();
        logger.info("Migration completed for model '{}'. Total records migrated: {} to shard {}", 
                  modelName, count, shardNumber);
        
        result.put("modelName", modelName);
        result.put("recordsMigrated", count);
        result.put("totalRecords", totalRecords);
        result.put("shardNumber", shardNumber);
        result.put("chunksProcessed", totalChunks);
        result.put("chunkSize", chunkSize);
        result.put("status", "success");
        
        return result;
    }
    
    /**
     * Gets all distinct models and their target shards, along with record counts
     * @return a list of maps containing model info and target shard
     */
    public List<Map<String, Object>> getAllModelsWithShardInfo() {
        logger.info("Fetching all distinct models and their shard information");
        
        // Get all distinct models across all tenants
        List<String> allModels = linkModelRepository.findAll().stream()
            .map(LinkModel::getModel)
            .distinct()
            .collect(Collectors.toList());
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (String model : allModels) {
            Map<String, Object> modelInfo = new HashMap<>();
            int shardNumber = shardingService.getShardNumber(model);
            long recordCount = linkModelRepository.findByModelAndTenantIdIsNotNull(model).size();
            
            modelInfo.put("modelName", model);
            modelInfo.put("targetShard", shardNumber);
            modelInfo.put("recordCount", recordCount);
            
            result.add(modelInfo);
        }
        
        logger.info("Found {} distinct models", result.size());
        return result;
    }
    
    /**
     * Get the current chunk size for model migration
     * @return the chunk size
     */
    public int getModelChunkSize() {
        return modelChunkSize;
    }
    
    /**
     * Set the chunk size for model migration
     * @param chunkSize the new chunk size
     */
    public void setModelChunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
        this.modelChunkSize = chunkSize;
        logger.info("Model migration chunk size set to {}", chunkSize);
    }
} 