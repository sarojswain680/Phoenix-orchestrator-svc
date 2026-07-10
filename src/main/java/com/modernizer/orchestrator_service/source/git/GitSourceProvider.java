package com.modernizer.orchestrator_service.source.git;

import com.modernizer.orchestrator_service.source.SourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.UUID;

@Component
@Slf4j
public class GitSourceProvider implements SourceProvider {

    @Override
    public String supports() {
        return "GIT";
    }

    @Override
    public Path fetch(String uri, String ref, UUID jobId) {
        log.info("[GIT] clone uri={} ref={} jobId={}", uri, ref, jobId);
        // TODO: GitUrlValidator (SSRF guard) + shallow clone @ pinned SHA
        return Path.of(System.getProperty("java.io.tmpdir"), "workspace", jobId.toString());
    }
}