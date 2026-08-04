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
   * Accepts the LLM provider in the query params and routes the raw JSON body to the orchestrator.
   *
   * <p>URL: POST /api/v1/chat?provider=openai
   */
  @PostMapping("/chat")
  public ResponseEntity<String> chat(
      @RequestParam(defaultValue = "openai") String provider, @RequestBody String bodyJson)
      throws IOException, InterruptedException {

    if (bodyJson == null || bodyJson.isBlank()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Request body (OpenAI-compatible payload) cannot be empty.");
    }

    String responseContent = orchestrator.routeChat(provider, bodyJson);
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
