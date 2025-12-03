package com.example.aicopilot.dto.definition;

import com.example.aicopilot.dto.common.SourceReference; // [New] Import
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * [Phase 1-Step 1] 정의서의 개별 단계 항목.
 */
public record ProcessStep(
        @JsonProperty("stepId")
        @JsonPropertyDescription("Simple numeric or code ID (e.g., '1', 'step_1').")
        String stepId,

        @JsonProperty("name")
        @JsonPropertyDescription("Short name of the step (e.g., 'Manager Approval').")
        String name,

        @JsonProperty("role")
        @JsonPropertyDescription("Who performs this step? (e.g., 'Employee', 'Manager', 'System').")
        String role,

        @JsonProperty("description")
        @JsonPropertyDescription("What happens in this step?")
        String description,

        @JsonProperty("type")
        @JsonPropertyDescription("Simple type: 'ACTION' (User/System) or 'DECISION' (Branching point).")
        String type,

        // [New] Source Mapping Info
        @JsonProperty("sourceRef")
        SourceReference sourceRef
) {}