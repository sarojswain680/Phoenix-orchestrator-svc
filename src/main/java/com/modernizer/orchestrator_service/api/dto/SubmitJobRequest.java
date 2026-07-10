package com.modernizer.orchestrator_service.api.dto;

public record SubmitJobRequest(
        String sourceType,      // "ZIP" or "GIT"
        String uri,             // s3://... or https://github.com/...
        String ref,             // branch/commit (git only)
        String tenantId
) {}