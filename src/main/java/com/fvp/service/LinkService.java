package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fvp.entity.Link;
import com.fvp.repository.LinkRepository;
import com.fvp.util.LoggingUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
            Link foundLink = linkRepository.findByTenantIdAndLinkAndThumbPathProcessedTrue(tenantId, link);
            if (foundLink != null) {
                cacheService.putInCacheWithExpiry(CacheService.CACHE_NAME_LINKS, cacheKey, foundLink, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
            }
            return foundLink;
        });
    }

    public List<Link> findByTenantId(Integer tenantId) {
        String cacheKey = generateCacheKey(tenantId, "all");
        
        return LoggingUtil.logOperationTime(logger, "find all links by tenant", () -> {
            TypeReference<List<Link>> typeRef = new TypeReference<List<Link>>() {};
            Optional<List<Link>> cachedLinks = cacheService.getCollectionFromCache(
                CacheService.CACHE_NAME_LINKS,
                cacheKey,
                typeRef
            );
            if (cachedLinks.isPresent()) {
                logger.info("Cache hit for all links for tenant: {}", tenantId);
                return cachedLinks.get();
            }

            logger.info("Cache miss for all links for tenant: {}", tenantId);
            List<Link> links = linkRepository.findByTenantId(tenantId);
            if (!links.isEmpty()) {
                cacheService.putInCacheWithExpiry(CacheService.CACHE_NAME_LINKS, cacheKey, links, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
            }
            return links;
        });
    }

    public Page<Link> findByTenantId(Integer tenantId, Pageable pageable) {
        // Pagination results are not cached as they are dynamic
        return LoggingUtil.logOperationTime(logger, "find links by tenant with pagination", () -> 
            linkRepository.findByTenantId(tenantId, pageable)
        );
    }

    public Page<Link> findByCreatedOnBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        // Date range queries are not cached as they are dynamic
        return LoggingUtil.logOperationTime(logger, "find links by date range with pagination", () -> 
            linkRepository.findByCreatedOnBetween(startDate, endDate, pageable)
        );
    }

    public Page<Link> findByCategoryContaining(String category, Pageable pageable) {
        // Category search results are not cached as they are dynamic
        return LoggingUtil.logOperationTime(logger, "find links by category with pagination", () -> 
            linkRepository.findByCategoryContaining(category, pageable)
        );
    }

    public Link findByLinkAndTenantId(String link, Integer tenantId) {
        String cacheKey = generateCacheKey(tenantId, "url_" + link);
        
        return LoggingUtil.logOperationTime(logger, "find link by URL and tenant", () -> {
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
            Link foundLink = linkRepository.findByLinkAndTenantId(link, tenantId);
            if (foundLink != null) {
                cacheService.putInCacheWithExpiry(CacheService.CACHE_NAME_LINKS, cacheKey, foundLink, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
            }
            return foundLink;
        });
    }

    public List<Link> findAllWithPagination(int offset, int limit) {
        // Pagination results are not cached as they are dynamic
        return LoggingUtil.logOperationTime(logger, "find all links with pagination", () -> 
            linkRepository.findAllWithPagination(offset, limit)
        );
    }

    public List<Integer> findDistinctTenantIds() {
        String cacheKey = "distinct_tenant_ids";
        
        return LoggingUtil.logOperationTime(logger, "find distinct tenant IDs", () -> {
            TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {};
            Optional<List<Integer>> cachedTenantIds = cacheService.getCollectionFromCache(
                CacheService.CACHE_NAME_LINKS,
                cacheKey,
                typeRef
            );
            if (cachedTenantIds.isPresent()) {
                logger.info("Cache hit for distinct tenant IDs");
                return cachedTenantIds.get();
            }

            logger.info("Cache miss for distinct tenant IDs");
            List<Integer> tenantIds = linkRepository.findDistinctTenantIds();
            if (!tenantIds.isEmpty()) {
                cacheService.putInCacheWithExpiry(CacheService.CACHE_NAME_LINKS, cacheKey, tenantIds, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
            }
            return tenantIds;
        });
    }

    public Link saveAndFlush(Link link) {
        // Invalidate relevant caches when saving a link
        if (link.getId() != null) {
            // Invalidate ID-based cache
            String idCacheKey = generateCacheKey(null, "id_" + link.getId());
            cacheService.evictFromCache(CacheService.CACHE_NAME_LINKS, idCacheKey);
            
            // Invalidate tenant-based caches
            String tenantCacheKey = generateCacheKey(link.getTenantId(), "all");
            cacheService.evictFromCache(CacheService.CACHE_NAME_LINKS, tenantCacheKey);
            
            // Invalidate count cache
            String countCacheKey = generateCacheKey(link.getTenantId(), "count");
            cacheService.evictFromCache(CacheService.CACHE_NAME_LINKS, countCacheKey);
        }
        
        return LoggingUtil.logOperationTime(logger, "save and flush link", () -> 
            linkRepository.saveAndFlush(link)
        );
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