package com.modernizer.orchestrator_service.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HotStore {
  // Pod memory — active chunk only
  private final Map<String, Object> memory = new ConcurrentHashMap<>();

  public void put(UUID jobId, String chunkId, Object astChunk) {
    memory.put(jobId + ":" + chunkId, astChunk);
    log.info("[HotStore] cached chunk {} for job {}", chunkId, jobId);
  }
}
