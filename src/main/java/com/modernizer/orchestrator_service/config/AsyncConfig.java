package com.modernizer.orchestrator_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("pipelineExecutor")
    public ExecutorService pipelineExecutor() {
        // Java 21 virtual threads — ideal for I/O-heavy parsing
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}