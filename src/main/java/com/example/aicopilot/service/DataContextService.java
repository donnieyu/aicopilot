package com.example.aicopilot.service;

import com.example.aicopilot.dto.dataEntities.DataEntity;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.process.Activity;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 데이터 컨텍스트 분석기.
 * 현재 포커스 된 노드 시점에서 '사용 가능한 변수(Upstream Variables)'가 무엇인지 계산합니다.
 */
@Service
@RequiredArgsConstructor
public class DataContextService {

    private final ObjectMapper objectMapper;

    /**
     * 현재 노드(focusNodeId) 이전에 존재하는 모든 데이터 엔티티를 조회합니다.
     * (단순화를 위해 현재는 모든 이전 노드의 데이터를 가져오지만, 추후 분기 로직에 따라 도달 가능한 데이터만 필터링할 수 있습니다.)
     */
    public String getAvailableVariablesJson(ProcessResponse process, DataEntitiesResponse dataModel, String focusNodeId) {
        if (process == null || dataModel == null) {
            return "[]";
        }

        // 1. Upstream Node IDs 찾기 (현재 노드보다 앞에 있는 노드들)
        Set<String> upstreamNodeIds = findUpstreamNodeIds(process, focusNodeId);

        // 2. 해당 노드들에서 생성된 데이터 엔티티 필터링
        List<Map<String, String>> availableVariables = dataModel.entities().stream()
                .filter(entity -> entity.sourceNodeId() != null && upstreamNodeIds.contains(entity.sourceNodeId()))
                .map(this::mapToVariableContext)
                .collect(Collectors.toList());

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(availableVariables);
        } catch (Exception e) {
            return "[]";
        }
    }

    // 간단한 BFS/DFS 역탐색 대신, 리스트 순서를 기반으로 앞쪽 노드들을 가져옵니다.
    // (BPMN 구조가 복잡해지면 Graph Traversal로 고도화 필요)
    private Set<String> findUpstreamNodeIds(ProcessResponse process, String focusNodeId) {
        Set<String> upstreamIds = new HashSet<>();
        boolean foundFocus = false;

        // activities 리스트는 위상 정렬되어 있다고 가정 (Outliner -> Architect 변환 시 순서 유지됨)
        for (Activity activity : process.activities()) {
            if (activity.id().equals(focusNodeId)) {
                foundFocus = true;
                break;
            }
            upstreamIds.add(activity.id());
        }

        // 만약 리스트 순서와 그래프 연결이 다르다면, 여기서 Edge 기반 역탐색 로직이 추가되어야 함.
        return upstreamIds;
    }

    private Map<String, String> mapToVariableContext(DataEntity entity) {
        // AI가 바인딩하기 편한 형태로 변환
        Map<String, String> var = new HashMap<>();
        var.put("variableName", entity.alias()); // e.g., ApplicantEmail
        var.put("sourceNodeId", entity.sourceNodeId()); // e.g., node_step_1
        var.put("type", entity.type().getValue()); // e.g., string
        var.put("description", entity.label()); // e.g., 지원자 이메일
        // AI를 위한 바인딩 문법 힌트 제공
        var.put("bindingSyntax", String.format("#{%s.%s}", entity.sourceNodeId(), entity.alias()));
        return var;
    }
}