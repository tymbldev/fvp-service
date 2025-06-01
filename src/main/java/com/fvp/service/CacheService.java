package com.fvp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fvp.util.CacheBypassUtil;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class CacheService {

  public static final String CACHE_NAME_CATEGORIES = "categories";
  public static final String CACHE_NAME_LINKS = "links";
  public static final String CACHE_NAME_MODELS = "models";
  public static final String CACHE_NAME_TENANTS = "tenants";

  private static final long DEFAULT_CACHE_TTL = 24 * 60 * 60; // 24 hours in seconds

  private final JedisPool jedisPool;
  private final ObjectMapper objectMapper;
  private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

  @Autowired
  public CacheService(JedisPool jedisPool, ObjectMapper objectMapper) {
    this.jedisPool = jedisPool;
    this.objectMapper = objectMapper;
  }

  public <T> Optional<T> getFromCache(String cacheName, String key, Class<T> type) {
    if (CacheBypassUtil.isCacheBypass()) {
      logger.info("Bypassing the cache..");
      return Optional.empty();
    }

    try {
      String cacheKey = generateCacheKey(cacheName, key);
      String cachedValue;

      try (Jedis jedis = jedisPool.getResource()) {
        cachedValue = jedis.get(cacheKey);
      }

      if (cachedValue == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(cachedValue, type));
    } catch (Exception e) {
      logger.error("Error getting value from cache: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public <T> Optional<T> getCollectionFromCache(String cacheName, String key, TypeReference<T> typeReference) {
    if (CacheBypassUtil.isCacheBypass()) {
      logger.info("Bypassing the cache..");
      return Optional.empty();
    }

    try {
      String cacheKey = generateCacheKey(cacheName, key);
      String cachedValue;

      try (Jedis jedis = jedisPool.getResource()) {
        cachedValue = jedis.get(cacheKey);
      }

      if (cachedValue == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(cachedValue, typeReference));
    } catch (Exception e) {
      logger.error("Error getting collection from cache: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public <T> void putInCache(String cacheName, String key, T value) {
    try {
      String fullKey = generateCacheKey(cacheName, key);
      String jsonValue = objectMapper.writeValueAsString(value);

      try (Jedis jedis = jedisPool.getResource()) {
        jedis.setex(fullKey, (int) DEFAULT_CACHE_TTL, jsonValue);
      }
    } catch (Exception e) {
      // Silently handle exceptions
    }
  }

  public <T> void putInCacheWithTTL(String cacheName, String key, T value, long ttlSeconds) {
    try {
      String fullKey = generateCacheKey(cacheName, key);
      String jsonValue = objectMapper.writeValueAsString(value);

      try (Jedis jedis = jedisPool.getResource()) {
        jedis.setex(fullKey, (int) ttlSeconds, jsonValue);
      }
    } catch (Exception e) {
      // Silently handle exceptions
    }
  }

  public void deleteFromCache(String cacheName, String key) {
    try {
      String fullKey = generateCacheKey(cacheName, key);
      try (Jedis jedis = jedisPool.getResource()) {
        jedis.del(fullKey);
      }
    } catch (Exception e) {
      // Silently handle exceptions
    }
  }

  public static String generateCacheKey(String cacheName, String key) {
    return cacheName + ":" + key;
  }

  public static String generateCacheKey(String cacheName, Integer tenantId) {
    return cacheName + ":tenant:" + tenantId;
  }

  public <T> void putInCacheWithExpiry(String cacheName, String key, T value, long timeout,
      TimeUnit unit) {
    try {
      String fullKey = generateCacheKey(cacheName, key);
      String jsonValue = objectMapper.writeValueAsString(value);
      int seconds = (int) unit.toSeconds(timeout);

      try (Jedis jedis = jedisPool.getResource()) {
        jedis.setex(fullKey, seconds, jsonValue);
      }
    } catch (Exception e) {
      // Silently handle exceptions
    }
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
    try {
      String fullKey = generateCacheKey(cacheName, key);
      try (Jedis jedis = jedisPool.getResource()) {
        jedis.del(fullKey);
      }
    } catch (Exception e) {
      // Silently handle exceptions
    }
  }

  public void clearCache(String cacheName) {
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
      }
    } catch (Exception e) {
      // Silently handle exceptions
    }
  }

  public void clearAllCaches() {
    try {
      try (Jedis jedis = jedisPool.getResource()) {
        jedis.flushDB();
      }
    } catch (Exception e) {
      // Silently handle exceptions
    }
  }
} 