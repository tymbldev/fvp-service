package com.fvp.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database configuration with autocommit enabled through Spring properties.
 */
@Configuration
public class DatabaseConfig {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

  @Autowired
  private Environment env;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private DataSourceProperties dataSourceProperties;

  @Bean
  public JdbcTemplate jdbcTemplate() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    try {
      String url = dataSourceProperties.getUrl();
      String username = dataSourceProperties.getUsername();
      logger.info("Configured database connection: {}", url);

      // Test database connection
      String dbVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
      logger.info("Successfully connected to database. MySQL version: {}", dbVersion);

      // Log autocommit status
      Boolean autoCommit = jdbcTemplate.getDataSource().getConnection().getAutoCommit();
      logger.info("Database connection autoCommit setting: {}", autoCommit);
      logger.info("Auto-commit enabled via Spring Data JPA configuration");
      logger.info("Using Hikari connection pool with auto-commit=true");
    } catch (Exception e) {
      logger.error("Error initializing database connection", e);
    }
    return jdbcTemplate;
  }

  /**
   * @return The database name from configuration properties
   */
  public String getDatabaseName() {
    return env.getProperty("database.name", "fvp_test");
  }
} 