package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.dto.ModelWithLinkDTO;
import com.fvp.entity.BaseLinkModel;
import com.fvp.entity.Link;
import com.fvp.entity.LinkModel;
import com.fvp.entity.Model;
import com.fvp.repository.LinkRepository;
import com.fvp.repository.ModelRepository;
import com.fvp.repository.ShardedLinkModelRepository;
import com.fvp.util.LoggingUtil;
import java.util.ArrayList;
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

  public Page<ModelWithLinkDTO> getModelLinks(Integer tenantId, String modelName, Pageable pageable,
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
      TypeReference<Page<ModelWithLinkDTO>> typeRef = new TypeReference<Page<ModelWithLinkDTO>>() {
      };
      Optional<Page<ModelWithLinkDTO>> cachedPage = cacheService.getCollectionFromCache(
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

      List<ModelWithLinkDTO> pageContent = new ArrayList<>();

      // For first page, include the "first link" as the first item
      if (pageable.getPageNumber() == 0) {
        ModelWithLinkDTO firstLink = modelService.getModelFirstLink(tenantId, modelName);

        if (firstLink != null) {
          // Only include the first link when no filters are applied (default landing)
          boolean includeFirstLink = (maxDuration == null && quality == null);

          if (includeFirstLink) {
            pageContent.add(firstLink);

            // If first page has only one item (pageSize=1), we're done
            if (pageable.getPageSize() == 1) {
              Page<ModelWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
              cacheService.putInCacheWithExpiry(MODEL_LINKS_CACHE, cacheKey, result, 1,
                  TimeUnit.HOURS);
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
            List<? extends BaseLinkModel> dbItemsBase = shardingService.getRepositoryForModel(
                    modelName)
                .findByModelWithFiltersExcludingLinkPageable(
                    tenantId,
                    modelName,
                    maxDuration,
                    quality,
                    firstLinkId,
                    adjustedOffset,
                    limit
                );

            List<LinkModel> dbItems = dbItemsBase.stream()
                .map(baseLinkModel -> {
                  LinkModel linkModel = new LinkModel();
                  BeanUtils.copyProperties(baseLinkModel, linkModel);
                  return linkModel;
                })
                .collect(Collectors.toList());

            pageContent.addAll(createDTOsFromLinkModels(dbItems, model));
          }
        }
      }

      // For non-first pages or when filters are applied, use regular pagination
      if (pageContent.isEmpty()) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<? extends BaseLinkModel> dbItemsBase = shardingService.getRepositoryForModel(modelName)
            .findByModelWithFiltersPageable(
                tenantId,
                modelName,
                maxDuration,
                quality,
                offset,
                limit
            );

        List<LinkModel> dbItems = dbItemsBase.stream()
            .map(baseLinkModel -> {
              LinkModel linkModel = new LinkModel();
              BeanUtils.copyProperties(baseLinkModel, linkModel);
              return linkModel;
            })
            .collect(Collectors.toList());

        pageContent.addAll(createDTOsFromLinkModels(dbItems, model));
      }

      logger.info("Returning page {} with {} items for tenant {} and model {}",
          pageable.getPageNumber(), pageContent.size(), tenantId, modelName);

      Page<ModelWithLinkDTO> result = new PageImpl<>(pageContent, pageable, totalCount);
      cacheService.putInCacheWithExpiry(MODEL_LINKS_CACHE, cacheKey, result, 1, TimeUnit.HOURS);
      return result;
    });
  }

  private List<ModelWithLinkDTO> createDTOsFromLinkModels(List<LinkModel> linkModels, Model model) {
    List<ModelWithLinkDTO> dtos = new ArrayList<>();

    for (LinkModel linkModel : linkModels) {
      try {
        Link link = linkRepository.findById(linkModel.getLinkId()).orElse(null);
        if (link == null) {
          continue;
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
        dtos.add(dto);
      } catch (Exception e) {
        logger.error("Error fetching link details for link ID {}: {}",
            linkModel.getLinkId(), e.getMessage());
      }
    }

    return dtos;
  }
} 