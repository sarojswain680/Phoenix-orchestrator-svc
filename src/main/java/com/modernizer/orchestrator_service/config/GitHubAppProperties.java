package com.modernizer.orchestrator_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub App credentials for minting short-lived installation tokens. Values are externalized (see
 * .env.example: GITHUB_APP_ID, GITHUB_APP_INSTALLATION_ID, GITHUB_APP_PRIVATE_KEY_PATH).
 */
@Component
@ConfigurationProperties(prefix = "github.app")
public class GitHubAppProperties {

  /** Numeric GitHub App ID. */
  private String id;

  /** Installation ID for the org/tenant this service acts on behalf of. */
  private String installationId;

  /** Filesystem path to the App private key (PEM, PKCS#8). */
  private String privateKeyPath;

  /** GitHub API base (override for GitHub Enterprise). */
  private String apiBaseUrl = "https://api.github.com";

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getInstallationId() {
    return installationId;
  }

  public void setInstallationId(String installationId) {
    this.installationId = installationId;
  }

  public String getPrivateKeyPath() {
    return privateKeyPath;
  }

  public void setPrivateKeyPath(String privateKeyPath) {
    this.privateKeyPath = privateKeyPath;
  }

  public String getApiBaseUrl() {
    return apiBaseUrl;
  }

  public void setApiBaseUrl(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }
}
