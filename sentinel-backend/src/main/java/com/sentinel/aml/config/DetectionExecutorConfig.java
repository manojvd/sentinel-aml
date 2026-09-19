package com.sentinel.aml.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DetectionExecutorConfig {

    @Bean
    public ExecutorService detectionExecutor(
            @Value("${sentinel.detection.thread-pool-size}") int threadPoolSize) {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}
