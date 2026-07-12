package com.modernizer.orchestrator_service.pipeline.steps;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import com.modernizer.orchestrator_service.pipeline.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ScanPartitionStep implements PipelineStep {

  @Override
  public String name() {
    return "SCAN_PARTITION";
  }

  @Override
  public void execute(PipelineContext ctx) {
    // TODO: walk workspace → classify files → partition FE/BE modules
  }
}
