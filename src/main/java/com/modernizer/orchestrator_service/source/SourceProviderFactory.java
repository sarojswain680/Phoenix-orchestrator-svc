package com.modernizer.orchestrator_service.source;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SourceProviderFactory {

    private final List<SourceProvider> providers;
    private Map<String, SourceProvider> map;

    public SourceProvider get(String type) {
        if (map == null) {
            map = providers.stream()
                    .collect(Collectors.toMap(SourceProvider::supports, Function.identity()));
        }
        SourceProvider p = map.get(type);
        if (p == null) {
            throw new IllegalArgumentException("Unsupported source type: " + type);
        }
        return p;
    }
}