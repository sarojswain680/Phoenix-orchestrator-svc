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
@ConditionalOnProperty(prefix = "external.epam.dial", name = "api-key")
public class EpamDialClient implements LlmService {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String apiKey;
  private final String baseUrl;
  private final String apiVersion = "2023-12-01-preview";

  public EpamDialClient(
      HttpClient httpClient,
      @Value("${external.epam.dial.api-key}") String apiKey,
      @Value("${external.epam.dial.base-url}") String baseUrl) {
    this.httpClient = httpClient;
    this.apiKey = apiKey;
    this.baseUrl =
        baseUrl != null && baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
  }

  @Override
  public String chatCompletions(String bodyJson) throws IOException, InterruptedException {
    String url = baseUrl + "/chat/completions?api-version=" + apiVersion;
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Api-Key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
            .build();

    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 400) {
      throw new IOException("EPAM DIAL API error: " + resp.statusCode() + " - " + resp.body());
    }

    JsonNode root = objectMapper.readTree(resp.body());
    JsonNode choices = root.path("choices");
    if (choices.isArray() && !choices.isEmpty()) {
      return choices.get(0).path("message").path("content").asText();
    }
    throw new IOException("Failed to parse EPAM DIAL response: " + resp.body());
  }

  @Override
  public String getProviderName() {
    return "epam-dial";
  }
}
