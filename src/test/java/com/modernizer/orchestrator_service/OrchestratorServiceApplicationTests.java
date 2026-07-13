package com.modernizer.orchestrator_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Disabled until local Docker/Testcontainers are configured in CI")
class OrchestratorServiceApplicationTests {

  @Test
  void contextLoads() {
    // This test ensures that the Spring application context loads successfully.
  }
}
