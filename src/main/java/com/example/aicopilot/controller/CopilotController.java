package com.example.aicopilot.controller;

import com.example.aicopilot.agent.ProcessOutliner;
import com.example.aicopilot.agent.SuggestionAgent;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import com.example.aicopilot.service.DataContextService;
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
    private final ProcessOutliner processOutliner; // [New] 주입
    private final DataContextService dataContextService;
    private final ObjectMapper objectMapper;

    // ... (startJob, transformJob, getStatus는 기존 유지) ...
    @PostMapping("/start")
    public ResponseEntity<?> startJob(@RequestBody Map<String, String> request) {
        String prompt = request.get("userPrompt");
        String jobId = UUID.randomUUID().toString();
        jobRepository.initJob(jobId);
        orchestrator.runQuickStartJob(jobId, prompt);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId, "message", "Mode A Started"));
    }

    @PostMapping("/transform")
    public ResponseEntity<?> transformJob(@RequestBody ProcessDefinition definition) {
        String jobId = UUID.randomUUID().toString();
        try {
            String definitionJson = objectMapper.writeValueAsString(definition);
            jobRepository.initJob(jobId);
            orchestrator.runTransformationJob(jobId, definitionJson);
            return ResponseEntity.accepted().body(Map.of("jobId", jobId, "message", "Mode B Started"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid Definition");
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        JobStatus status = jobRepository.findById(jobId);
        if (status == null) return ResponseEntity.notFound().build();
        String etag = "\"" + status.version() + "\"";
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(status);
    }

    // -------------------------------------------------------------------------
    // Suggestion APIs (Split by Type)
    // -------------------------------------------------------------------------

    /**
     * Type 1: Graph Context Suggestion (Next Best Action)
     * 기존 /suggest -> /suggest/graph 로 명확화 (기존 클라이언트 호환성을 위해 유지하거나 변경)
     */
    @PostMapping("/suggest/graph") // URL 변경 권장
    public ResponseEntity<SuggestionResponse> suggestNextNode(@RequestBody Map<String, String> request) {
        String currentGraphJson = request.get("currentGraphJson");
        String focusNodeId = request.get("focusNodeId");
        String jobId = request.get("jobId");

        String availableVarsJson = "[]";
        if (jobId != null) {
            JobStatus job = jobRepository.findById(jobId);
            if (job != null && job.processResponse() != null && job.dataEntitiesResponse() != null) {
                availableVarsJson = dataContextService.getAvailableVariablesJson(
                        job.processResponse(),
                        job.dataEntitiesResponse(),
                        focusNodeId
                );
            }
        }

        SuggestionResponse response = suggestionAgent.suggestNextSteps(
                currentGraphJson,
                focusNodeId,
                availableVarsJson
        );
        return ResponseEntity.ok(response);
    }

    // [Legacy Support] 기존 엔드포인트 유지 (필요 시)
    @PostMapping("/suggest")
    public ResponseEntity<SuggestionResponse> suggestLegacy(@RequestBody Map<String, String> request) {
        return suggestNextNode(request);
    }

    /**
     * Type 2: Outline Suggestion (Drafting Phase) [New]
     * 주제와 설명을 주면 프로세스 단계(Steps)를 제안합니다.
     */
    @PostMapping("/suggest/outline")
    public ResponseEntity<ProcessDefinition> suggestOutline(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        String description = request.get("description");

        ProcessDefinition definition = processOutliner.suggestSteps(topic, description);
        return ResponseEntity.ok(definition);
    }
}