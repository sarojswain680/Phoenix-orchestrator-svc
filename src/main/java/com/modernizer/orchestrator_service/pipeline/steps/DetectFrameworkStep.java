package com.modernizer.orchestrator_service.pipeline.steps;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import com.modernizer.orchestrator_service.pipeline.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class DetectFrameworkStep implements PipelineStep {

    @Override
    public String name() {
        return "DETECT_FRAMEWORK";
    }

    @Override
    public void execute(PipelineContext ctx) {
        // TODO: layered detection (extension → manifest → heuristic → LLM tiebreak)
    }
}