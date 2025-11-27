package com.example.aicopilot.service;

import com.example.aicopilot.dto.process.Activity;
import com.example.aicopilot.dto.process.NodeType;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.dto.process.config.NodeConfiguration;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 프로세스 설계 검증기 (The Inspector).
 * AI가 생성한 프로세스 정의가 논리적으로 타당한지(Broken Link 등) 검사합니다.
 * [Ver 8.1 Fix] 가상 노드 로직 제거 및 END_EVENT 타입 검증 강화
 */
@Component
public class ProcessValidator {

    // [Deleted] Virtual node logic causes hallucination. "node_end" must be a real node.
    // private static final String VIRTUAL_END_NODE = "node_end";

    public void validate(ProcessResponse process) {
        if (process.activities() == null || process.activities().isEmpty()) {
            throw new IllegalArgumentException("프로세스에 최소 하나의 액티비티가 있어야 합니다.");
        }

        // 1. 모든 실제 노드 ID 수집 (Source of Truth)
        Set<String> validNodeIds = process.activities().stream()
                .map(Activity::id)
                .collect(Collectors.toSet());

        // [Removed] validNodeIds.add(VIRTUAL_END_NODE);

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

            // 일반 노드 검증
            validateNextActivityId(activity, validNodeIds);
            validateGatewayConditions(activity, validNodeIds);
        }

        if (!hasEndEvent) {
            throw new IllegalArgumentException("프로세스에 'END_EVENT' 타입의 종료 노드가 없습니다.");
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
                    "구조적 오류 감지: 노드 ['%s']가 존재하지 않는 노드 ['%s']를 nextActivityId로 참조하고 있습니다.",
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
                            "구조적 오류 감지: 노드 ['%s']의 분기 조건이 존재하지 않는 노드 ['%s']를 targetActivityId로 참조하고 있습니다.",
                            activity.id(), targetId
                    ));
                }
            }
        }
    }
}