package com.modernizer.orchestrator_service.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "external.gemini", name = "api-key")
public class GeminiClient implements LlmService {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String apiKey;
  private final String baseUrl;

  public GeminiClient(
      HttpClient httpClient,
      @Value("${external.gemini.api-key}") String apiKey,
      @Value("${external.gemini.base-url}") String baseUrl) {
    this.httpClient = httpClient;
    this.apiKey = apiKey;
    this.baseUrl =
        baseUrl != null && baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;
  }

  @Override
  public String chatCompletions(String bodyJson) throws IOException, InterruptedException {
    String model = extractModel(bodyJson, "gemini-2.5-pro");
    String translatedPayload = translateToGeminiPayload(bodyJson);
    String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(translatedPayload))
            .build();

    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 400) {
      throw new IOException("Gemini API error: " + resp.statusCode() + " - " + resp.body());
    }

    JsonNode root = objectMapper.readTree(resp.body());
    JsonNode candidates = root.path("candidates");
    if (candidates.isArray() && !candidates.isEmpty()) {
      JsonNode parts = candidates.get(0).path("content").path("parts");
      if (parts.isArray() && !parts.isEmpty()) {
        return parts.get(0).path("text").asText();
      }
    }
    throw new IOException("Failed to parse Gemini response: " + resp.body());
  }

  private String extractModel(String openAiJson, String defaultModel) {
    try {
      JsonNode root = objectMapper.readTree(openAiJson);
      if (root.has("model")) {
        return root.get("model").asText();
      }
    } catch (Exception ignored) {
      // ignore
    }
    return defaultModel;
  }

  private String translateToGeminiPayload(String openAiJson) throws IOException {
    JsonNode root = objectMapper.readTree(openAiJson);
    ObjectNode geminiRoot = objectMapper.createObjectNode();

    ArrayNode openAiMessages = (ArrayNode) root.path("messages");
    ArrayNode contents = objectMapper.createArrayNode();
    StringBuilder systemPrompt = new StringBuilder();

    for (JsonNode msg : openAiMessages) {
      String role = msg.path("role").asText();
      String content = msg.path("content").asText();

      if ("system".equalsIgnoreCase(role)) {
        if (!systemPrompt.isEmpty()) {
          systemPrompt.append("\n");
        }
        systemPrompt.append(content);
      } else {
        ObjectNode geminiContent = objectMapper.createObjectNode();
        geminiContent.put("role", "assistant".equalsIgnoreCase(role) ? "model" : "user");

        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", content);
        parts.add(part);

        geminiContent.set("parts", parts);
        contents.add(geminiContent);
      }
    }
    geminiRoot.set("contents", contents);

    if (!systemPrompt.isEmpty()) {
      ObjectNode systemInstruction = objectMapper.createObjectNode();
      ArrayNode parts = objectMapper.createArrayNode();
      ObjectNode part = objectMapper.createObjectNode();
      part.put("text", systemPrompt.toString());
      parts.add(part);
      systemInstruction.set("parts", parts);
      geminiRoot.set("systemInstruction", systemInstruction);
    }
    return objectMapper.writeValueAsString(geminiRoot);
  }

  @Override
  public String getProviderName() {
    return "gemini";
  }

  @Override
  public boolean isActive() {
    return apiKey != null && !apiKey.isBlank();
  }
}
