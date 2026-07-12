package com.modernizer.orchestrator_service.pipeline;

import com.modernizer.orchestrator_service.api.dto.SubmitJobRequest;
import com.modernizer.orchestrator_service.persistence.JobStatusService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineOrchestrator {

  private final List<PipelineStep> steps; // Spring injects in @Order sequence
  private final JobStatusService jobStatus;

  @Async("pipelineExecutor")
  public void run(UUID jobId, SubmitJobRequest req) {
    PipelineContext ctx = new PipelineContext(jobId, req.tenantId());
    ctx.setSourceType(req.sourceType());
    ctx.setSourceUri(req.uri());
    ctx.setSourceRef(req.ref());

    jobStatus.markRunning(jobId);
    try {
      for (PipelineStep step : steps) {
        log.info("Job {} → executing step {}", jobId, step.name());
        jobStatus.markStepRunning(jobId, step.name());
        step.execute(ctx);
        jobStatus.checkpoint(jobId, step.name());
      }
      jobStatus.markSucceeded(jobId);
      // TODO: persist DependencyMap + publish event → Stage 2
    } catch (Exception e) {
      log.error("Job {} failed", jobId, e);
      jobStatus.markFailed(jobId, e.getMessage());
    } finally {
      // TODO: cleanup workspace
    }
  }
}
