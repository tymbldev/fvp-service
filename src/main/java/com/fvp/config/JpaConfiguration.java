package com.fvp.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA Configuration to apply our custom dialect
 */
@Configuration
public class JpaConfiguration {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.dialect", CustomMySQLDialect.class.getName());
            hibernateProperties.putAll(properties);
        };
    }
} 