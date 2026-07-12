package com.modernizer.orchestrator_service.pipeline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class PipelineContext {

  private final UUID jobId;
  private final String tenantId;

  private String sourceType;
  private String sourceUri;
  private String sourceRef;

  private Path workspacePath;
  private List<Object> modules = new ArrayList<>(); // TODO: Module
  private Object projectProfile; // TODO: ProjectProfile
  private List<Object> codeUnits = new ArrayList<>(); // TODO: CodeUnit
  private Object dependencyGraph; // TODO: Graph
  private List<Object> insights = new ArrayList<>(); // TODO: SemanticInsight
  private Object verificationReport;

  public PipelineContext(UUID jobId, String tenantId) {
    this.jobId = jobId;
    this.tenantId = tenantId;
  }
}
