package com.fvp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "elasticsearch.sync")
@Data
public class ElasticsearchSyncConfig {

  /**
   * Whether Elasticsearch sync is enabled
   */
  private boolean enabled = true;

  /**
   * Batch size for processing records
   */
  private int batchSize = 100;

  /**
   * Thread pool size for background processing
   */
  private int threadPoolSize = 20;
} 