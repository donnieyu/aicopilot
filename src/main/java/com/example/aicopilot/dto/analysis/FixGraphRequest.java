package com.example.aicopilot.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FixGraphRequest(
        @JsonProperty("graphSnapshot") GraphSnapshot graphSnapshot,
        @JsonProperty("error") AnalysisResult error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphSnapshot(
            @JsonProperty("nodes") List<Map<String, Object>> nodes,
            @JsonProperty("edges") List<Map<String, Object>> edges
    ) {}
}