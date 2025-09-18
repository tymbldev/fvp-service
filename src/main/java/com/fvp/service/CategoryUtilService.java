package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryUtilService {

  private static final Logger logger = LoggingUtil.getLogger(CategoryUtilService.class);
  private static final String CATEGORY_LINKS_CACHE = "categoryLinks";

  private final AllCatService allCatService;
  private final LinkService linkService;
  private final CacheService cacheService;
  private final LinkCategoryService linkCategoryService;
  private final CategoryService categoryService;

  public CategoryUtilService(
      AllCatService allCatService,
      LinkService linkService,
      CacheService cacheService,
      LinkCategoryService linkCategoryService,
      CategoryService categoryService) {
    this.allCatService = allCatService;
    this.linkService = linkService;
    this.cacheService = cacheService;
    this.linkCategoryService = linkCategoryService;
    this.categoryService = categoryService;
  }

  public Page<CategoryWithLinkDTO> getCategoryLinks(Integer tenantId, String categoryName,
      Pageable pageable, Integer minDuration, Integer maxDuration, String quality) {
    long methodStartTime = System.currentTimeMillis();
    logger.info("=== Starting getCategoryLinks method ===");
    logger.info("Request parameters - tenantId: {}, categoryName: '{}', page: {}, size: {}, minDuration: {}, maxDuration: {}, quality: '{}'",
        tenantId, categoryName, pageable.getPageNumber(), pageable.getPageSize(), minDuration,
        maxDuration, quality);
    
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters detected - tenantId: {}, categoryName: '{}' - returning empty page", 
          tenantId, categoryName);
      return Page.empty();
    }

    // Generate cache key based on all parameters
    String cacheKey = String.format("%s_%s_%d_%d_%s_%s_%s",
        tenantId,
        categoryName,
        pageable.getPageNumber(),
        pageable.getPageSize(),
        minDuration != null ? minDuration : "null",
        maxDuration != null ? maxDuration : "null",
        quality != null ? quality : "null"
    );
    logger.info("Generated cache key: '{}'", cacheKey);

    // Try to get from cache first
    long cacheStartTime = System.currentTimeMillis();
    TypeReference<Page<CategoryWithLinkDTO>> typeRef = new TypeReference<Page<CategoryWithLinkDTO>>() {
    };
    Optional<Page<CategoryWithLinkDTO>> cachedPage = cacheService.getCollectionFromCache(
        CATEGORY_LINKS_CACHE,
        cacheKey,
        typeRef
    );
    long cacheDuration = System.currentTimeMillis() - cacheStartTime;

    if (cachedPage.isPresent()) {
      logger.info("Cache HIT for category links - key: '{}', duration: {} ms, cached result size: {}",
          cacheKey, cacheDuration, cachedPage.get().getContent().size());
      return cachedPage.get();
    }

    logger.info("Cache MISS for category links - key: '{}', cache lookup duration: {} ms", 
        cacheKey, cacheDuration);

    // Get the category by name
    logger.info("Fetching category from database - tenantId: {}, categoryName: '{}'", tenantId, categoryName);
    AllCat category = LoggingUtil.logOperationTime(
        logger,
        "fetch category by name",
        () -> allCatService.findByTenantIdAndName(tenantId, categoryName)
    );

    if (category == null) {
      logger.warn("Category NOT FOUND in database - tenantId: {}, categoryName: '{}' - returning empty page", 
          tenantId, categoryName);
      return Page.empty();
    }
    
    logger.info("Category found - ID: {}, Name: '{}', Description: '{}', TenantId: {}", 
        category.getId(), category.getName(), category.getDescription(), category.getTenantId());

    // Get the total count with filters (for pagination metadata)
    logger.info("Counting total links with filters - tenantId: {}, categoryName: '{}', minDuration: {}, maxDuration: {}, quality: '{}'",
        tenantId, categoryName, minDuration, maxDuration, quality);
    Long totalCount = LoggingUtil.logOperationTime(
        logger,
        "count links with filters",
        () -> linkCategoryService.countByCategoryWithFilters(tenantId, categoryName, minDuration, maxDuration, quality)
    );
    logger.info("Total count of links with filters: {} for category '{}'", totalCount, categoryName);

    List<CategoryWithLinkDTO> pageContent = new ArrayList<>();

    // For first page (page 0), use getCategoryFirstLink to get the first result
    if (pageable.getPageNumber() == 0) {
      logger.info("=== Processing FIRST PAGE (page 0) ===");
      logger.info("Using getCategoryFirstLink strategy for tenant {} and category '{}'",
          tenantId, categoryName);
      
      // Get the first link using getCategoryFirstLink method
      logger.info("Calling getCategoryFirstLink - tenantId: {}, categoryName: '{}'", tenantId, categoryName);
      CategoryWithLinkDTO firstLinkDTO = LoggingUtil.logOperationTime(
          logger,
          "get category first link",
          () -> categoryService.getCategoryFirstLink(tenantId, categoryName)
      );
      
      if (firstLinkDTO != null) {
        logger.info("First link SUCCESSFULLY retrieved - Link: '{}', Title: '{}', Thumbnail: '{}' for tenant {} and category '{}'",
            firstLinkDTO.getLink(), firstLinkDTO.getLinkTitle(), firstLinkDTO.getLinkThumbnail(), tenantId, categoryName);
        
        // Get the link entity for more complete information
        Link linkEntity = null;
        try {
          linkEntity = linkService.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId,
              firstLinkDTO.getLink());
          if (linkEntity != null) {
            logger.info("Link entity found for first link - ID: {}, Title: '{}', Duration: {}, Source: '{}'", 
                linkEntity.getId(), linkEntity.getTitle(), linkEntity.getDuration(), linkEntity.getSource());
            
            // Create a complete DTO from the link entity to ensure all fields are populated
            CategoryWithLinkDTO completeFirstLinkDTO = new CategoryWithLinkDTO();
            completeFirstLinkDTO.setId(category.getId());
            completeFirstLinkDTO.setName(category.getName());
            completeFirstLinkDTO.setDescription(category.getDescription());
            completeFirstLinkDTO.setLink(linkEntity.getLink());
            completeFirstLinkDTO.setLinkTitle(linkEntity.getTitle());
            completeFirstLinkDTO.setLinkThumbnail(linkEntity.getThumbnail());
            completeFirstLinkDTO.setLinkThumbPath(linkEntity.getThumbpath());
            completeFirstLinkDTO.setLinkDuration(linkEntity.getDuration());
            completeFirstLinkDTO.setLinkSource(linkEntity.getSource());
            completeFirstLinkDTO.setLinkTrailer(linkEntity.getTrailer());
            completeFirstLinkDTO.setLinkCount(firstLinkDTO.getLinkCount()); // Keep the link count from the original DTO
            
            pageContent.add(completeFirstLinkDTO);
            logger.info("Added complete first link DTO to page content. Current page content size: {}", pageContent.size());
          } else {
            logger.warn("Link entity NOT FOUND for first link - using original DTO for tenantId: {}, link: '{}'", 
                tenantId, firstLinkDTO.getLink());
            pageContent.add(firstLinkDTO);
            logger.info("Added original first link DTO to page content. Current page content size: {}", pageContent.size());
          }
        } catch (Exception e) {
          logger.warn("Error fetching link entity for first link - using original DTO for tenantId: {}, link: '{}', error: {}", 
              tenantId, firstLinkDTO.getLink(), e.getMessage());
          pageContent.add(firstLinkDTO);
          logger.info("Added original first link DTO to page content. Current page content size: {}", pageContent.size());
        }
        
        // If first page has only one item (pageSize=1), we're done
        if (pageable.getPageSize() == 1) {
          logger.info("Page size is 1 - returning only first link. No additional items needed.");
          Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
          logger.info("Caching result with key: '{}'", cacheKey);
          cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1,
              TimeUnit.HOURS);
          long methodDuration = System.currentTimeMillis() - methodStartTime;
          logger.info("=== Method completed in {} ms - returning single first link ===", methodDuration);
          return result;
        }
        
        // For first page, get remaining items (pageSize - 1) and exclude the first link's ID
        // We already have the linkEntity from above, so use it
        Integer firstLinkId = linkEntity != null ? linkEntity.getId() : null;
        int adjustedOffset = 0;
        int limit = pageable.getPageSize() - 1;
        
        logger.info("Fetching {} additional items for first page (excluding first link ID: {})", 
            limit, firstLinkId);
        logger.info("Query parameters - tenantId: {}, categoryName: '{}', minDuration: {}, maxDuration: {}, quality: '{}', firstLinkId: {}, offset: {}, limit: {}",
            tenantId, categoryName, minDuration, maxDuration, quality, firstLinkId, adjustedOffset, limit);
        
        // Get the remaining items for the first page
        List<LinkCategory> dbItems = LoggingUtil.logOperationTime(
            logger,
            "fetch items with pagination excluding first link",
            () -> linkCategoryService.findByCategoryWithFiltersExcludingLinkPageable(
                tenantId,
                categoryName,
                minDuration,
                maxDuration,
                quality,
                firstLinkId,
                adjustedOffset,
                limit
            )
        );
        logger.info("Retrieved {} additional items from database for first page", dbItems.size());
        
        pageContent.addAll(createDTOsFromLinkCategories(dbItems, category));
        
        logger.info("First page COMPLETED - Total items: {} (1 first link + {} additional items) for tenant {} and category '{}'",
            pageContent.size(), dbItems.size(), tenantId, categoryName);
        
        Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
        logger.info("Caching first page result with key: '{}'", cacheKey);
        cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1,
            TimeUnit.HOURS);
        long methodDuration = System.currentTimeMillis() - methodStartTime;
        logger.info("=== Method completed in {} ms - returning first page with {} items ===", methodDuration, pageContent.size());
        return result;
      } else {
        logger.warn("No first link found for tenant {} and category '{}' - falling back to regular pagination",
            tenantId, categoryName);
      }
    }

    // For non-first pages, also exclude the first link to avoid duplication
    logger.info("=== Processing NON-FIRST PAGE (page {}) ===", pageable.getPageNumber());
    int offset = (int) pageable.getOffset();
    int limit = pageable.getPageSize();
    logger.info("Page parameters - offset: {}, limit: {}", offset, limit);
    
    // Get the first link to exclude it from subsequent pages
    CategoryWithLinkDTO firstLinkDTO = null;
    Integer firstLinkId = null;
    if (pageable.getPageNumber() > 0) {
      logger.info("Getting first link to exclude from page {} results for tenant {} and category '{}'",
          pageable.getPageNumber(), tenantId, categoryName);
      
      firstLinkDTO = LoggingUtil.logOperationTime(
          logger,
          "get category first link for exclusion",
          () -> categoryService.getCategoryFirstLink(tenantId, categoryName)
      );
      
      if (firstLinkDTO != null) {
        logger.info("First link retrieved for exclusion - Link: '{}', Title: '{}'", 
            firstLinkDTO.getLink(), firstLinkDTO.getLinkTitle());
        try {
          Link linkEntity = linkService.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId,
              firstLinkDTO.getLink());
          firstLinkId = linkEntity != null ? linkEntity.getId() : null;
          logger.info("Will exclude first link with ID {} from page {} results", firstLinkId, pageable.getPageNumber());
        } catch (Exception e) {
          logger.warn("Error fetching first link entity for exclusion - tenantId: {}, link: '{}', error: {}", 
              tenantId, firstLinkDTO.getLink(), e.getMessage());
        }
      } else {
        logger.warn("No first link found for exclusion on page {} - tenantId: {}, categoryName: '{}'", 
            pageable.getPageNumber(), tenantId, categoryName);
      }
    }
    
    // Adjust limit: if we're excluding the first link, we need to fetch one extra record
    // to maintain the same page size (since we're excluding one, we need one more)
    int adjustedLimit = limit;
    if (firstLinkId != null) {
      adjustedLimit = limit + 1; // Fetch one extra to compensate for exclusion
      logger.info("Adjusted limit from {} to {} to compensate for excluded first link", limit, adjustedLimit);
    } else {
      logger.info("No first link to exclude - using original limit: {}", limit);
    }
    
    final Integer finalFirstLinkId = firstLinkId;
    final int finalOffset = offset;
    final int finalLimit = adjustedLimit;
    
    logger.info("Final query parameters - tenantId: {}, categoryName: '{}', minDuration: {}, maxDuration: {}, quality: '{}', firstLinkId: {}, offset: {}, limit: {}",
        tenantId, categoryName, minDuration, maxDuration, quality, finalFirstLinkId, finalOffset, finalLimit);
    
    List<LinkCategory> dbItems;
    if (finalFirstLinkId != null) {
      // Use exclusion query to avoid duplicating the first link
      logger.info("Using EXCLUSION query to avoid duplicating first link");
      dbItems = LoggingUtil.logOperationTime(
          logger,
          "fetch items with pagination excluding first link",
          () -> linkCategoryService.findByCategoryWithFiltersExcludingLinkPageable(
              tenantId,
              categoryName,
              minDuration,
              maxDuration,
              quality,
              finalFirstLinkId,
              finalOffset,
              finalLimit
          )
      );
    } else {
      // Use regular pagination if no first link to exclude
      logger.info("Using REGULAR pagination (no first link to exclude)");
      dbItems = LoggingUtil.logOperationTime(
        logger,
        "fetch items with pagination",
        () -> linkCategoryService.findByCategoryWithFiltersPageable(
            tenantId,
            categoryName,
            minDuration,
            maxDuration,
            quality,
              finalOffset,
              finalLimit
        )
    );
    }
    
    logger.info("Retrieved {} items from database for page {}", dbItems.size(), pageable.getPageNumber());
    
    // Add DB items to page content if first page didn't already add items
    if (pageContent.isEmpty()) {
      logger.info("Page content is empty - adding {} items from database", dbItems.size());
      pageContent.addAll(createDTOsFromLinkCategories(dbItems, category));
    } else {
      logger.info("Page content already has {} items - not adding database items", pageContent.size());
    }

    logger.info("Page {} COMPLETED - Total items: {} for tenant {} and category '{}'",
        pageable.getPageNumber(), pageContent.size(), tenantId, categoryName);

    Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
    logger.info("Caching page {} result with key: '{}'", pageable.getPageNumber(), cacheKey);
    cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1, TimeUnit.HOURS);
    long methodDuration = System.currentTimeMillis() - methodStartTime;
    logger.info("=== Method completed in {} ms - returning page {} with {} items ===", 
        methodDuration, pageable.getPageNumber(), pageContent.size());
    return result;
  }

  /**
   * Helper method to create DTOs from LinkCategory entities
   */
  private List<CategoryWithLinkDTO> createDTOsFromLinkCategories(List<LinkCategory> linkCategories,
      AllCat category) {
    logger.info("Creating DTOs from {} LinkCategory entities for category '{}'", 
        linkCategories.size(), category.getName());
    List<CategoryWithLinkDTO> dtos = new ArrayList<>();
    int skippedCount = 0;

    for (LinkCategory linkCategory : linkCategories) {
      Link link = linkCategory.getLink();

      // Skip links that don't match the tenant id (additional safety check)
      if (!link.getTenantId().equals(category.getTenantId())) {
        logger.info("Skipping link due to tenant mismatch - Link tenantId: {}, Category tenantId: {}, Link: '{}'", 
            link.getTenantId(), category.getTenantId(), link.getLink());
        skippedCount++;
        continue;
      }

      CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
      dto.setId(category.getId());
      dto.setName(category.getName());
      dto.setDescription(category.getDescription());
      dto.setLink(link.getLink());
      dto.setLinkTitle(link.getTitle());
      dto.setLinkThumbnail(link.getThumbnail());
      dto.setLinkThumbPath(link.getThumbpath());
      dto.setLinkDuration(link.getDuration());
      dto.setLinkSource(link.getSource());
      dto.setLinkTrailer(link.getTrailer());
      dtos.add(dto);
      
      logger.info("Created DTO for link - ID: {}, Title: '{}', Duration: {}, Source: '{}'", 
          link.getId(), link.getTitle(), link.getDuration(), link.getSource());
    }

    logger.info("DTO creation completed - Created: {} DTOs, Skipped: {} links (tenant mismatch) for category '{}'", 
        dtos.size(), skippedCount, category.getName());
    return dtos;
  }
} 