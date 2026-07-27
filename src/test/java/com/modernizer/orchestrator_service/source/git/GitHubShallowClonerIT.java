package com.modernizer.orchestrator_service.source.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * Integration test: performs a REAL shallow clone against a tiny public repo to prove depth=1 and
 * exact-SHA pinning (AC #2). Requires network. Run with: {@code ./gradlew test -PincludeTags=it}
 */
@Tag("it")
class GitHubShallowClonerIT {

  // A stable, tiny public repo + a known commit SHA on it.
  private static final String PUBLIC_REPO = "https://github.com/octocat/Hello-World.git";
  private static final String KNOWN_SHA = "7fd1a60b01f91b314f59955a4e4d4e80d8edf11d";

  @Test
  void clonesShallowAndPinsToExactSha(@TempDir Path tmp) throws Exception {
    // For public repos we don't need a token; stub the provider to return "".
    var tokenProvider = Mockito.mock(GitHubAppTokenProvider.class);
    when(tokenProvider.getInstallationToken()).thenReturn("");

    var cloner = new GitHubShallowCloner(new GitUrlValidator(), tokenProvider);
    Path ws = tmp.resolve("repo");

    cloner.cloneAtSha(PUBLIC_REPO, KNOWN_SHA, ws);

    // Assert HEAD is exactly the requested SHA.
    try (Git git = Git.open(ws.toFile())) {
      String head = git.getRepository().resolve("HEAD").getName();
      assertThat(head).isEqualTo(KNOWN_SHA);

      // Assert shallow: a .git/shallow marker exists (depth=1).
      assertThat(Files.exists(ws.resolve(".git/shallow"))).isTrue();
    }
  }
}
