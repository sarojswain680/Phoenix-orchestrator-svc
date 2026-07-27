package com.modernizer.orchestrator_service.source.git;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernizer.orchestrator_service.config.GitHubAppProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mints short-lived GitHub App installation tokens.
 *
 * <p>Flow: sign an app JWT (RS256, 10-min max) with the App private key → exchange it at {@code
 * /app/installations/{id}/access_tokens} for an installation token (~1h). We use the installation
 * token as the git password ("x-access-token"). We never use a long-lived PAT.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GitHubAppTokenProvider {

  private final GitHubAppProperties props;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /**
   * @return a short-lived installation token usable as the git HTTPS password.
   */
  public String getInstallationToken() {
    try {
      String appJwt = buildAppJwt();
      return exchangeForInstallationToken(appJwt);
    } catch (Exception e) {
      // Do not leak key material or token internals into logs.
      throw new IllegalStateException("Failed to obtain GitHub App installation token", e);
    }
  }

  private String buildAppJwt() throws IOException {
    RSAPrivateKey key = loadPrivateKey(props.getPrivateKeyPath());
    Instant now = Instant.now();
    // Clock-skew guard: iat backdated 60s; exp within GitHub's 10-min hard limit.
    return JWT.create()
        .withIssuer(props.getId())
        .withIssuedAt(now.minusSeconds(60))
        .withExpiresAt(now.plus(Duration.ofMinutes(9)))
        .sign(Algorithm.RSA256(null, key));
  }

  private String exchangeForInstallationToken(String appJwt)
      throws IOException, InterruptedException {
    String url =
        props.getApiBaseUrl()
            + "/app/installations/"
            + props.getInstallationId()
            + "/access_tokens";

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + appJwt)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 201) {
      throw new IllegalStateException(
          "GitHub token exchange failed: HTTP " + response.statusCode());
    }

    JsonNode node = objectMapper.readTree(response.body());
    JsonNode token = node.get("token");
    if (token == null || token.asText().isBlank()) {
      throw new IllegalStateException("GitHub token exchange returned no token field");
    }
    log.debug(
        "Obtained GitHub installation token (expires_at={})", node.path("expires_at").asText());
    return token.asText();
  }

  /** Loads a PKCS#8 PEM RSA private key from disk. */
  private RSAPrivateKey loadPrivateKey(String pemPath) throws IOException {
    if (pemPath == null || pemPath.isBlank()) {
      throw new IllegalStateException("GITHUB_APP_PRIVATE_KEY_PATH is not configured");
    }
    String pem = Files.readString(Path.of(pemPath), StandardCharsets.UTF_8);
    String base64 =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    if (base64.isBlank()) {
      throw new IllegalStateException(
          "GitHub App private key at " + pemPath + " is empty or not PKCS#8 PEM");
    }
    byte[] der = Base64.getDecoder().decode(base64);
    try {
      var spec = new PKCS8EncodedKeySpec(der);
      return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid GitHub App private key (expected PKCS#8 PEM)", e);
    }
  }
}
