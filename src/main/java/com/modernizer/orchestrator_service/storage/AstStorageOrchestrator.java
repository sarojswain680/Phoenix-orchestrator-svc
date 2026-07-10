package com.modernizer.orchestrator_service.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AstStorageOrchestrator {

    private final HotStore hot;
    private final WarmStore warm;
    private final ColdStore cold;

    public void storeChunk(UUID jobId, String chunkId, Object astChunk) {
        hot.put(jobId, chunkId, astChunk);
        warm.putPointer(jobId, chunkId);
        cold.archive(jobId, chunkId, astChunk);
    }
}