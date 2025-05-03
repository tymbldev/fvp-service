package com.fvp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

  @Value("${spring.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.redis.port:6379}")
  private int redisPort;

  @Value("${spring.redis.password:}")
  private String redisPassword;

  @Value("${spring.redis.timeout:2000}")
  private int timeout;

  @Value("${spring.redis.jedis.pool.max-active:8}")
  private int maxActive;

  @Value("${spring.redis.jedis.pool.max-idle:8}")
  private int maxIdle;

  @Value("${spring.redis.jedis.pool.min-idle:0}")
  private int minIdle;

  @Value("${spring.redis.jedis.pool.max-wait:-1}")
  private long maxWaitMillis;

  @Bean
  public JedisPool jedisPool() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(maxActive);
    poolConfig.setMaxIdle(maxIdle);
    poolConfig.setMinIdle(minIdle);
    poolConfig.setMaxWaitMillis(maxWaitMillis);

    if (redisPassword != null && !redisPassword.isEmpty()) {
      return new JedisPool(poolConfig, redisHost, redisPort, timeout, redisPassword);
    } else {
      return new JedisPool(poolConfig, redisHost, redisPort, timeout);
    }
  }
} 