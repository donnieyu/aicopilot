package com.example.aicopilot.dto.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Response for Asset Analysis (Image/File -> Context).
 */
public record AssetAnalysisResponse(
        @JsonProperty("topic")
        @JsonPropertyDescription("A concise topic title derived from the asset (e.g. 'Expense Approval Process').")
        String topic,

        @JsonProperty("description")
        @JsonPropertyDescription("A detailed textual description of the process flow found in the asset, ready for the Outliner.")
        String description
) {}