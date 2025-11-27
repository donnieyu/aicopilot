package com.example.aicopilot.dto.process;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines the semantic type of a workflow node.
 * [Update] Added explicit START_EVENT and END_EVENT for structural integrity.
 */
public enum NodeType {
    /**
     * Represents the logical start of the process.
     */
    START_EVENT("start_event"),

    /**
     * Represents a step requiring human interaction (e.g., filling a form, approval).
     * configuration: { "formId": "...", "assignee": "..." }
     */
    USER_TASK("user_task"),

    /**
     * Represents an automated system action (e.g., sending email, API call).
     * configuration: { "serviceType": "EMAIL", "params": { ... } }
     */
    SERVICE_TASK("service_task"),

    /**
     * Represents a logical branching point (e.g., exclusively choosing one path).
     * configuration: { "conditions": [ { "expression": "...", "targetId": "..." } ] }
     */
    EXCLUSIVE_GATEWAY("exclusive_gateway"),

    /**
     * Represents the logical end of the process.
     * MUST NOT have a nextActivityId.
     */
    END_EVENT("end_event");

    private final String value;

    NodeType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NodeType fromValue(String value) {
        for (NodeType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NodeType: " + value);
    }
}