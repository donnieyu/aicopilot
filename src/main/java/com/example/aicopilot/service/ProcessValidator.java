package com.example.aicopilot.service;

import com.example.aicopilot.dto.process.Activity;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.dto.process.config.NodeConfiguration;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 프로세스 설계 검증기 (The Inspector).
 * AI가 생성한 프로세스 정의가 논리적으로 타당한지(Broken Link 등) 검사합니다.
 */
@Component
public class ProcessValidator {

    private static final String VIRTUAL_END_NODE = "node_end";

    public void validate(ProcessResponse process) {
        if (process.activities() == null || process.activities().isEmpty()) {
            throw new IllegalArgumentException("프로세스에 최소 하나의 액티비티가 있어야 합니다.");
        }

        Set<String> validNodeIds = process.activities().stream()
                .map(Activity::id)
                .collect(Collectors.toSet());

        validNodeIds.add(VIRTUAL_END_NODE);

        for (Activity activity : process.activities()) {
            validateNextActivityId(activity, validNodeIds);
            validateGatewayConditions(activity, validNodeIds);
        }
    }

    private void validateNextActivityId(Activity activity, Set<String> validNodeIds) {
        String nextId = activity.nextActivityId();
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