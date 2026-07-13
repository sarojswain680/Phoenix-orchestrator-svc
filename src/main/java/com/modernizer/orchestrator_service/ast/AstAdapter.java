package com.modernizer.orchestrator_service.ast;

import com.modernizer.orchestrator_service.pipeline.PipelineContext;

public interface AstAdapter {
  String supportsLanguage(); // "JAVA", "JS", "ANGULAR"...

  void parse(PipelineContext ctx, Object module); // TODO: return CodeUnits
}
