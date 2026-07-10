package com.modernizer.orchestrator_service.pipeline.steps;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import com.modernizer.orchestrator_service.pipeline.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class BuildGraphStep implements PipelineStep {

    @Override
    public String name() {
        return "BUILD_GRAPH";
    }

    @Override
    public void execute(PipelineContext ctx) {
        // TODO: build dependency graph + FE<->BE cross-link
    }
}