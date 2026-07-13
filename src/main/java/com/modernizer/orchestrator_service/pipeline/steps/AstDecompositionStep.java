package com.modernizer.orchestrator_service.pipeline.steps;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import com.modernizer.orchestrator_service.pipeline.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class AstDecompositionStep implements PipelineStep {

  @Override
  public String name() {
    return "AST_DECOMPOSITION";
  }

  @Override
  public void execute(PipelineContext ctx) {
    // TODO: chunked, deterministic parsing (JavaParser + Node sidecar via gRPC)
    //       NOT LLM. Stream + checkpoint + fault isolation.
  }
}
