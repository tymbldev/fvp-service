package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.ModelWithLinkDTO;
import com.fvp.dto.ModelLinksResponseDTO;
import com.fvp.entity.Link;
import com.fvp.entity.Model;
import com.fvp.repository.LinkRepository;
import com.fvp.repository.ModelRepository;
import com.fvp.repository.ElasticsearchLinkModelRepository;
import com.fvp.document.LinkDocument;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ModelUtilService {

  private static final Logger logger = LoggerFactory.getLogger(ModelUtilService.class);
  private static final String MODEL_LINKS_CACHE = "modelLinks";

  @Autowired
  private ModelRepository modelRepository;

  @Autowired
  private LinkRepository linkRepository;

  @Autowired
  private LinkService linkService;

  @Autowired
  private ModelService modelService;

  @Autowired
  private CacheService cacheService;

  @Autowired
  private ElasticsearchLinkModelRepository elasticsearchLinkModelRepository;

  public List<ModelWithLinkDTO> getFirstLinksForModels(Integer tenantId, List<String> modelNames) {
    List<ModelWithLinkDTO> results = new ArrayList<>();

    for (String modelName : modelNames) {
      try {
        // Get the model entity first
        Model model = modelRepository.findByTenantIdAndName(tenantId, modelName);
        if (model == null) {
          continue;
        }

        ModelWithLinkDTO dto = new ModelWithLinkDTO();
        // Set all model fields
        dto.setId(model.getId());
        dto.setTenantId(model.getTenantId());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setCountry(model.getCountry());
        dto.setThumbnail(model.getThumbnail());
        dto.setThumbPath(model.getThumbpath());
        dto.setAge(model.getAge());
        results.add(dto);
      } catch (Exception e) {
        logger.error("Error getting model {}: {}", modelName, e.getMessage(), e);
      }
    }

    return results;
  }

  public ModelWithLinkDTO getModelWithFirstLink(Integer tenantId, String modelName) {
    return LoggingUtil.logOperationTime(logger, "get model with first link", () -> {
      Model model = modelRepository.findByTenantIdAndName(tenantId, modelName);
      if (model == null) {
        return null;
      }

      // Get random link for the model
      Optional<LinkDocument> randomLinkDoc = elasticsearchLinkModelRepository.findRandomLinkByModel(tenantId, modelName);

      if (!randomLinkDoc.isPresent()) {
        return null;
      }

      try {
        // Extract link ID from LinkDocument
        String linkIdStr = randomLinkDoc.get().getLinkId();
        if (linkIdStr == null) {
          return null;
        }
        
        Integer linkId = Integer.parseInt(linkIdStr);
        Link link = linkRepository.findById(linkId).orElse(null);
        if (link == null) {
          return null;
        }

        ModelWithLinkDTO dto = new ModelWithLinkDTO();
        dto.setId(model.getId());
        dto.setTenantId(model.getTenantId());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setCountry(model.getCountry());
        dto.setThumbnail(model.getThumbnail());
        dto.setThumbPath(model.getThumbpath());
        dto.setAge(model.getAge());
        
        // Set link fields
        dto.setLink(link.getLink());
        dto.setLinkTitle(link.getTitle());
        dto.setLinkThumbnail(link.getThumbnail());
        dto.setLinkThumbPath(link.getThumbpath());
        dto.setLinkSource(link.getSource());
        dto.setLinkTrailer(link.getTrailer());
        dto.setLinkDuration(link.getDuration());
        dto.setLinkId((long) link.getId());

        return dto;
      } catch (Exception e) {
        logger.error("Error fetching link details for model {}: {}",
            modelName, e.getMessage());
        return null;
      }
    });
  }

  public Page<ModelLinksResponseDTO> getModelLinks(Integer tenantId, String modelName, Pageable pageable,
      Integer maxDuration, String quality) {
    return LoggingUtil.logOperationTime(logger, "get model links", () -> {
      if (tenantId == null || modelName == null) {
        logger.warn("Invalid parameters: tenantId={}, modelName={}", tenantId, modelName);
        return Page.empty();
      }

      // Generate cache key based on all parameters
      String cacheKey = String.format("%s_%s_%d_%d_%s_%s",
          tenantId,
          modelName,
          pageable.getPageNumber(),
          pageable.getPageSize(),
          maxDuration != null ? maxDuration : "null",
          quality != null ? quality : "null"
      );

      // Try to get from cache first
      TypeReference<Page<ModelLinksResponseDTO>> typeRef = new TypeReference<Page<ModelLinksResponseDTO>>() {};
      Optional<Page<ModelLinksResponseDTO>> cachedPage = cacheService.getCollectionFromCache(
          MODEL_LINKS_CACHE,
          cacheKey,
          typeRef
      );

      if (cachedPage.isPresent()) {
        logger.info("Cache hit for model links: {}", cacheKey);
        return cachedPage.get();
      }

      logger.info("Cache miss for model links: {}", cacheKey);

      // Get the model by name
      Model model = modelRepository.findByTenantIdAndName(tenantId, modelName);
      if (model == null) {
        logger.warn("Model not found for tenant {} and name {}", tenantId, modelName);
        return Page.empty();
      }

      // Get the total count with filters (for pagination metadata)
      Long totalCount = elasticsearchLinkModelRepository.countByModelWithFilters(tenantId, modelName, maxDuration, quality);
      if (totalCount == 0) {
        logger.info("No links found for model {} with given filters", modelName);
        return Page.empty();
      }

      List<ModelLinksResponseDTO.LinkDTO> pageContent = new ArrayList<>();

      // For first page, include the "first link" as the first item
      if (pageable.getPageNumber() == 0) {
        ModelWithLinkDTO firstLink = modelService.getModelFirstLink(tenantId, modelName);

        if (firstLink != null) {
          // Only include the first link when no filters are applied (default landing)
          boolean includeFirstLink = (maxDuration == null && quality == null);

          if (includeFirstLink) {
            ModelLinksResponseDTO.LinkDTO firstLinkDTO = new ModelLinksResponseDTO.LinkDTO();
            pageContent.add(firstLinkDTO);

            // If first page has only one item (pageSize=1), we're done
            if (pageable.getPageSize() == 1) {
              ModelLinksResponseDTO response = createResponseDTO(model, pageContent, pageable, totalCount);
              Page<ModelLinksResponseDTO> result = new PageImpl<>(Collections.singletonList(response), pageable, totalCount);
              cacheService.putInCacheWithExpiry(MODEL_LINKS_CACHE, cacheKey, result, 1, TimeUnit.HOURS);
              return result;
            }

            // Get link entity for exclusion from subsequent query
            Link linkEntity = null;
            try {
              linkEntity = linkRepository.findById(firstLink.getId()).orElse(null);
            } catch (Exception e) {
              logger.warn("Error fetching link entity: {}", e.getMessage());
            }

            // For first page, get one less item from DB and exclude the first link's ID
            Integer firstLinkId = linkEntity != null ? linkEntity.getId() : null;

            // Adjust offset and limit for the subsequent query
            int adjustedOffset = 0;
            int limit = pageable.getPageSize() - 1;

            // Get the remaining items for the first page
            List<LinkDocument> remainingLinks = elasticsearchLinkModelRepository.findByModelWithFiltersExcludingLinkPageable(
                    tenantId, modelName, maxDuration, quality, firstLinkId, adjustedOffset, limit);

            try {
              // Extract all link IDs from LinkDocument
              List<Integer> linkIds = remainingLinks.stream()
                  .map(linkDoc -> {
                    try {
                      return Integer.parseInt(linkDoc.getLinkId());
                    } catch (NumberFormatException e) {
                      logger.warn("Invalid linkId format: {}", linkDoc.getLinkId());
                      return null;
                    }
                  })
                  .filter(id -> id != null)
                  .collect(Collectors.toList());
              
              // Fetch all links in a single database call
              if (!linkIds.isEmpty()) {
                List<Link> links = linkRepository.findAllById(linkIds);
                
                // Create a map for quick lookup
                Map<Integer, Link> linkMap = links.stream()
                    .collect(Collectors.toMap(Link::getId, link -> link));
                
                // Create DTOs with all fields properly mapped
                for (LinkDocument linkDoc : remainingLinks) {
                  try {
                    Integer linkId = Integer.parseInt(linkDoc.getLinkId());
                    Link link = linkMap.get(linkId);
                    if (link != null) {
                      ModelLinksResponseDTO.LinkDTO linkDTO = new ModelLinksResponseDTO.LinkDTO();
                      linkDTO.setLink(link.getLink());
                      linkDTO.setLinkTitle(link.getTitle());
                      linkDTO.setLinkThumbnail(link.getThumbnail());
                      linkDTO.setLinkThumbPath(link.getThumbpath());
                      linkDTO.setLinkSource(link.getSource());
                      linkDTO.setLinkTrailer(link.getTrailer());
                      linkDTO.setLinkDuration(link.getDuration());
                      pageContent.add(linkDTO);
                    }
                  } catch (NumberFormatException e) {
                    logger.warn("Invalid linkId format: {}", linkDoc.getLinkId());
                  }
                }
              }
            } catch (Exception e) {
              logger.error("Error fetching link details in batch: {}", e.getMessage());
            }
          } else {
            // If filters are applied, get all items for the first page
            List<LinkDocument> firstPageLinks = elasticsearchLinkModelRepository.findByModelWithFiltersPageable(
                    tenantId, modelName, maxDuration, quality, 0, pageable.getPageSize());

            try {
              // Extract all link IDs from LinkDocument
              List<Integer> linkIds = firstPageLinks.stream()
                  .map(linkDoc -> {
                    try {
                      return Integer.parseInt(linkDoc.getLinkId());
                    } catch (NumberFormatException e) {
                      logger.warn("Invalid linkId format: {}", linkDoc.getLinkId());
                      return null;
                    }
                  })
                  .filter(id -> id != null)
                  .collect(Collectors.toList());
              
              // Fetch all links in a single database call
              if (!linkIds.isEmpty()) {
                List<Link> links = linkRepository.findAllById(linkIds);
                
                // Create a map for quick lookup
                Map<Integer, Link> linkMap = links.stream()
                    .collect(Collectors.toMap(Link::getId, link -> link));
                
                // Create DTOs with all fields properly mapped
                for (LinkDocument linkDoc : firstPageLinks) {
                  try {
                    Integer linkId = Integer.parseInt(linkDoc.getLinkId());
                    Link link = linkMap.get(linkId);
                    if (link != null) {
                      ModelLinksResponseDTO.LinkDTO linkDTO = new ModelLinksResponseDTO.LinkDTO();
                      linkDTO.setLink(link.getLink());
                      linkDTO.setLinkTitle(link.getTitle());
                      linkDTO.setLinkThumbnail(link.getThumbnail());
                      linkDTO.setLinkThumbPath(link.getThumbpath());
                      linkDTO.setLinkSource(link.getSource());
                      linkDTO.setLinkTrailer(link.getTrailer());
                      linkDTO.setLinkDuration(link.getDuration());
                      pageContent.add(linkDTO);
                    }
                  } catch (NumberFormatException e) {
                    logger.warn("Invalid linkId format: {}", linkDoc.getLinkId());
                  }
                }
              }
            } catch (Exception e) {
              logger.error("Error fetching link details for first page: {}", e.getMessage());
            }
          }
        }
      } else {
        // For subsequent pages, get items directly
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        List<LinkDocument> pageLinks = elasticsearchLinkModelRepository.findByModelWithFiltersPageable(
                tenantId, modelName, maxDuration, quality, offset, pageable.getPageSize());

        try {
          // Extract all link IDs from LinkDocument
          List<Integer> linkIds = pageLinks.stream()
              .map(linkDoc -> {
                try {
                  return Integer.parseInt(linkDoc.getLinkId());
                } catch (NumberFormatException e) {
                  logger.warn("Invalid linkId format: {}", linkDoc.getLinkId());
                  return null;
                }
              })
              .filter(id -> id != null)
              .collect(Collectors.toList());
          
          // Fetch all links in a single database call
          if (!linkIds.isEmpty()) {
            List<Link> links = linkRepository.findAllById(linkIds);
            
            // Create a map for quick lookup
            Map<Integer, Link> linkMap = links.stream()
                .collect(Collectors.toMap(Link::getId, link -> link));
            
            // Create DTOs with all fields properly mapped
            for (LinkDocument linkDoc : pageLinks) {
              try {
                Integer linkId = Integer.parseInt(linkDoc.getLinkId());
                Link link = linkMap.get(linkId);
                if (link != null) {
                  ModelLinksResponseDTO.LinkDTO linkDTO = new ModelLinksResponseDTO.LinkDTO();
                  linkDTO.setLink(link.getLink());
                  linkDTO.setLinkTitle(link.getTitle());
                  linkDTO.setLinkThumbnail(link.getThumbnail());
                  linkDTO.setLinkThumbPath(link.getThumbpath());
                  linkDTO.setLinkSource(link.getSource());
                  linkDTO.setLinkTrailer(link.getTrailer());
                  linkDTO.setLinkDuration(link.getDuration());
                  pageContent.add(linkDTO);
                }
              } catch (NumberFormatException e) {
                logger.warn("Invalid linkId format: {}", linkDoc.getLinkId());
              }
            }
          }
        } catch (Exception e) {
          logger.error("Error fetching link details for page: {}", e.getMessage());
        }
      }

      ModelLinksResponseDTO response = createResponseDTO(model, pageContent, pageable, totalCount);
      Page<ModelLinksResponseDTO> result = new PageImpl<>(Collections.singletonList(response), pageable, totalCount);
      cacheService.putInCacheWithExpiry(MODEL_LINKS_CACHE, cacheKey, result, 1, TimeUnit.HOURS);
      return result;
    });
  }

  private ModelLinksResponseDTO createResponseDTO(Model model, List<ModelLinksResponseDTO.LinkDTO> links, Pageable pageable, long totalCount) {
    ModelLinksResponseDTO response = new ModelLinksResponseDTO();
    
    // Set individual model fields
    response.setId(model.getId());
    response.setTenantId(model.getTenantId());
    response.setName(model.getName());
    response.setDescription(model.getDescription());
    response.setCountry(model.getCountry());
    response.setThumbnail(model.getThumbnail());
    response.setThumbPath(model.getThumbpath());
    response.setAge(model.getAge());
    
    // Set links and pagination data
    response.setLinks(links);
    response.setTotalElements(totalCount);
    response.setTotalPages((int) Math.ceil((double) totalCount / pageable.getPageSize()));
    response.setNumber(pageable.getPageNumber());
    response.setSize(pageable.getPageSize());
    response.setFirst(pageable.getPageNumber() == 0);
    response.setLast(pageable.getPageNumber() >= response.getTotalPages() - 1);
    response.setEmpty(links.isEmpty());
    return response;
  }
} 