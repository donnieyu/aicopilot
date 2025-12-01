package com.example.aicopilot.dto.suggestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoDiscoveryRequest(
        @JsonProperty("processContext")
        Map<String, Object> processContext, // JSON Object of nodes/edges

        @JsonProperty("existingEntities")
        List<Map<String, Object>> existingEntities // Simplified list of current entities
) {}