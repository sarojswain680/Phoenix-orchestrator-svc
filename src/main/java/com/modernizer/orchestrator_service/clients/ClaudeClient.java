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
@ConditionalOnProperty(prefix = "external.claude", name = "api-key")
public class ClaudeClient implements LlmService {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String apiKey;
  private final String baseUrl;

  public ClaudeClient(
      HttpClient httpClient,
      @Value("${external.claude.api-key}") String apiKey,
      @Value("${external.claude.base-url:https://api.anthropic.com}") String baseUrl) {
    this.httpClient = httpClient;
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
  }

  @Override
  public String chatCompletions(String bodyJson) throws IOException, InterruptedException {
    String translatedPayload = translateToClaudePayload(bodyJson);
    String url = baseUrl + "/v1/messages";

    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(translatedPayload))
            .build();

    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 400) {
      throw new IOException("Claude API error: " + resp.statusCode() + " - " + resp.body());
    }

    JsonNode root = objectMapper.readTree(resp.body());
    JsonNode contentNode = root.path("content");
    if (contentNode.isArray() && !contentNode.isEmpty()) {
      return contentNode.get(0).path("text").asText();
    }
    throw new IOException("Failed to parse Claude response: " + resp.body());
  }

  private String translateToClaudePayload(String openAiJson) throws IOException {
    JsonNode root = objectMapper.readTree(openAiJson);
    ObjectNode claudeRoot = objectMapper.createObjectNode();

    claudeRoot.put("model", root.path("model").asText("claude-3-5-sonnet-latest"));
    claudeRoot.put("max_tokens", root.path("max_tokens").asInt(4096));

    if (root.has("temperature")) claudeRoot.set("temperature", root.get("temperature"));
    if (root.has("top_p")) claudeRoot.set("top_p", root.get("top_p"));

    ArrayNode openAiMessages = (ArrayNode) root.path("messages");
    ArrayNode claudeMessages = objectMapper.createArrayNode();
    StringBuilder systemPrompt = new StringBuilder();

    for (JsonNode msg : openAiMessages) {
      String role = msg.path("role").asText();
      String content = msg.path("content").asText();

      if ("system".equalsIgnoreCase(role)) {
        if (!systemPrompt.isEmpty()) systemPrompt.append("\n");
        systemPrompt.append(content);
      } else {
        ObjectNode claudeMsg = objectMapper.createObjectNode();
        claudeMsg.put("role", "assistant".equalsIgnoreCase(role) ? "assistant" : "user");
        claudeMsg.put("content", content);
        claudeMessages.add(claudeMsg);
      }
    }

    if (!systemPrompt.isEmpty()) {
      claudeRoot.put("system", systemPrompt.toString());
    }
    claudeRoot.set("messages", claudeMessages);
    return objectMapper.writeValueAsString(claudeRoot);
  }

  @Override
  public String getProviderName() {
    return "claude";
  }
}
