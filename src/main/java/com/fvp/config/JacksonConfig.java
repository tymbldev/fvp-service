package com.fvp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Add Page deserializer
        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(Page.class, new PageDeserializer());
        objectMapper.registerModule(pageModule);
        
        // Add Java Time support
        objectMapper.registerModule(new JavaTimeModule());
        
        return objectMapper;
    }
} 