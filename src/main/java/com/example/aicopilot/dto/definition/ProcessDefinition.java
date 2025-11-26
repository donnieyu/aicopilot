package com.example.aicopilot.dto.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * [Phase 1-Step 1] 프로세스 정의서 (The Outliner).
 * BPMN 시각화 전 단계의 순수 비즈니스 로직 리스트입니다.
 */
public record ProcessDefinition(
        @JsonProperty("topic")
        @JsonPropertyDescription("The main topic of the process (e.g., 'Vacation Request').")
        String topic,

        @JsonProperty("steps")
        @JsonPropertyDescription("Ordered list of business steps.")
        List<ProcessStep> steps
) {}