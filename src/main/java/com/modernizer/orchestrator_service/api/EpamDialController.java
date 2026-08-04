package com.modernizer.orchestrator_service.api;

import com.modernizer.orchestrator_service.clients.EpamDialClient;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/epam/dial")
public class EpamDialController {

  private static final Logger log = LoggerFactory.getLogger(EpamDialController.class);
  private final EpamDialClient client;

  public EpamDialController(EpamDialClient client) {
    this.client = client;
  }

  @PostMapping(
      value = "/chat",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE) // Changed to plain text response
  public ResponseEntity<String> chat(@RequestBody String bodyJson) {
    try {
      String response = client.chatCompletions(bodyJson);
      return ResponseEntity.ok(response); // Will return the clean, readable text!
    } catch (IOException e) {
      log.error("EPAM DIAL API call failed", e);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("EPAM DIAL API call failed");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("EPAM DIAL call interrupted", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("request interrupted");
    }
  }
}
