package com.modernizer.orchestrator_service.source.git;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

/**
 * Shallow-clones a GitHub repository (depth = 1) and pins the working tree to an EXACT commit SHA.
 *
 * <p>SECURITY / CORRECTNESS:
 *
 * <ul>
 *   <li>depth=1 minimizes data pulled and avoids full-history exfiltration (AC #2).
 *   <li>We fetch the specific SHA and hard-reset to it — never trust a moving branch tip.
 *   <li>URL + SHA are validated up front (SSRF + pinning-bypass guards).
 *   <li>Auth uses a short-lived installation token, not a PAT.
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GitHubShallowCloner {

  private final GitUrlValidator validator;
  private final GitHubAppTokenProvider tokenProvider;

  /**
   * @param repoUrl e.g. {@code https://github.com/tenant-org/repo.git}
   * @param commitSha the exact 40-char SHA to modernize
   * @param workDir dedicated, empty, sandboxed directory
   * @return the workDir containing exactly the requested commit's tree
   */
  public Path cloneAtSha(String repoUrl, String commitSha, Path workDir) throws Exception {
    validator.validateRepoUrl(repoUrl);
    validator.validateSha(commitSha);

    String token = tokenProvider.getInstallationToken();
    var creds = new UsernamePasswordCredentialsProvider("x-access-token", token);

    log.info("[GIT] shallow-clone depth=1 sha={} dir={}", commitSha, workDir);

    try (Git git =
        Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(workDir.toFile())
            .setCredentialsProvider(creds)
            .setDepth(1) // ── AC #2: shallow ──
            .setCloneAllBranches(false)
            .setNoCheckout(true) // we checkout the exact SHA ourselves
            .call()) {

      // Fetch the exact SHA shallowly (covers case where SHA != default-branch tip).
      git.fetch()
          .setCredentialsProvider(creds)
          .setRefSpecs(new RefSpec(commitSha))
          .setDepth(1)
          .call();

      // Pin working tree to the exact commit.
      git.reset().setMode(ResetType.HARD).setRef(commitSha).call();

      log.info("[GIT] pinned workspace to sha={}", commitSha);
    }
    return workDir;
  }
}
