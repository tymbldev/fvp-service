package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.LinkRepository;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

  private static final Logger logger = LoggingUtil.getLogger(CategoryService.class);
  private static final String HOME_CATEGORIES_CACHE = "homeCategories";
  private static final String CATEGORY_FIRST_LINK_CACHE = "categoryFirstLink";
  private static final String CATEGORY_LINKS_CACHE = "categoryLinks";

  @Autowired
  private AllCatRepository allCatRepository;

  @Autowired
  private LinkCategoryRepository linkCategoryRepository;

  @Autowired
  private LinkRepository linkRepository;

  @Autowired
  private CacheService cacheService;

  @Value("${category.recent-links-days:90}")
  private int recentLinksDays;

  private final ExecutorService executorService = Executors.newFixedThreadPool(5);

  @PostConstruct
  public void preloadCacheInBackground() {
    logger.info("Starting background cache preloading for all tenants");
    
    // Get all unique tenant IDs
    List<Integer> tenantIds = LoggingUtil.logOperationTime(
        logger, 
        "fetch tenant IDs", 
        () -> linkRepository.findAll().stream()
            .map(Link::getTenantId)
            .distinct()
            .collect(Collectors.toList())
    );
    
    // Preload cache for each tenant in parallel
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    for (Integer tenantId : tenantIds) {
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          logger.info("Preloading cache for tenant {}", tenantId);
          getHomeCategoriesWithLinks(tenantId);
          logger.info("Completed cache preloading for tenant {}", tenantId);
        } catch (Exception e) {
          logger.error("Error preloading cache for tenant {}: {}", tenantId, e.getMessage(), e);
        }
      }, executorService);
      
      futures.add(future);
    }
    
    // Wait for all futures to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    logger.info("Completed background cache preloading for all tenants");
  }

  public List<CategoryWithLinkDTO> getHomeCategoriesWithLinks(Integer tenantId) {
    String cacheKey = CacheService.generateCacheKey(HOME_CATEGORIES_CACHE, tenantId);
    
    TypeReference<List<CategoryWithLinkDTO>> typeRef = new TypeReference<List<CategoryWithLinkDTO>>() {};
    
    // Try to get from cache first
    Optional<List<CategoryWithLinkDTO>> cachedResult = LoggingUtil.logOperationTime(
        logger, 
        "get home categories from cache", 
        () -> cacheService.getCollectionFromCache(
            CacheService.CACHE_NAME_CATEGORIES,
            cacheKey,
            typeRef
        )
    );

    if (cachedResult.isPresent() && !cachedResult.get().isEmpty()) {
      logger.info("Retrieved {} home categories from cache for tenant {}", 
          cachedResult.get().size(), tenantId);
      return cachedResult.get();
    }

    logger.info("Cache miss for home categories, fetching from database for tenant {}", tenantId);
    List<CategoryWithLinkDTO> result = new ArrayList<>();

    // Get all categories with home=1 ordered by home_cat_order
    List<AllCat> categories = LoggingUtil.logOperationTime(
        logger, 
        "fetch home categories from database", 
        () -> allCatRepository.findByTenantIdAndHomeOrderByHomeCatOrder(tenantId, 1)
    );

    for (AllCat category : categories) {
      // Get the count of links for this category
      Long linkCount = LoggingUtil.logOperationTime(
          logger, 
          "count links for category " + category.getName(), 
          () -> linkCategoryRepository.countByTenantIdAndCategory(tenantId, category.getName())
      );

      // Get the first link using the dedicated method
      CategoryWithLinkDTO firstLink = getCategoryFirstLink(tenantId, category.getName());
      if (firstLink == null) {
        logger.warn("No links found for category {} in tenant {}", category.getName(), tenantId);
        continue; // Skip if no links found
      }

      CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
      dto.setId(category.getId());
      dto.setName(category.getName());
      dto.setHomeThumb(category.getHomeThumb());
      dto.setHeader(category.getHeader());
      dto.setHomeSEO(category.getHomeSEO());
      dto.setHomeCatOrder(category.getHomeCatOrder());
      dto.setDescription(category.getDescription());
      dto.setLink(firstLink.getLink());
      dto.setLinkTitle(firstLink.getLinkTitle());
      dto.setLinkThumbnail(firstLink.getLinkThumbnail());
      dto.setLinkDuration(firstLink.getLinkDuration());
      dto.setLinkCount(linkCount);

      result.add(dto);
    }

    // Store in cache
    LoggingUtil.logOperationTime(
        logger, 
        "store home categories in cache", 
        () -> {
          cacheService.putInCache(CacheService.CACHE_NAME_CATEGORIES, cacheKey, result);
          return null;
        }
    );
    
    logger.info("Stored {} home categories in cache for tenant {}", result.size(), tenantId);
    return result;
  }

  public CategoryWithLinkDTO getCategoryFirstLink(Integer tenantId, String categoryName) {
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters: tenantId={}, categoryName={}", tenantId, categoryName);
      return null;
    }

    String cacheKey = tenantId + "_" + categoryName;
    
    // Try to get from cache first
    Optional<CategoryWithLinkDTO> cachedResult = LoggingUtil.logOperationTime(
        logger, 
        "get category first link from cache", 
        () -> cacheService.getFromCache(
            CATEGORY_FIRST_LINK_CACHE, 
            cacheKey, 
            CategoryWithLinkDTO.class
        )
    );
    
    if (cachedResult.isPresent()) {
      logger.info("Retrieved category first link from cache for tenant {} and category {}", 
          tenantId, categoryName);
      return cachedResult.get();
    }
    
    logger.info("Cache miss for category first link, fetching from database for tenant {} and category {}", 
        tenantId, categoryName);
    
    // Get the category by name
    AllCat category = LoggingUtil.logOperationTime(
        logger, 
        "fetch category by name", 
        () -> allCatRepository.findByTenantIdAndName(tenantId, categoryName)
    );
    
    if (category == null) {
      logger.warn("Category not found for tenant {} and name {}", tenantId, categoryName);
      return null;
    }

    // Get the count of links for this category
    Long linkCount = LoggingUtil.logOperationTime(
        logger, 
        "count links for category", 
        () -> linkCategoryRepository.countByTenantIdAndCategory(tenantId, categoryName)
    );

    // Get a random link for this category
    Optional<LinkCategory> randomLinkCategory = LoggingUtil.logOperationTime(
        logger, 
        "find random link by category", 
        () -> linkCategoryRepository.findRandomLinkByCategory(tenantId, categoryName)
    );
    
    if (!randomLinkCategory.isPresent()) {
      logger.warn("No random link found for category {} in tenant {}", categoryName, tenantId);
      return null;
    }

    Link link = randomLinkCategory.get().getLink();

    // Ensure the link belongs to the same tenant
    if (!link.getTenantId().equals(tenantId)) {
      logger.warn("Link tenant ID {} does not match requested tenant ID {}", 
          link.getTenantId(), tenantId);
      return null;
    }

    CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
    dto.setId(category.getId());
    dto.setName(category.getName());
    dto.setHomeThumb(category.getHomeThumb());
    dto.setHeader(category.getHeader());
    dto.setHomeSEO(category.getHomeSEO());
    dto.setHomeCatOrder(category.getHomeCatOrder());
    dto.setDescription(category.getDescription());
    dto.setLink(link.getLink());
    dto.setLinkTitle(link.getTitle());
    dto.setLinkThumbnail(link.getThumbnail());
    dto.setLinkDuration(link.getDuration());
    dto.setLinkCount(linkCount);

    // Store in cache
    LoggingUtil.logOperationTime(
        logger, 
        "store category first link in cache", 
        () -> {
          cacheService.putInCache(CATEGORY_FIRST_LINK_CACHE, cacheKey, dto);
          return null;
        }
    );
    
    logger.info("Stored category first link in cache for tenant {} and category {}", 
        tenantId, categoryName);
    return dto;
  }

  public Page<CategoryWithLinkDTO> getCategoryLinks(Integer tenantId, String categoryName,
      Pageable pageable, Integer maxDuration, String quality) {
    if (tenantId == null || categoryName == null) {
      logger.warn("Invalid parameters: tenantId={}, categoryName={}", tenantId, categoryName);
      return Page.empty();
    }

    logger.info("Fetching category links for tenant {} and category {}, page: {}, size: {}, maxDuration: {}, quality: {}", 
        tenantId, categoryName, pageable.getPageNumber(), pageable.getPageSize(), maxDuration, quality);
    
    // Get the category by name
    AllCat category = LoggingUtil.logOperationTime(
        logger, 
        "fetch category by name", 
        () -> allCatRepository.findByTenantIdAndName(tenantId, categoryName)
    );
    
    if (category == null) {
      logger.warn("Category not found for tenant {} and name {}", tenantId, categoryName);
      return Page.empty();
    }
    
    // Get the total count with filters (for pagination metadata)
    Long totalCount = LoggingUtil.logOperationTime(
        logger, 
        "count links with filters", 
        () -> linkCategoryRepository.countByCategoryWithFilters(tenantId, categoryName, maxDuration, quality)
    );
    
    if (totalCount == 0) {
      logger.info("No links found for category {} with given filters", categoryName);
      return Page.empty();
    }
    
    List<CategoryWithLinkDTO> pageContent = new ArrayList<>();
    
    // For first page, include the "first link" as the first item
    if (pageable.getPageNumber() == 0) {
      CategoryWithLinkDTO firstLink = getCategoryFirstLink(tenantId, categoryName);
      
      if (firstLink != null) {
        // Only include the first link when no filters are applied (default landing)
        boolean includeFirstLink = (maxDuration == null && quality == null);
        
        if (includeFirstLink) {
          pageContent.add(firstLink);
          
          // If first page has only one item (pageSize=1), we're done
          if (pageable.getPageSize() == 1) {
            return new PageImpl<>(pageContent, pageable, totalCount);
          }
          
          // Get link entity for exclusion from subsequent query
          Link linkEntity = null;
          try {
            linkEntity = linkRepository.findByTenantIdAndLink(tenantId, firstLink.getLink());
          } catch (Exception e) {
            logger.warn("Error fetching link entity: {}", e.getMessage());
          }
          
          // For first page, get one less item from DB and exclude the first link's ID
          Integer firstLinkId = linkEntity != null ? linkEntity.getId() : null;
          
          if (firstLinkId != null) {
            // Get remaining items for first page, excluding the first link
            List<LinkCategory> remainingItems = LoggingUtil.logOperationTime(
                logger, 
                "fetch remaining items for first page", 
                () -> linkCategoryRepository.findByCategoryWithFiltersExcludingLinkPageable(
                    tenantId, 
                    categoryName, 
                    maxDuration, 
                    quality,
                    firstLinkId,
                    0, // still start from beginning
                    pageable.getPageSize() - 1 // one less item
                )
            );
            
            // Add remaining items to page content
            pageContent.addAll(createDTOsFromLinkCategories(remainingItems, category));
            
            return new PageImpl<>(pageContent, pageable, totalCount);
          }
        }
      }
    }
    
    // For pages other than first page (or if first link couldn't be included)
    // Calculate correct offset, adjusting for first link if needed
    int offset = (int) pageable.getOffset();
    final int limit = pageable.getPageSize();
    
    // For first page, if we didn't add a first link, use normal pagination
    // For subsequent pages, adjust offset to account for the first link on page 0
    final int adjustedOffset;
    if (pageable.getPageNumber() > 0 && pageContent.isEmpty()) {
      adjustedOffset = offset - 1; // Reduce offset by 1 to account for first link on page 0
    } else {
      adjustedOffset = offset;
    }
    
    // Fetch items from database with pagination
    List<LinkCategory> dbItems = LoggingUtil.logOperationTime(
        logger, 
        "fetch items with pagination", 
        () -> linkCategoryRepository.findByCategoryWithFiltersPageable(
            tenantId, 
            categoryName, 
            maxDuration, 
            quality, 
            adjustedOffset, 
            limit
        )
    );
    
    // Add DB items to page content if first page didn't already add items
    if (pageContent.isEmpty()) {
      pageContent.addAll(createDTOsFromLinkCategories(dbItems, category));
    }
    
    logger.info("Returning page {} with {} items for tenant {} and category {}", 
        pageable.getPageNumber(), pageContent.size(), tenantId, categoryName);
    
    return new PageImpl<>(pageContent, pageable, totalCount);
  }
  
  /**
   * Helper method to create DTOs from LinkCategory entities
   */
  private List<CategoryWithLinkDTO> createDTOsFromLinkCategories(List<LinkCategory> linkCategories, AllCat category) {
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
      dto.setHomeThumb(category.getHomeThumb());
      dto.setHeader(category.getHeader());
      dto.setHomeSEO(category.getHomeSEO());
      dto.setHomeCatOrder(category.getHomeCatOrder());
      dto.setDescription(category.getDescription());
      dto.setLink(link.getLink());
      dto.setLinkTitle(link.getTitle());
      dto.setLinkThumbnail(link.getThumbnail());
      dto.setLinkDuration(link.getDuration());
      dto.setLinkCount(linkCategoryRepository.countByTenantIdAndCategory(category.getTenantId(), category.getName()));
      
      dtos.add(dto);
    }
    
    return dtos;
  }
} 