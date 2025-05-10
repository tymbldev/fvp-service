package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.ModelWithLinkDTO;
import com.fvp.dto.ModelLinksResponseDTO;
import com.fvp.entity.BaseLinkModel;
import com.fvp.entity.Link;
import com.fvp.entity.LinkModel;
import com.fvp.entity.Model;
import com.fvp.repository.LinkRepository;
import com.fvp.repository.ModelRepository;
import com.fvp.repository.ShardedLinkModelRepository;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
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
  private LinkModelShardingService shardingService;

  @Autowired
  private LinkService linkService;

  @Autowired
  private ModelService modelService;

  @Autowired
  private CacheService cacheService;

  public List<ModelWithLinkDTO> getFirstLinksForModels(Integer tenantId, List<String> modelNames) {
    List<ModelWithLinkDTO> results = new ArrayList<>();

    for (String modelName : modelNames) {
      try {
        ShardedLinkModelRepository<? extends BaseLinkModel> repository =
            shardingService.getRepositoryForModel(modelName);

        List<? extends BaseLinkModel> linkModels = repository.findByModelAndTenantId(modelName,
            tenantId);
        if (!linkModels.isEmpty()) {
          BaseLinkModel linkModel = linkModels.get(0);
          Link link = linkRepository.findById(linkModel.getLinkId()).orElse(null);

          if (link != null) {
            ModelWithLinkDTO dto = new ModelWithLinkDTO();
            dto.setId(linkModel.getId());
            dto.setName(modelName);
            dto.setLink(link.getLink());
            dto.setLinkTitle(link.getTitle());
            dto.setLinkThumbnail(link.getThumbnail());
            dto.setLinkThumbPath(link.getThumbpath());
            dto.setLinkSource(link.getSource());
            dto.setLinkTrailer(link.getTrailer());
            dto.setLinkDuration(link.getDuration());
            results.add(dto);
          }
        }
      } catch (Exception e) {
        logger.error("Error getting first link for model {}: {}", modelName, e.getMessage(), e);
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
      Optional<? extends BaseLinkModel> randomLinkBase = shardingService.getRepositoryForModel(
              modelName)
          .findRandomLinkByModel(tenantId, modelName);

      if (!randomLinkBase.isPresent()) {
        return null;
      }

      LinkModel randomLink = new LinkModel();
      BeanUtils.copyProperties(randomLinkBase.get(), randomLink);

      try {
        Link link = linkRepository.findById(randomLink.getLinkId()).orElse(null);
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
        dto.setLink(link.getLink());
        dto.setLinkTitle(link.getTitle());
        dto.setLinkThumbnail(link.getThumbnail());
        dto.setLinkThumbPath(link.getThumbpath());
        dto.setLinkDuration(link.getDuration());

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
      Long totalCount = shardingService.getRepositoryForModel(modelName)
          .countByModelWithFilters(tenantId, modelName, maxDuration, quality);
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
            ModelLinksResponseDTO.LinkDTO firstLinkDTO = convertToLinkDTO(firstLink);
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
              linkEntity = linkRepository.findById(firstLink.getLinkId().intValue()).orElse(null);
            } catch (Exception e) {
              logger.warn("Error fetching link entity: {}", e.getMessage());
            }

            // For first page, get one less item from DB and exclude the first link's ID
            Integer firstLinkId = linkEntity != null ? linkEntity.getId() : null;

            // Adjust offset and limit for the subsequent query
            int adjustedOffset = 0;
            int limit = pageable.getPageSize() - 1;

            // Get the remaining items for the first page
            List<? extends BaseLinkModel> remainingLinksBase = shardingService.getRepositoryForModel(modelName)
                .findByModelWithFiltersExcludingLinkPageable(
                    tenantId, modelName, maxDuration, quality, firstLinkId, adjustedOffset, limit);

            List<LinkModel> remainingLinks = remainingLinksBase.stream()
                .map(baseLinkModel -> {
                    LinkModel linkModel = new LinkModel();
                    BeanUtils.copyProperties(baseLinkModel, linkModel);
                    return linkModel;
                })
                .collect(Collectors.toList());

            for (LinkModel linkModel : remainingLinks) {
              try {
                Link link = linkRepository.findById(linkModel.getLinkId()).orElse(null);
                if (link != null) {
                  ModelLinksResponseDTO.LinkDTO linkDTO = convertToLinkDTO(link);
                  pageContent.add(linkDTO);
                }
              } catch (Exception e) {
                logger.error("Error fetching link details for link ID {}: {}", linkModel.getLinkId(), e.getMessage());
              }
            }
          } else {
            // If filters are applied, get all items for the first page
            List<? extends BaseLinkModel> firstPageLinksBase = shardingService.getRepositoryForModel(modelName)
                .findByModelWithFiltersPageable(
                    tenantId, modelName, maxDuration, quality, 0, pageable.getPageSize());

            List<LinkModel> firstPageLinks = firstPageLinksBase.stream()
                .map(baseLinkModel -> {
                    LinkModel linkModel = new LinkModel();
                    BeanUtils.copyProperties(baseLinkModel, linkModel);
                    return linkModel;
                })
                .collect(Collectors.toList());

            for (LinkModel linkModel : firstPageLinks) {
              try {
                Link link = linkRepository.findById(linkModel.getLinkId()).orElse(null);
                if (link != null) {
                  ModelLinksResponseDTO.LinkDTO linkDTO = convertToLinkDTO(link);
                  pageContent.add(linkDTO);
                }
              } catch (Exception e) {
                logger.error("Error fetching link details for link ID {}: {}", linkModel.getLinkId(), e.getMessage());
              }
            }
          }
        }
      } else {
        // For subsequent pages, get items directly
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        List<? extends BaseLinkModel> pageLinksBase = shardingService.getRepositoryForModel(modelName)
            .findByModelWithFiltersPageable(
                tenantId, modelName, maxDuration, quality, offset, pageable.getPageSize());

        List<LinkModel> pageLinks = pageLinksBase.stream()
            .map(baseLinkModel -> {
                LinkModel linkModel = new LinkModel();
                BeanUtils.copyProperties(baseLinkModel, linkModel);
                return linkModel;
            })
            .collect(Collectors.toList());

        for (LinkModel linkModel : pageLinks) {
          try {
            Link link = linkRepository.findById(linkModel.getLinkId()).orElse(null);
            if (link != null) {
              ModelLinksResponseDTO.LinkDTO linkDTO = convertToLinkDTO(link);
              pageContent.add(linkDTO);
            }
          } catch (Exception e) {
            logger.error("Error fetching link details for link ID {}: {}", linkModel.getLinkId(), e.getMessage());
          }
        }
      }

      ModelLinksResponseDTO response = createResponseDTO(model, pageContent, pageable, totalCount);
      Page<ModelLinksResponseDTO> result = new PageImpl<>(Collections.singletonList(response), pageable, totalCount);
      cacheService.putInCacheWithExpiry(MODEL_LINKS_CACHE, cacheKey, result, 1, TimeUnit.HOURS);
      return result;
    });
  }

  private ModelLinksResponseDTO.LinkDTO convertToLinkDTO(Link link) {
    ModelLinksResponseDTO.LinkDTO linkDTO = new ModelLinksResponseDTO.LinkDTO();
    linkDTO.setLink(link.getLink());
    linkDTO.setLinkTitle(link.getTitle());
    linkDTO.setLinkThumbnail(link.getThumbnail());
    linkDTO.setLinkThumbPath(link.getThumbpath());
    linkDTO.setLinkSource(link.getSource());
    linkDTO.setLinkTrailer(link.getTrailer());
    linkDTO.setLinkDuration(link.getDuration());
    return linkDTO;
  }

  private ModelLinksResponseDTO.LinkDTO convertToLinkDTO(ModelWithLinkDTO dto) {
    ModelLinksResponseDTO.LinkDTO linkDTO = new ModelLinksResponseDTO.LinkDTO();
    linkDTO.setLink(dto.getLink());
    linkDTO.setLinkTitle(dto.getLinkTitle());
    linkDTO.setLinkThumbnail(dto.getLinkThumbnail());
    linkDTO.setLinkThumbPath(dto.getLinkThumbPath());
    linkDTO.setLinkSource(dto.getLinkSource());
    linkDTO.setLinkTrailer(dto.getLinkTrailer());
    linkDTO.setLinkDuration(dto.getLinkDuration());
    return linkDTO;
  }

  private ModelLinksResponseDTO createResponseDTO(Model model, List<ModelLinksResponseDTO.LinkDTO> links, Pageable pageable, long totalCount) {
    ModelLinksResponseDTO response = new ModelLinksResponseDTO();
    response.setId(model.getId());
    response.setTenantId(model.getTenantId());
    response.setName(model.getName());
    response.setDescription(model.getDescription());
    response.setCountry(model.getCountry());
    response.setThumbnail(model.getThumbnail());
    response.setThumbPath(model.getThumbpath());
    response.setAge(model.getAge());
    response.setLinks(links);
    response.setTotalElements(totalCount);
    response.setTotalPages((int) Math.ceil((double) totalCount / pageable.getPageSize()));
    response.setNumber(pageable.getPageNumber());
    response.setSize(pageable.getPageSize());
    response.setFirst(pageable.getPageNumber() == 0);
    response.setLast(pageable.getPageNumber() >= (totalCount - 1) / pageable.getPageSize());
    response.setEmpty(links.isEmpty());
    return response;
  }
} 