package com.example.aicopilot.service;

import com.example.aicopilot.agent.ProcessArchitect;
import com.example.aicopilot.agent.ProcessOutliner;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.event.ProcessGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 워크플로우 오케스트레이터 (Ver 7.1).
 * 2-Step Generation (Outliner -> Transformer) 및 Self-Correction 파이프라인을 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final ProcessOutliner processOutliner;  // [NEW] 1단계: 리스트 작성가
    private final ProcessArchitect processArchitect; // [NEW] 2단계: 맵 변환가

    private final ProcessValidator processValidator; // [Safety] 검증기
    private final JobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Mode A: Quick Start (Natural Language -> List -> Map)
     * 사용자의 문장을 받아 정의서를 만들고, 이를 맵으로 변환합니다.
     */
    @Async
    public void runQuickStartJob(String jobId, String userRequest) {
        try {
            // Step 1: Outlining
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "1단계: 요구사항을 분석하여 단계 리스트(Outliner) 작성 중...");
            ProcessDefinition definition = processOutliner.draftDefinition(userRequest);
            String definitionJson = objectMapper.writeValueAsString(definition);

            // Step 2: Transformation with Retry
            transformAndFinalize(jobId, userRequest, definitionJson);

        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    /**
     * Mode B: Transformation (List -> Map)
     * 프론트엔드에서 완성된 정의서(JSON)를 받아 맵으로 변환합니다.
     */
    @Async
    public void runTransformationJob(String jobId, String definitionJson) {
        try {
            // Mode B는 바로 2단계 진입
            String userRequest = "Manual Definition Transformation";
            transformAndFinalize(jobId, userRequest, definitionJson);

        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    // 공통 변환 및 검증 로직 (Self-Correction Loop)
    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "2단계: 리스트를 분석하여 BPMN 프로세스 맵으로 변환(Transformation) 중...");

        long startTransform = System.currentTimeMillis();
        ProcessResponse process = null;
        String lastError = null;
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (attempt == 1) {
                    process = processArchitect.transformToMap(definitionJson);
                } else {
                    jobRepository.updateState(jobId, JobStatus.State.PROCESSING,
                            String.format("구조적 오류 자동 수정 중... (시도 %d/%d)", attempt, maxRetries));

                    String invalidMapJson = objectMapper.writeValueAsString(process);
                    process = processArchitect.fixMap(definitionJson, invalidMapJson, lastError);
                }

                processValidator.validate(process); // 검증
                break; // 성공 시 탈출

            } catch (IllegalArgumentException e) {
                lastError = e.getMessage();
                System.err.printf("[Job %s] Transformation Error (Attempt %d): %s%n", jobId, attempt, lastError);
                if (attempt == maxRetries) throw new RuntimeException("프로세스 맵 변환 실패: " + lastError);
            }
        }

        long duration = System.currentTimeMillis() - startTransform;
        jobRepository.saveArtifact(jobId, "PROCESS", process, duration);

        // 후속 작업(데이터, 폼) 위임
        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private void handleError(String jobId, Exception e) {
        e.printStackTrace();
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "작업 중 오류 발생: " + e.getMessage());
    }
}