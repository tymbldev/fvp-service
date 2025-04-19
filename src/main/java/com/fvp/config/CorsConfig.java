package com.fvp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:*}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Set allowed origins
        if (!"*".equals(allowedOrigins)) {
            for (String origin : allowedOrigins.split(",")) {
                config.addAllowedOrigin(origin.trim());
            }
        } else {
            config.addAllowedOrigin("*");
        }
        
        // Set allowed methods
        if (!"*".equals(allowedMethods)) {
            for (String method : allowedMethods.split(",")) {
                config.addAllowedMethod(method.trim());
            }
        } else {
            config.addAllowedMethod("*");
        }
        
        // Set allowed headers
        if (!"*".equals(allowedHeaders)) {
            for (String header : allowedHeaders.split(",")) {
                config.addAllowedHeader(header.trim());
            }
        } else {
            config.addAllowedHeader("*");
        }
        
        // Allow credentials
        config.setAllowCredentials(true);
        
        // Set max age
        config.setMaxAge(maxAge);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 