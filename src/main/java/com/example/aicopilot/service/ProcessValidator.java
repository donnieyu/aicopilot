package com.example.aicopilot.service;

import com.example.aicopilot.dto.process.Activity;
import com.example.aicopilot.dto.process.NodeType;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.dto.process.config.NodeConfiguration;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Process Design Validator (The Inspector).
 * Checks if the AI-generated process definition is logically valid (e.g., broken links).
 */
@Component
public class ProcessValidator {

    private static final String VIRTUAL_END_NODE = "node_end";

    public void validate(ProcessResponse process) {
        if (process.activities() == null || process.activities().isEmpty()) {
            throw new IllegalArgumentException("Process must have at least one activity.");
        }

        // 1. 모든 실제 노드 ID 수집 (Source of Truth)
        Set<String> validNodeIds = process.activities().stream()
                .map(Activity::id)
                .collect(Collectors.toSet());

        validNodeIds.add(VIRTUAL_END_NODE);

        boolean hasEndEvent = false;

        for (Activity activity : process.activities()) {
            // End Event 검증
            if (activity.type() == NodeType.END_EVENT) {
                hasEndEvent = true;
                if (activity.nextActivityId() != null) {
                    throw new IllegalArgumentException(String.format(
                            "논리적 오류 감지: 종료 노드 ['%s']는 nextActivityId를 가질 수 없습니다.", activity.id()));
                }
                continue; // 종료 노드는 nextActivityId 검증 패스
            }
            if (!hasEndEvent) {
                throw new IllegalArgumentException("프로세스에 'END_EVENT' 타입의 종료 노드가 없습니다.");
            }
            // 일반 노드 검증
            validateNextActivityId(activity, validNodeIds);
            validateGatewayConditions(activity, validNodeIds);
        }
    }

    private void validateNextActivityId(Activity activity, Set<String> validNodeIds) {
        String nextId = activity.nextActivityId();
        // Gateway가 아닌 일반 Task는 nextActivityId가 필수 (흐름의 단절 방지)
        if (activity.type() != NodeType.EXCLUSIVE_GATEWAY && nextId == null) {
            throw new IllegalArgumentException(String.format(
                    "흐름 단절 오류: 노드 ['%s'](Type: %s)에 다음 단계(nextActivityId)가 정의되지 않았습니다.",
                    activity.id(), activity.type()
            ));
        }
        if (nextId != null && !validNodeIds.contains(nextId)) {
            throw new IllegalArgumentException(String.format(
                    "Structural Error Detected: Node ['%s'] refers to non-existent node ['%s'] as nextActivityId.",
                    activity.id(), nextId
            ));
        }
    }

    private void validateGatewayConditions(Activity activity, Set<String> validNodeIds) {
        NodeConfiguration config = activity.configuration();
        if (config != null && config.conditions() != null) {
            for (NodeConfiguration.BranchCondition condition : config.conditions()) {
                String targetId = condition.targetActivityId();
                if (targetId != null && !validNodeIds.contains(targetId)) {
                    throw new IllegalArgumentException(String.format(
                            "Structural Error Detected: Branch condition in Node ['%s'] refers to non-existent node ['%s'] as targetActivityId.",
                            activity.id(), targetId
                    ));
                }
            }
        }
    }
}