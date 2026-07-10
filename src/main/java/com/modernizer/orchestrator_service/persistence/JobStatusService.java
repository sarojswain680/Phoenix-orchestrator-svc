package com.modernizer.orchestrator_service.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class JobStatusService {

    // TODO: back with JobRepository (Postgres) + Redis for fast reads

    public void markRunning(UUID jobId) {
        log.info("Job {} → RUNNING", jobId);
    }

    public void markStepRunning(UUID jobId, String step) {
        log.info("Job {} → step {} RUNNING", jobId, step);
    }

    public void checkpoint(UUID jobId, String step) {
        log.info("Job {} → checkpoint after {}", jobId, step);
    }

    public void markSucceeded(UUID jobId) {
        log.info("Job {} → SUCCEEDED", jobId);
    }

    public void markFailed(UUID jobId, String reason) {
        log.error("Job {} → FAILED: {}", jobId, reason);
    }
}