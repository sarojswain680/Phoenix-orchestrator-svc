package com.modernizer.orchestrator_service.pipeline;

public interface PipelineStep {
    String name();
    void execute(PipelineContext ctx);
}
