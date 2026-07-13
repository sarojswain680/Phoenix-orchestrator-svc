package com.modernizer.orchestrator_service.pipeline.steps;

import com.modernizer.orchestrator_service.pipeline.*;
import com.modernizer.orchestrator_service.source.SourceProviderFactory;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
public class IngestStep implements PipelineStep {

  private final SourceProviderFactory factory;

  @Override
  public String name() {
    return "INGEST";
  }

  @Override
  public void execute(PipelineContext ctx) {
    var provider = factory.get(ctx.getSourceType());
    // TODO: provider validates + downloads/clones + AV scan + secret scan
    Path ws = provider.fetch(ctx.getSourceUri(), ctx.getSourceRef(), ctx.getJobId());
    ctx.setWorkspacePath(ws);
  }
}
