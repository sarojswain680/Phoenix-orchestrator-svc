package com.modernizer.orchestrator_service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.modernizer.orchestrator_service.clients.LlmOrchestrator;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller that routes raw chat completion payloads to your LlmOrchestrator. */
@RestController
@RequestMapping("/api/v1")
public class LlmOrchestrationController {

  private final LlmOrchestrator orchestrator;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public LlmOrchestrationController(LlmOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  /**
   * Accepts optional provider query param and either a raw prompt string OR a raw JSON payload.
   *
   * <p>URL: POST /api/v1/chat
   *
   * <p>URL: POST /api/v1/chat?provider=openai
   */
  @PostMapping("/chat")
  public ResponseEntity<String> chat(
      @RequestParam(required = false) String provider, @RequestBody String body)
      throws IOException, InterruptedException {

    if (body == null || body.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request body cannot be empty.");
    }

    String formattedPayload = body.trim();

    // Case A: Input is raw plain text prompt
    if (!formattedPayload.startsWith("{")) {
      formattedPayload =
          """
          {
            "model": "gpt-4o",
            "messages": [
              {
                "role": "user",
                "content": "%s"
              }
            ]
          }
          """
              .formatted(formattedPayload.replace("\"", "\\\"").replace("\n", "\\n"));
    } else {
      // Case B: Input is JSON but might be missing the "model" parameter
      try {
        JsonNode jsonNode = objectMapper.readTree(formattedPayload);
        if (!jsonNode.has("model") || jsonNode.path("model").asText().isBlank()) {
          ((ObjectNode) jsonNode).put("model", "gpt-4o");
          formattedPayload = objectMapper.writeValueAsString(jsonNode);
        }
      } catch (Exception ignored) {
        // Fall back to original formattedPayload if parsing fails
      }
    }

    String responseContent = orchestrator.routeChat(provider, formattedPayload);
    return ResponseEntity.ok(responseContent);
  }

  /** Centralized local error handler for invalid providers or configurations. */
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<Map<String, String>> handleClientErrors(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "Invalid Request", "message", ex.getMessage()));
  }

  /** Centralized exception handler for upstream network/parsing errors. */
  @ExceptionHandler({IOException.class, InterruptedException.class})
  public ResponseEntity<Map<String, String>> handleNetworkErrors(Exception ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(Map.of("error", "Upstream Service Failure", "message", ex.getMessage()));
  }
}
