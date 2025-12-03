package com.example.aicopilot.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Represents the linkage to the original source asset (e.g., coordinates in an image/PDF).
 */
public record SourceReference(
        @JsonProperty("fileId")
        String fileId,

        @JsonProperty("pageIndex")
        int pageIndex,

        @JsonProperty("rects")
        @JsonPropertyDescription("List of bounding boxes (normalized 0-100%).")
        List<SourceRect> rects,

        @JsonProperty("confidence")
        double confidence,

        @JsonProperty("snippet")
        String snippet,

        // [New] AI Reasoning for the confidence score
        @JsonProperty("reason")
        @JsonPropertyDescription("Explanation for the confidence score (e.g. 'Text blurry', 'Clear shape').")
        String reason
) {
    public record SourceRect(
            double x,
            double y,
            double w,
            double h
    ) {}
}