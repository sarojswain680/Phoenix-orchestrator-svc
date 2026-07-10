package com.modernizer.orchestrator_service.source;

import java.nio.file.Path;
import java.util.UUID;

public interface SourceProvider {
    String supports();   // "ZIP" or "GIT"
    Path fetch(String uri, String ref, UUID jobId);
}