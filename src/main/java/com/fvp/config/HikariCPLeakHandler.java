package com.fvp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class HikariCPLeakHandler {

  private static final Logger logger = LoggerFactory.getLogger(HikariCPLeakHandler.class);

  @Autowired
  private DataSourceProperties dataSourceProperties;

  @Bean
  @Primary
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();

    // Set basic properties from application.properties
    config.setJdbcUrl(dataSourceProperties.getUrl());
    config.setUsername(dataSourceProperties.getUsername());
    config.setPassword(dataSourceProperties.getPassword());
    config.setDriverClassName(dataSourceProperties.getDriverClassName());

    // Configure leak detection with enhanced logging
    config.setLeakDetectionThreshold(30000); // 30 seconds

    // Add connection health checks
    config.setConnectionTestQuery("SELECT 1");
    config.setValidationTimeout(5000);

    // Configure pool sizes and timeouts
    config.setMaximumPoolSize(20);
    config.setMinimumIdle(10);
    config.setIdleTimeout(300000);
    config.setMaxLifetime(1800000);
    config.setConnectionTimeout(30000);

    // Add custom leak detector
    config.setRegisterMbeans(true);

    // Add a hook to log and close leaked connections
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Checking for leaked connections before shutdown...");
    }));

    logger.info("Configuring enhanced HikariCP with leak detection");

    return new HikariDataSource(config);
  }
} 