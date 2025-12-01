package com.example.aicopilot.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphStructure(
        @JsonProperty("nodes") List<GraphNode> nodes,
        @JsonProperty("edges") List<GraphEdge> edges
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphNode(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("data") Map<String, Object> data,
            @JsonProperty("position") Map<String, Double> position
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraphEdge(
            @JsonProperty("id") String id,
            @JsonProperty("source") String source,
            @JsonProperty("target") String target,
            @JsonProperty("label") String label
    ) {}
}