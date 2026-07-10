package com.modernizer.orchestrator_service.api;

import com.modernizer.orchestrator_service.api.dto.JobResponse;
import com.modernizer.orchestrator_service.api.dto.SubmitJobRequest;
import com.modernizer.orchestrator_service.pipeline.PipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final PipelineOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<JobResponse> submit(@RequestBody SubmitJobRequest req) {
        UUID jobId = UUID.randomUUID();
        // TODO: validate request, persist job as PENDING
        orchestrator.run(jobId, req);   // async
        return ResponseEntity.accepted()
                .body(new JobResponse(jobId, "ACCEPTED"));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> status(@PathVariable UUID jobId) {
        // TODO: fetch real status from JobStatusService
        return ResponseEntity.ok(new JobResponse(jobId, "RUNNING"));
    }
}