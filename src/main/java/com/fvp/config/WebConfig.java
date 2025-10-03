package com.fvp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering filters and other web-related beans
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired
  private AccessLoggingFilter accessLoggingFilter;

  /**
   * Register the access logging filter
   */
  @Bean
  public FilterRegistrationBean<AccessLoggingFilter> accessLoggingFilterRegistration() {
    FilterRegistrationBean<AccessLoggingFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(accessLoggingFilter);
    registration.addUrlPatterns("/*");
    registration.setName("accessLoggingFilter");
    registration.setOrder(1);
    return registration;
  }
}
