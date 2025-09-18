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
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters: tenantId={}, categoryName={}", tenantId, categoryName);
      return Page.empty();
    }

    logger.info(
        "Fetching category links for tenant {} and category {}, page: {}, size: {}, minDuration: {}, maxDuration: {}, quality: {}",
        tenantId, categoryName, pageable.getPageNumber(), pageable.getPageSize(), minDuration,
        maxDuration,
        quality);

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

    // Try to get from cache first
    TypeReference<Page<CategoryWithLinkDTO>> typeRef = new TypeReference<Page<CategoryWithLinkDTO>>() {
    };
    Optional<Page<CategoryWithLinkDTO>> cachedPage = cacheService.getCollectionFromCache(
        CATEGORY_LINKS_CACHE,
        cacheKey,
        typeRef
    );

    if (cachedPage.isPresent()) {
      logger.info("Cache hit for category links: {}", cacheKey);
      return cachedPage.get();
    }

    logger.info("Cache miss for category links: {}", cacheKey);

    // Get the category by name
    AllCat category = LoggingUtil.logOperationTime(
        logger,
        "fetch category by name",
        () -> allCatService.findByTenantIdAndName(tenantId, categoryName)
    );

    if (category == null) {
      logger.warn("Category not found for tenant {} and name {}", tenantId, categoryName);
      return Page.empty();
    }

    // Get the total count with filters (for pagination metadata)
    Long totalCount = LoggingUtil.logOperationTime(
        logger,
        "count links with filters",
        () -> linkCategoryService.countByCategoryWithFilters(tenantId, categoryName, minDuration, maxDuration, quality)
    );

    List<CategoryWithLinkDTO> pageContent = new ArrayList<>();

    // For first page (page 0), use getCategoryFirstLink to get the first result
    if (pageable.getPageNumber() == 0) {
      logger.info("Processing first page (page 0) - using getCategoryFirstLink for tenant {} and category {}",
          tenantId, categoryName);
      
      // Get the first link using getCategoryFirstLink method
      CategoryWithLinkDTO firstLinkDTO = LoggingUtil.logOperationTime(
          logger,
          "get category first link",
          () -> categoryService.getCategoryFirstLink(tenantId, categoryName)
      );
      
      if (firstLinkDTO != null) {
        logger.info("First link found with linkId {} for tenant {} and category {}",
            firstLinkDTO.getLink(), tenantId, categoryName);
        pageContent.add(firstLinkDTO);
        
        // If first page has only one item (pageSize=1), we're done
        if (pageable.getPageSize() == 1) {
          Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
          cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1,
              TimeUnit.HOURS);
          return result;
        }
        
        // Get the link entity for exclusion from subsequent query
        Link linkEntity = null;
        try {
          linkEntity = linkService.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId,
              firstLinkDTO.getLink());
        } catch (Exception e) {
          logger.warn("Error fetching link entity for exclusion: {}", e.getMessage());
        }
        
        // For first page, get remaining items (pageSize - 1) and exclude the first link's ID
        Integer firstLinkId = linkEntity != null ? linkEntity.getId() : null;
        int adjustedOffset = 0;
        int limit = pageable.getPageSize() - 1;
        
        logger.info("Fetching {} additional items for first page, excluding linkId: {}", 
            limit, firstLinkId);
        
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
        pageContent.addAll(createDTOsFromLinkCategories(dbItems, category));
        
        logger.info("First page completed with {} total items for tenant {} and category {}",
            pageContent.size(), tenantId, categoryName);
        
        Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
        cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1,
            TimeUnit.HOURS);
        return result;
      } else {
        logger.warn("No first link found for tenant {} and category {}, falling back to regular pagination",
            tenantId, categoryName);
      }
    }

    // For non-first pages, also exclude the first link to avoid duplication
    int offset = (int) pageable.getOffset();
    int limit = pageable.getPageSize();
    
    // Get the first link to exclude it from subsequent pages
    CategoryWithLinkDTO firstLinkDTO = null;
    Integer firstLinkId = null;
    if (pageable.getPageNumber() > 0) {
      logger.info("Processing page {} - getting first link to exclude from results for tenant {} and category {}",
          pageable.getPageNumber(), tenantId, categoryName);
      
      firstLinkDTO = LoggingUtil.logOperationTime(
          logger,
          "get category first link for exclusion",
          () -> categoryService.getCategoryFirstLink(tenantId, categoryName)
      );
      
      if (firstLinkDTO != null) {
        try {
          Link linkEntity = linkService.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId,
              firstLinkDTO.getLink());
          firstLinkId = linkEntity != null ? linkEntity.getId() : null;
          logger.info("Excluding first link with ID {} from page {} results", firstLinkId, pageable.getPageNumber());
        } catch (Exception e) {
          logger.warn("Error fetching first link entity for exclusion: {}", e.getMessage());
        }
      }
    }
    
    // Adjust limit: if we're excluding the first link, we need to fetch one extra record
    // to maintain the same page size (since we're excluding one, we need one more)
    int adjustedLimit = limit;
    if (firstLinkId != null) {
      adjustedLimit = limit + 1; // Fetch one extra to compensate for exclusion
      logger.info("Adjusted limit from {} to {} to compensate for excluded first link", limit, adjustedLimit);
    }
    
    final Integer finalFirstLinkId = firstLinkId;
    final int finalOffset = offset;
    final int finalLimit = adjustedLimit;
    
    List<LinkCategory> dbItems;
    if (finalFirstLinkId != null) {
      // Use exclusion query to avoid duplicating the first link
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
    
    // Add DB items to page content if first page didn't already add items
    if (pageContent.isEmpty()) {
      pageContent.addAll(createDTOsFromLinkCategories(dbItems, category));
    }

    logger.info("Returning page {} with {} items for tenant {} and category {}",
        pageable.getPageNumber(), pageContent.size(), tenantId, categoryName);

    Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
    cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1, TimeUnit.HOURS);
    return result;
  }

  /**
   * Helper method to create DTOs from LinkCategory entities
   */
  private List<CategoryWithLinkDTO> createDTOsFromLinkCategories(List<LinkCategory> linkCategories,
      AllCat category) {
    List<CategoryWithLinkDTO> dtos = new ArrayList<>();

    for (LinkCategory linkCategory : linkCategories) {
      Link link = linkCategory.getLink();

      // Skip links that don't match the tenant id (additional safety check)
      if (!link.getTenantId().equals(category.getTenantId())) {
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
    }

    return dtos;
  }
} 