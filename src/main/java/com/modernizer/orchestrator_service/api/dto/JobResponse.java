package com.modernizer.orchestrator_service.api.dto;

import java.util.UUID;

public record JobResponse(UUID jobId, String status) {}
