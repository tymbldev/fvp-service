package com.fvp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    
    @Bean
    public ExecutorService executorService() {
        // Create a fixed thread pool with 5 threads
        return Executors.newFixedThreadPool(5);
    }
} 