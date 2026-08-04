package com.modernizer.orchestrator_service.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "external.copilot", name = "api-key")
public class CopilotClient implements LlmService {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String apiKey;
  private final String baseUrl;

  public CopilotClient(
      HttpClient httpClient,
      @Value("${external.copilot.api-key}") String apiKey,
      @Value("${external.copilot.base-url:https://api.githubcopilot.com}") String baseUrl) {
    this.httpClient = httpClient;
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
  }

  @Override
  public String chatCompletions(String bodyJson) throws IOException, InterruptedException {
    String url = baseUrl + "/chat/completions";
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
            .build();

    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 400) {
      throw new IOException("GitHub Copilot error: " + resp.statusCode() + " - " + resp.body());
    }

    JsonNode root = objectMapper.readTree(resp.body());
    JsonNode choices = root.path("choices");
    if (choices.isArray() && !choices.isEmpty()) {
      return choices.get(0).path("message").path("content").asText();
    }
    throw new IOException("Failed to parse Copilot response: " + resp.body());
  }

  @Override
  public String getProviderName() {
    return "copilot";
  }

  @Override
  public boolean isActive() {
    return apiKey != null && !apiKey.isBlank();
  }
}
