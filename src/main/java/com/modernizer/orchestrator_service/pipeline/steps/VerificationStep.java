package com.modernizer.orchestrator_service.pipeline.steps;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import com.modernizer.orchestrator_service.pipeline.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class VerificationStep implements PipelineStep {

  @Override
  public String name() {
    return "VERIFICATION";
  }

  @Override
  public void execute(PipelineContext ctx) {
    // TODO: round-trip + structural invariants + confidence score
  }
}
