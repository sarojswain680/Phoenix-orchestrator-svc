package com.modernizer.orchestrator_service.storage;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ColdStore {
  // Cloud Storage (S3/MinIO) — full AST chunks, durable
  public void archive(UUID jobId, String chunkId, Object astChunk) {
    log.info("[ColdStore] archiving chunk {} job {}", chunkId, jobId);
    // TODO: S3 putObject
  }
}
