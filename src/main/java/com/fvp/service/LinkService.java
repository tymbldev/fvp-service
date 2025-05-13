package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.entity.Link;
import com.fvp.repository.LinkRepository;
import com.fvp.util.LoggingUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class LinkService {

  private static final Logger logger = LoggingUtil.getLogger(LinkService.class);
  private static final String LINK_CACHE_PREFIX = "link_";
  private static final int CACHE_EXPIRY_MINUTES = 60;
  private static final String LINK_COUNT_TOTAL_CACHE = "linkCountTotal";

  @Autowired
  private LinkRepository linkRepository;

  @Autowired
  private CacheService cacheService;

  public Link findByTenantIdAndLinkAndThumbPathProcessedTrue(Integer tenantId, String link) {
    String cacheKey = generateCacheKey(tenantId, "link_" + link);

    return LoggingUtil.logOperationTime(logger, "find link by tenant and URL", () -> {
      Optional<Link> cachedLink = cacheService.getFromCache(
          CacheService.CACHE_NAME_LINKS,
          cacheKey,
          Link.class
      );
      if (cachedLink.isPresent()) {
        logger.info("Cache hit for link: {}", link);
        return cachedLink.get();
      }

      logger.info("Cache miss for link: {}", link);
      Link foundLink = linkRepository.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId,
          link);
      if (foundLink != null) {
        cacheService.putInCacheWithExpiry(CacheService.CACHE_NAME_LINKS, cacheKey, foundLink,
            CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
      }
      return foundLink;
    });
  }

  //@Transactional(readOnly = true)
  public long getTotalLinkCount(Integer tenantId) {
    if (tenantId == null) {
      return 0;
    }

    String cacheKey = generateCacheKey(tenantId, "total");

    // Try to get from cache first
    Optional<Long> cachedCount = LoggingUtil.logOperationTime(
        logger,
        "get total link count from cache",
        () -> cacheService.getFromCache(LINK_COUNT_TOTAL_CACHE, cacheKey, Long.class)
    );

    if (cachedCount.isPresent()) {
      logger.debug("Cache hit for total link count: tenant={}", tenantId);
      return cachedCount.get();
    }

    logger.debug("Cache miss for total link count: tenant={}", tenantId);

    // Get count from DB
    Long count = LoggingUtil.logOperationTime(
        logger,
        "count links for tenant",
        () -> linkRepository.countByTenantId(tenantId)
    );

    // Cache the result
    LoggingUtil.logOperationTime(
        logger,
        "cache total link count",
        () -> {
          cacheService.putInCacheWithExpiry(
              LINK_COUNT_TOTAL_CACHE,
              cacheKey,
              count,
              24,
              TimeUnit.HOURS
          );
          return null;
        }
    );

    return count;
  }

  private String generateCacheKey(Integer tenantId, String suffix) {
    return LINK_CACHE_PREFIX + (tenantId != null ? tenantId + "_" : "") + suffix;
  }
}