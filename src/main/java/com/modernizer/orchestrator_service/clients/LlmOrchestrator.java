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
    LlmService service = clients.get(provider.toLowerCase());
    if (service == null) {
      throw new IllegalArgumentException(
          "Provider '"
              + provider
              + "' is not supported or not configured. "
              + "Available providers: "
              + clients.keySet());
    }
    return service.chatCompletions(bodyJson);
  }
}
