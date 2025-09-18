package com.fvp.service;

import com.fvp.entity.AllCat;
import com.fvp.repository.AllCatRepository;
import com.fvp.util.LoggingUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class CategoryMappingService {

  private static final Logger logger = LoggingUtil.getLogger(CategoryMappingService.class);

  private final AllCatRepository allCatRepository;
  private final Map<String, String> categoryNameMapping = new HashMap<>();

  public CategoryMappingService(AllCatRepository allCatRepository) {
    this.allCatRepository = allCatRepository;
  }

  @PostConstruct
  public void initializeCategoryMapping() {
    logger.info("=== Starting category mapping initialization ===");
    long startTime = System.currentTimeMillis();
    
    try {
      // Fetch all categories from database
      List<AllCat> allCategories = allCatRepository.findAll();
      logger.info("Fetched {} categories from database for mapping initialization", allCategories.size());

      int mappedCount = 0;
      int duplicateCount = 0;

      for (AllCat category : allCategories) {
        if (category.getName() != null && !category.getName().trim().isEmpty()) {
          String actualName = category.getName();
          String lowerCaseName = actualName.toLowerCase();
          
          if (categoryNameMapping.containsKey(lowerCaseName)) {
            logger.warn("Duplicate category name found (case-insensitive): '{}' -> '{}' (existing: '{}')", 
                lowerCaseName, actualName, categoryNameMapping.get(lowerCaseName));
            duplicateCount++;
          } else {
            categoryNameMapping.put(lowerCaseName, actualName);
            mappedCount++;
            logger.debug("Mapped category: '{}' -> '{}'", lowerCaseName, actualName);
          }
        } else {
          logger.warn("Skipping category with null or empty name - ID: {}", category.getId());
        }
      }

      long duration = System.currentTimeMillis() - startTime;
      logger.info("=== Category mapping initialization completed in {} ms ===", duration);
      logger.info("Successfully mapped {} categories, {} duplicates found", mappedCount, duplicateCount);
      logger.info("Category mapping cache size: {}", categoryNameMapping.size());

    } catch (Exception e) {
      logger.error("Error during category mapping initialization: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to initialize category mapping", e);
    }
  }

  /**
   * Maps an incoming category name (case-insensitive) to the actual database category name
   * 
   * @param incomingCategoryName The category name from the frontend (case-insensitive)
   * @return The actual category name from the database, or null if not found
   */
  public String mapToActualCategoryName(String incomingCategoryName) {
    if (incomingCategoryName == null || incomingCategoryName.trim().isEmpty()) {
      logger.warn("Incoming category name is null or empty");
      return null;
    }

    String trimmedName = incomingCategoryName.trim();
    String lowerCaseName = trimmedName.toLowerCase();
    
    String actualName = categoryNameMapping.get(lowerCaseName);
    
    if (actualName != null) {
      logger.info("Category mapping found - Input: '{}' -> Actual: '{}'", trimmedName, actualName);
      return actualName;
    } else {
      logger.warn("No category mapping found for input: '{}' (lowercase: '{}')", trimmedName, lowerCaseName);
      return null;
    }
  }

  /**
   * Maps an incoming category name to actual category name, with fallback to original if not found
   * 
   * @param incomingCategoryName The category name from the frontend
   * @return The actual category name if found, otherwise the original input
   */
  public String mapToActualCategoryNameWithFallback(String incomingCategoryName) {
    String actualName = mapToActualCategoryName(incomingCategoryName);
    return actualName != null ? actualName : incomingCategoryName;
  }

  /**
   * Checks if a category name exists in the mapping (case-insensitive)
   * 
   * @param categoryName The category name to check
   * @return true if the category exists, false otherwise
   */
  public boolean categoryExists(String categoryName) {
    if (categoryName == null || categoryName.trim().isEmpty()) {
      return false;
    }
    
    String lowerCaseName = categoryName.trim().toLowerCase();
    boolean exists = categoryNameMapping.containsKey(lowerCaseName);
    logger.debug("Category existence check - Input: '{}' -> Exists: {}", categoryName, exists);
    return exists;
  }

  /**
   * Gets the current size of the category mapping cache
   * 
   * @return The number of categories in the mapping cache
   */
  public int getMappingCacheSize() {
    return categoryNameMapping.size();
  }

  /**
   * Refreshes the category mapping cache by re-fetching from database
   * This can be called if categories are added/updated at runtime
   */
  public void refreshCategoryMapping() {
    logger.info("Refreshing category mapping cache");
    categoryNameMapping.clear();
    initializeCategoryMapping();
  }
}
