package com.modernizer.orchestrator_service.pipeline.steps;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import com.modernizer.orchestrator_service.pipeline.PipelineStep;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class EnrichmentStep implements PipelineStep {

    @Override
    public String name() {
        return "ENRICHMENT";
    }

    @Override
    public void execute(PipelineContext ctx) {
        // TODO: Gemini SEMANTIC ENRICHMENT ONLY (send signatures, not full code)
    }
}