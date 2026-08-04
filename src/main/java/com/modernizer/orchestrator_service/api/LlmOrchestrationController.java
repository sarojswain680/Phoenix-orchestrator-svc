package com.modernizer.orchestrator_service.api;

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

  public LlmOrchestrationController(LlmOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  /**
   * Accepts optional provider query param and either a raw prompt string OR a raw JSON payload.
   *
   * <p>URL: POST /api/v1/chat (completely implicit)
   *
   * <p>URL: POST /api/v1/chat?provider=openai (explicit routing)
   */
  @PostMapping("/chat")
  public ResponseEntity<String> chat(
      @RequestParam(required = false) String provider, @RequestBody String body)
      throws IOException, InterruptedException {

    if (body == null || body.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request body cannot be empty.");
    }

    String formattedPayload = body.trim();

    // Auto-Framing: If the input is plain text and NOT a JSON block, wrap it in OpenAI format
    if (!formattedPayload.startsWith("{")) {
      formattedPayload =
          """
          {
            "messages": [
              {
                "role": "user",
                "content": "%s"
              }
            ]
          }
          """
              .formatted(formattedPayload.replace("\"", "\\\"").replace("\n", "\\n"));
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
