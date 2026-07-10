package com.modernizer.orchestrator_service.enrich;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CodeParserAgent {

    /**
     * Gemini SEMANTIC ENRICHMENT only (business purpose, layer, risks).
     * Send SIGNATURES, never full code. Redact secrets first.
     */
    public Object analyze(Object codeUnit) {
        log.info("[CodeParserAgent] enrich (stub) - Gemini wiring added later");
        // TODO: wire Spring AI ChatClient once GCP credentials configured
        return null;
    }
}