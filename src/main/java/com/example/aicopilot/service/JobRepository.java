package com.example.aicopilot.service;

import com.example.aicopilot.dto.*;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobRepository {
    private final Map<String, JobStatus> store = new ConcurrentHashMap<>();

    public void save(JobStatus status) {
        store.put(status.jobId(), status);
    }

    public JobStatus findById(String jobId) {
        return store.get(jobId);
    }

    public void initJob(String jobId) {
        save(JobStatus.init(jobId));
    }

    public void updateState(String jobId, JobStatus.State state, String message) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId, state, message,
                    current.lastUpdatedStage(), // 단계 유지
                    current.version() + 1,      // 버전 증가 (메시지만 바뀐 것도 변경임)
                    current.processResponse(), current.dataEntitiesResponse(), current.formResponse()
            ));
        }
    }

    public void saveArtifact(String jobId, String type, ProcessResponse processResponse) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId, JobStatus.State.PROCESSING, current.message(),
                    "PROCESS",            // ★ 단계 명시
                    current.version() + 1, // ★ 버전 증가
                    processResponse, current.dataEntitiesResponse(), current.formResponse()
            ));
        }
    }

    // DataEntitiesResponse, FormResponse 저장 메서드도 동일하게 수정 (type 파라미터를 lastUpdatedStage로 활용)
    public void saveArtifact(String jobId, String type, DataEntitiesResponse dataEntitiesResponse) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId, JobStatus.State.PROCESSING, current.message(),
                    "DATA",                // ★ 단계 명시
                    current.version() + 1, // ★ 버전 증가
                    current.processResponse(), dataEntitiesResponse, current.formResponse()
            ));
        }
    }

    public void saveArtifact(String jobId, String type, FormResponse formResponse) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId, JobStatus.State.PROCESSING, current.message(),
                    "FORM",                // ★ 단계 명시
                    current.version() + 1, // ★ 버전 증가
                    current.processResponse(), current.dataEntitiesResponse(), formResponse
            ));
        }
    }
}