package com.modernizer.orchestrator_service.ast;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SidecarAstAdapter implements AstAdapter {

    @Override
    public String supportsLanguage() {
        return "JS";
    }

    @Override
    public void parse(PipelineContext ctx, Object module) {
        log.info("[SidecarAstAdapter] would call Node sidecar via gRPC");
        // TODO: gRPC streaming call to Node parser sidecar
    }
}