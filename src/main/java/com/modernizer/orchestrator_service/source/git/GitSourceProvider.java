package com.modernizer.orchestrator_service.source.git;

import com.modernizer.orchestrator_service.source.SourceProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * GIT source provider: prepares an isolated workspace and delegates to {@link GitHubShallowCloner}.
 *
 * <p>PR2 will insert Gitleaks (and optionally ClamAV) between clone and return.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GitSourceProvider implements SourceProvider {

  private final GitHubShallowCloner cloner;

  @Override
  public String supports() {
    return "GIT";
  }

  @Override
  public Path fetch(String uri, String ref, UUID jobId) {
    log.info("[GIT] fetch uri={} ref={} jobId={}", uri, ref, jobId);
    Path workspace = prepareWorkspace(jobId);
    try {
      // AC #2: ref MUST be an exact SHA in strict mode (validated inside the cloner).
      cloner.cloneAtSha(uri, ref, workspace);
      return workspace;
    } catch (SecurityException se) {
      // Validation failures (bad URL / non-SHA ref) are security events — do not retry.
      cleanupQuietly(workspace);
      throw se;
    } catch (Exception e) {
      cleanupQuietly(workspace);
      throw new IllegalStateException("Git clone failed for jobId=" + jobId, e);
    }
  }

  private Path prepareWorkspace(UUID jobId) {
    try {
      Path base = Path.of(System.getProperty("java.io.tmpdir"), "workspace", jobId.toString());
      Files.createDirectories(base);
      return base;
    } catch (IOException e) {
      throw new IllegalStateException("Could not create workspace for jobId=" + jobId, e);
    }
  }

  private void cleanupQuietly(Path dir) {
    try (var paths = Files.walk(dir)) {
      paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
    } catch (IOException ignored) {
      log.warn("[GIT] failed to clean workspace {}", dir);
    }
  }
}
