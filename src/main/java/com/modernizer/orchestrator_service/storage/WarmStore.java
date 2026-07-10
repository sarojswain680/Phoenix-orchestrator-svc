package com.modernizer.orchestrator_service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class WarmStore {
    // Redis — METADATA / POINTERS / LOCKS only (NOT giant ASTs)
    public void putPointer(UUID jobId, String chunkId) {
        log.info("[WarmStore] pointer for chunk {} job {}", chunkId, jobId);
        // TODO: RedisTemplate.opsForValue().set(...)
    }
}