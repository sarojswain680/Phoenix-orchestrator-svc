package com.modernizer.orchestrator_service.source.zip;

import com.modernizer.orchestrator_service.source.SourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.UUID;

@Component
@Slf4j
public class ZipSourceProvider implements SourceProvider {

    @Override
    public String supports() {
        return "ZIP";
    }

    @Override
    public Path fetch(String uri, String ref, UUID jobId) {
        log.info("[ZIP] fetch uri={} jobId={}", uri, jobId);
        // TODO: download from S3 + ZipSafetyValidator + extract + AV scan
        return Path.of(System.getProperty("java.io.tmpdir"), "workspace", jobId.toString());
    }
}