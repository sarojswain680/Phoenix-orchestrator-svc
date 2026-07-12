package com.modernizer.orchestrator_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OrchestratorServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrchestratorServiceApplication.class, args);
  }
}
