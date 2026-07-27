package com.modernizer.orchestrator_service.source.git;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates repository URLs and commit SHAs before any network/git operation.
 *
 * <p>SECURITY: prevents SSRF (cloning internal hosts), protocol smuggling, and pinning bypass via
 * ambiguous short SHAs. Reject-by-default: only https github.com URLs and full 40-char SHAs pass.
 */
@Component
public class GitUrlValidator {

  // https://github.com/<owner>/<repo>[.git] — no userinfo, no ports, no other hosts.
  private static final Pattern GITHUB_HTTPS =
      Pattern.compile("^https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+?(\\.git)?$");

  // Full SHA-1 (40 hex). Reject short SHAs — they can be ambiguous across refs.
  private static final Pattern FULL_SHA = Pattern.compile("^[0-9a-fA-F]{40}$");

  public void validateRepoUrl(String url) {
    if (url == null || !GITHUB_HTTPS.matcher(url).matches()) {
      throw new SecurityException("Rejected repository URL (must be https github.com): " + url);
    }
    // Defense-in-depth: explicitly reject embedded credentials / traversal.
    if (url.contains("@") || url.contains("..")) {
      throw new SecurityException("Rejected repository URL (illegal characters): " + url);
    }
  }

  public void validateSha(String sha) {
    if (sha == null || !FULL_SHA.matcher(sha).matches()) {
      throw new SecurityException(
          "Rejected ref: a full 40-character commit SHA is required (got: " + sha + ")");
    }
  }
}
