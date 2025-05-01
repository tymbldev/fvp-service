package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryUtilService {

  private static final Logger logger = LoggingUtil.getLogger(CategoryUtilService.class);
  private static final String CATEGORY_LINKS_CACHE = "categoryLinks";

  @Autowired
  private LinkCategoryRepository linkCategoryRepository;

  @Autowired
  private AllCatService allCatService;

  @Autowired
  private LinkService linkService;

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private CacheService cacheService;

  @Value("${category.recent-links-days:90}")
  private int recentLinksDays;

  public Page<CategoryWithLinkDTO> getCategoryLinks(Integer tenantId, String categoryName,
      Pageable pageable, Integer minDuration, Integer maxDuration, String quality) {
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters: tenantId={}, categoryName={}", tenantId, categoryName);
      return Page.empty();
    }

    logger.info(
        "Fetching category links for tenant {} and category {}, page: {}, size: {}, minDuration: {}, maxDuration: {}, quality: {}",
        tenantId, categoryName, pageable.getPageNumber(), pageable.getPageSize(), minDuration, maxDuration,
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
        () -> linkCategoryRepository.countByCategoryWithFilters(tenantId, categoryName, minDuration, maxDuration,
            quality)
    );

    if (totalCount == 0) {
      logger.info("No links found for category {} with given filters", categoryName);
      return Page.empty();
    }

    List<CategoryWithLinkDTO> pageContent = new ArrayList<>();

    // For first page, include the "first link" as the first item
    if (pageable.getPageNumber() == 0) {
      CategoryWithLinkDTO firstLink = categoryService.getCategoryFirstLink(tenantId, categoryName);

      if (firstLink != null) {
        // Only include the first link when no filters are applied (default landing)
        boolean includeFirstLink = (minDuration == null && maxDuration == null && quality == null);

        if (includeFirstLink) {
          pageContent.add(firstLink);

          // If first page has only one item (pageSize=1), we're done
          if (pageable.getPageSize() == 1) {
            Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
            cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1,
                TimeUnit.HOURS);
            return result;
          }

          // Get link entity for exclusion from subsequent query
          Link linkEntity = null;
          try {
            linkEntity = linkService.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId,
                firstLink.getLink());
          } catch (Exception e) {
            logger.warn("Error fetching link entity: {}", e.getMessage());
          }

          // For first page, get one less item from DB and exclude the first link's ID
          Integer firstLinkId = linkEntity != null ? linkEntity.getId() : null;

          // Adjust offset and limit for the subsequent query
          int adjustedOffset = 0;
          int limit = pageable.getPageSize() - 1;

          // Get the remaining items for the first page
          List<LinkCategory> dbItems = LoggingUtil.logOperationTime(
              logger,
              "fetch items with pagination excluding first link",
              () -> linkCategoryRepository.findByCategoryWithFiltersExcludingLinkPageable(
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
          Page<CategoryWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
          cacheService.putInCacheWithExpiry(CATEGORY_LINKS_CACHE, cacheKey, result, 1,
              TimeUnit.HOURS);
          return result;
        }
      }
    }

    // For non-first pages or when filters are applied, use regular pagination
    int offset = (int) pageable.getOffset();
    int limit = pageable.getPageSize();

    List<LinkCategory> dbItems = LoggingUtil.logOperationTime(
        logger,
        "fetch items with pagination",
        () -> linkCategoryRepository.findByCategoryWithFiltersPageable(
            tenantId,
            categoryName,
            minDuration,
            maxDuration,
            quality,
            offset,
            limit
        )
    );

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
      dtos.add(dto);
    }

    return dtos;
  }
} 