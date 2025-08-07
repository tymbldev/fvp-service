package com.fvp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import com.fvp.util.LoggingUtil;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class RedisService {

  private static final Logger logger = LoggingUtil.getLogger(RedisService.class);
  private final JedisPool jedisPool;
  private final ObjectMapper objectMapper;

  public RedisService(JedisPool jedisPool, ObjectMapper objectMapper) {
    this.jedisPool = jedisPool;
    this.objectMapper = objectMapper;
  }

  public void setValue(String key, Object value) {
    try (Jedis jedis = jedisPool.getResource()) {
      String jsonValue = objectMapper.writeValueAsString(value);
      jedis.set(key, jsonValue);
    } catch (JsonProcessingException e) {
      logger.error("Error serializing object for key {}: {}", key, e.getMessage());
    }
  }

  public void setValueWithExpiry(String key, Object value, long timeout, TimeUnit unit) {
    try (Jedis jedis = jedisPool.getResource()) {
      String jsonValue = objectMapper.writeValueAsString(value);
      int seconds = (int) unit.toSeconds(timeout);
      jedis.setex(key, seconds, jsonValue);
    } catch (JsonProcessingException e) {
      logger.error("Error serializing object for key {}: {}", key, e.getMessage());
    }
  }

  public <T> T getValue(String key, Class<T> type) {
    try (Jedis jedis = jedisPool.getResource()) {
      String value = jedis.get(key);
      if (value == null) {
        return null;
      }
      return objectMapper.readValue(value, type);
    } catch (Exception e) {
      logger.error("Error deserializing object for key {}: {}", key, e.getMessage());
      return null;
    }
  }

  public void deleteValue(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.del(key);
    }
  }

  public boolean hasKey(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.exists(key);
    }
  }

  public void setHashValue(String key, String hashKey, Object value) {
    try (Jedis jedis = jedisPool.getResource()) {
      String jsonValue = objectMapper.writeValueAsString(value);
      jedis.hset(key, hashKey, jsonValue);
    } catch (JsonProcessingException e) {
      logger.error("Error serializing object for hash key {}: {}", hashKey, e.getMessage());
    }
  }

  public <T> T getHashValue(String key, String hashKey, Class<T> type) {
    try (Jedis jedis = jedisPool.getResource()) {
      String value = jedis.hget(key, hashKey);
      if (value == null) {
        return null;
      }
      return objectMapper.readValue(value, type);
    } catch (Exception e) {
      logger.error("Error deserializing object for hash key {}: {}", hashKey, e.getMessage());
      return null;
    }
  }

  public void deleteHashValue(String key, String hashKey) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.hdel(key, hashKey);
    }
  }
} 