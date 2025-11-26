package com.example.aicopilot.controller;

import com.example.aicopilot.agent.SuggestionAgent;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import com.example.aicopilot.service.JobRepository;
import com.example.aicopilot.service.WorkflowOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final WorkflowOrchestrator orchestrator;
    private final JobRepository jobRepository;
    private final SuggestionAgent suggestionAgent;
    private final ObjectMapper objectMapper;

    /**
     * 1. [Mode A] Quick Start (자연어 -> 리스트 -> 맵)
     */
    @PostMapping("/start")
    public ResponseEntity<?> startJob(@RequestBody Map<String, String> request) {
        String prompt = request.get("userPrompt");
        String jobId = UUID.randomUUID().toString();

        jobRepository.initJob(jobId);
        orchestrator.runQuickStartJob(jobId, prompt); // Quick Start 모드 실행

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "message", "Mode A (Quick Start) 작업이 시작되었습니다."
        ));
    }

    /**
     * 2. [Mode B] Transformation (리스트 JSON -> 맵)
     * 프론트엔드에서 편집한 '단계 리스트'를 기반으로 맵 생성을 요청합니다.
     */
    @PostMapping("/transform")
    public ResponseEntity<?> transformJob(@RequestBody ProcessDefinition definition) {
        String jobId = UUID.randomUUID().toString();

        try {
            String definitionJson = objectMapper.writeValueAsString(definition);

            jobRepository.initJob(jobId);
            orchestrator.runTransformationJob(jobId, definitionJson); // Transformation 모드 실행

            return ResponseEntity.accepted().body(Map.of(
                    "jobId", jobId,
                    "message", "Mode B (Transformation) 작업이 시작되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid Process Definition Format");
        }
    }

    /**
     * 3. 상태 조회 (Polling)
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        JobStatus status = jobRepository.findById(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        String etag = "\"" + status.version() + "\"";
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(status);
    }

    /**
     * 4. 실시간 제안 (On-Demand)
     */
    @PostMapping("/suggest")
    public ResponseEntity<SuggestionResponse> suggestNextSteps(@RequestBody Map<String, String> request) {
        String currentGraphJson = request.get("currentGraphJson");
        String focusNodeId = request.get("focusNodeId");
        String prompt = "Analyze the provided graph and suggest the next logical steps after node: " + focusNodeId;

        SuggestionResponse response = suggestionAgent.suggestNextSteps(prompt, currentGraphJson, focusNodeId);
        return ResponseEntity.ok(response);
    }
}