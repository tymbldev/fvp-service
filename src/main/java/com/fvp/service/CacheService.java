package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fvp.util.LoggingUtil;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class CacheService {

  private static final Logger logger = LoggingUtil.getLogger(CacheService.class);

  public static final String CACHE_NAME_CATEGORIES = "categories";
  public static final String CACHE_NAME_LINKS = "links";
  public static final String CACHE_NAME_MODELS = "models";
  public static final String CACHE_NAME_TENANTS = "tenants";

  private static final long DEFAULT_CACHE_TTL = 24 * 60 * 60; // 24 hours in seconds

  private final JedisPool jedisPool;
  private final ObjectMapper objectMapper;

  @Autowired
  public CacheService(JedisPool jedisPool, ObjectMapper objectMapper) {
    this.jedisPool = jedisPool;
    this.objectMapper = objectMapper;
  }

  /**
   * Get a value from cache
   *
   * @param cacheName The name of the cache
   * @param key The key to get
   * @param clazz The class of the value
   * @return The value, or empty if not found
   */
  public <T> Optional<T> getFromCache(String cacheName, String key, Class<T> clazz) {
    return LoggingUtil.logOperationTime(logger, "get from cache: " + cacheName + ":" + key, () -> {
      try {
        String fullKey = generateCacheKey(cacheName, key);
        String value;

        try (Jedis jedis = jedisPool.getResource()) {
          value = jedis.get(fullKey);
        }

        if (value == null) {
          logger.debug("Cache miss for key: {}", fullKey);
          return Optional.empty();
        }

        logger.debug("Cache hit for key: {}", fullKey);
        return Optional.of(objectMapper.readValue(value, clazz));
      } catch (Exception e) {
        logger.error("Error getting from cache: {}", e.getMessage(), e);
        return Optional.empty();
      }
    });
  }

  /**
   * Get a collection from cache
   *
   * @param cacheName The name of the cache
   * @param key The key to get
   * @param typeRef The type reference of the collection
   * @return The collection, or empty if not found
   */
  public <T> Optional<T> getCollectionFromCache(String cacheName, String key,
      TypeReference<T> typeRef) {
    return LoggingUtil.logOperationTime(logger,
        "get collection from cache: " + cacheName + ":" + key, () -> {
          try {
            String fullKey = generateCacheKey(cacheName, key);
            String value;

            try (Jedis jedis = jedisPool.getResource()) {
              value = jedis.get(fullKey);
            }

            if (value == null) {
              logger.debug("Cache miss for collection key: {}", fullKey);
              return Optional.empty();
            }

            logger.debug("Cache hit for collection key: {}", fullKey);
            
            // Special handling for Page objects
            if (typeRef.getType().getTypeName().contains("Page")) {
              return Optional.of((T) objectMapper.readValue(value, Page.class));
            }
            
            return Optional.of(objectMapper.readValue(value, typeRef));
          } catch (Exception e) {
            logger.error("Error getting collection from cache: {}", e.getMessage(), e);
            return Optional.empty();
          }
        });
  }

  /**
   * Put a value in cache
   *
   * @param cacheName The name of the cache
   * @param key The key to put
   * @param value The value to put
   */
  public <T> void putInCache(String cacheName, String key, T value) {
    LoggingUtil.logOperationTime(logger, "put in cache: " + cacheName + ":" + key, () -> {
      try {
        String fullKey = generateCacheKey(cacheName, key);
        String jsonValue = objectMapper.writeValueAsString(value);

        try (Jedis jedis = jedisPool.getResource()) {
          jedis.setex(fullKey, (int) DEFAULT_CACHE_TTL, jsonValue);
        }

        logger.debug("Stored in cache: {}", fullKey);
        return null;
      } catch (Exception e) {
        logger.error("Error putting in cache: {}", e.getMessage(), e);
        return null;
      }
    });
  }

  /**
   * Put a value in cache with a custom TTL
   *
   * @param cacheName The name of the cache
   * @param key The key to put
   * @param value The value to put
   * @param ttlSeconds The TTL in seconds
   */
  public <T> void putInCacheWithTTL(String cacheName, String key, T value, long ttlSeconds) {
    LoggingUtil.logOperationTime(logger, "put in cache with TTL: " + cacheName + ":" + key, () -> {
      try {
        String fullKey = generateCacheKey(cacheName, key);
        String jsonValue = objectMapper.writeValueAsString(value);

        try (Jedis jedis = jedisPool.getResource()) {
          jedis.setex(fullKey, (int) ttlSeconds, jsonValue);
        }

        logger.debug("Stored in cache with TTL {}s: {}", ttlSeconds, fullKey);
        return null;
      } catch (Exception e) {
        logger.error("Error putting in cache with TTL: {}", e.getMessage(), e);
        return null;
      }
    });
  }

  /**
   * Delete a value from cache
   *
   * @param cacheName The name of the cache
   * @param key The key to delete
   */
  public void deleteFromCache(String cacheName, String key) {
    LoggingUtil.logOperationTime(logger, "delete from cache: " + cacheName + ":" + key, () -> {
      try {
        String fullKey = generateCacheKey(cacheName, key);
        Long result;

        try (Jedis jedis = jedisPool.getResource()) {
          result = jedis.del(fullKey);
        }

        if (result > 0) {
          logger.debug("Deleted from cache: {}", fullKey);
        } else {
          logger.debug("Key not found in cache: {}", fullKey);
        }
        return null;
      } catch (Exception e) {
        logger.error("Error deleting from cache: {}", e.getMessage(), e);
        return null;
      }
    });
  }

  /**
   * Generate a cache key
   *
   * @param cacheName The name of the cache
   * @param key The key
   * @return The full cache key
   */
  public static String generateCacheKey(String cacheName, String key) {
    return cacheName + ":" + key;
  }

  /**
   * Generate a cache key with tenant ID
   *
   * @param cacheName The name of the cache
   * @param tenantId The tenant ID
   * @return The full cache key
   */
  public static String generateCacheKey(String cacheName, Integer tenantId) {
    return cacheName + ":tenant:" + tenantId;
  }

  public <T> void putInCacheWithExpiry(String cacheName, String key, T value, long timeout,
      TimeUnit unit) {
    LoggingUtil.logOperationTime(logger, "put in cache with expiry: " + cacheName + ":" + key,
        () -> {
          try {
            String fullKey = generateCacheKey(cacheName, key);
            String jsonValue = objectMapper.writeValueAsString(value);
            int seconds = (int) unit.toSeconds(timeout);

            try (Jedis jedis = jedisPool.getResource()) {
              jedis.setex(fullKey, seconds, jsonValue);
            }

            logger.debug("Stored in cache with expiry {} {}: {}", timeout, unit, fullKey);
            return null;
          } catch (Exception e) {
            logger.error("Error putting in cache with expiry: {}", e.getMessage(), e);
            return null;
          }
        });
  }

  public <T> T getOrCompute(String cacheName, String key, Supplier<T> supplier, Class<T> type) {
    Optional<T> cachedValue = getFromCache(cacheName, key, type);

    if (!cachedValue.isPresent()) {
      T value = supplier.get();
      if (value != null) {
        putInCache(cacheName, key, value);
      }
      return value;
    }

    return cachedValue.get();
  }

  public void evictFromCache(String cacheName, String key) {
    LoggingUtil.logOperationTime(logger, "evict from cache: " + cacheName + ":" + key, () -> {
      try {
        String fullKey = generateCacheKey(cacheName, key);
        Long result;

        try (Jedis jedis = jedisPool.getResource()) {
          result = jedis.del(fullKey);
        }

        if (result > 0) {
          logger.debug("Evicted from cache: {}", fullKey);
        } else {
          logger.debug("Key not found in cache: {}", fullKey);
        }
        return null;
      } catch (Exception e) {
        logger.error("Error evicting from cache: {}", e.getMessage(), e);
        return null;
      }
    });
  }

  public void clearCache(String cacheName) {
    LoggingUtil.logOperationTime(logger, "clear cache: " + cacheName, () -> {
      try {
        String pattern = generateCacheKey(cacheName, "*");
        Set<String> keys;

        try (Jedis jedis = jedisPool.getResource()) {
          keys = jedis.keys(pattern);
        }

        if (keys != null && !keys.isEmpty()) {
          try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(keys.toArray(new String[0]));
          }
          logger.debug("Cleared cache: {}", cacheName);
        }
        return null;
      } catch (Exception e) {
        logger.error("Error clearing cache: {}", e.getMessage(), e);
        return null;
      }
    });
  }

  public void clearAllCaches() {
    LoggingUtil.logOperationTime(logger, "clear all caches", () -> {
      try {
        try (Jedis jedis = jedisPool.getResource()) {
          jedis.flushDB();
        }
        logger.info("All caches cleared");
        return null;
      } catch (Exception e) {
        logger.error("Error clearing all caches: {}", e.getMessage(), e);
        return null;
      }
    });
  }
} 