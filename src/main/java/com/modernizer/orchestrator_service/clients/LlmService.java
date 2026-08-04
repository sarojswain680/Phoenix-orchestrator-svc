package com.modernizer.orchestrator_service.clients;

import java.io.IOException;

public interface LlmService {
  /** Calls the specific LLM endpoint and extracts the text response. */
  String chatCompletions(String bodyJson) throws IOException, InterruptedException;

  /** Identifies the provider name. */
  String getProviderName();

  /** Safety check: Checks if this service has a valid, non-blank API key. */
  default boolean isActive() {
    return true;
  }
}
