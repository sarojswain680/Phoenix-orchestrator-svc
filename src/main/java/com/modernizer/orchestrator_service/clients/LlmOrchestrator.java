package com.modernizer.orchestrator_service.clients;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LlmOrchestrator {
  private final Map<String, LlmService> clients;

  public LlmOrchestrator(List<LlmService> services) {
    this.clients =
        services.stream()
            .collect(
                Collectors.toMap(
                    service -> service.getProviderName().toLowerCase(), service -> service));
  }

  public String routeChat(String provider, String bodyJson)
      throws IOException, InterruptedException {

    String activeProvider = provider;

    // Auto-detect: If no provider was explicitly passed, use the first configured one
    if (activeProvider == null || activeProvider.isBlank()) {
      if (clients.isEmpty()) {
        throw new IllegalStateException(
            "No active LLM providers are configured in your .env file!");
      }
      activeProvider = clients.keySet().iterator().next();
    }

    LlmService service = clients.get(activeProvider.toLowerCase());
    if (service == null) {
      throw new IllegalArgumentException(
          "Provider '"
              + activeProvider
              + "' is not supported or not active. "
              + "Available active providers: "
              + clients.keySet());
    }
    return service.chatCompletions(bodyJson);
  }
}
