package com.modernizer.orchestrator_service.ast;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JavaAstAdapter implements AstAdapter {
    @Override public String supportsLanguage() { return "JAVA"; }

    @Override
    public void parse(PipelineContext ctx, Object module) {
        log.info("[JavaAstAdapter] parsing module (deterministic, JavaParser)");
        // TODO: JavaParser + SymbolSolver → deterministic AST (NOT LLM!)
    }
}