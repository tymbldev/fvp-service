package com.fvp.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
    cacheManager.setCacheNames(java.util.Arrays.asList(
        "categoryShardCache",     // For category to shard mapping
        "linkCategoryCache",      // For link category entities
        "categoryCountCache",     // For category count operations
        "categoriesCache"         // For storing lists of categories
    ));
    return cacheManager;
  }
} 