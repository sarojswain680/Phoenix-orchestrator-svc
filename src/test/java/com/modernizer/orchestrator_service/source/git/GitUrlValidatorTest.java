package com.modernizer.orchestrator_service.source.git;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GitUrlValidatorTest {

  private final GitUrlValidator validator = new GitUrlValidator();

  @Test
  void acceptsValidGitHubHttpsUrl() {
    assertThatCode(() -> validator.validateRepoUrl("https://github.com/octocat/hello.git"))
        .doesNotThrowAnyException();
    assertThatCode(() -> validator.validateRepoUrl("https://github.com/octocat/hello"))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://github.com/a/b", // not https  → SSRF/plaintext
        "https://gitlab.com/a/b", // wrong host → SSRF
        "https://github.com/a", // missing repo
        "https://user:pass@github.com/a/b", // embedded creds
        "https://github.com/a/../../etc", // traversal
        "ssh://git@github.com/a/b", // wrong protocol
        "https://internal-host/a/b" // internal SSRF target
      })
  void rejectsMaliciousOrNonGitHubUrls(String url) {
    assertThatThrownBy(() -> validator.validateRepoUrl(url)).isInstanceOf(SecurityException.class);
  }

  @Test
  void acceptsFull40CharSha() {
    assertThatCode(() -> validator.validateSha("0123456789abcdef0123456789abcdef01234567"))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "abc123", // short SHA → ambiguity/pinning bypass
        "main", // branch name, not a SHA
        "0123456789abcdef0123456789abcdef0123456", // 39 chars
        "0123456789abcdef0123456789abcdef012345678", // 41 chars
        "ZZZ3456789abcdef0123456789abcdef01234567" // non-hex
      })
  void rejectsNonExactSha(String ref) {
    assertThatThrownBy(() -> validator.validateSha(ref)).isInstanceOf(SecurityException.class);
  }
}
