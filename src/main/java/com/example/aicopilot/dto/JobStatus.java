package com.example.aicopilot.dto;

import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;

public record JobStatus(
        String jobId,
        State state,
        String message,

        // [NEW] lastUpdatedStage: 마지막으로 업데이트된 단계 (PROCESS, DATA, FORM)
        // 클라이언트가 이 값을 보고 "아, 이번엔 폼 데이터만 바뀌었으니 폼 영역만 다시 그려야겠다"고 판단할 수 있습니다.
        // 전체 화면을 깜빡거리며 다시 그리는 비효율을 막는 핵심 키입니다.
        String lastUpdatedStage,

        // [NEW] version: 데이터의 버전 (0부터 1씩 증가)
        // ETag 생성에 사용됩니다. 데이터가 진짜로 변경되었는지 확인하는 기준이 됩니다.
        // 단순히 시간이 지났다고 해서 무거운 JSON을 다시 보내는 낭비를 막습니다.
        long version,

        ProcessResponse processResponse,
        DataEntitiesResponse dataEntitiesResponse,
        FormResponse formResponse
) {
    public enum State {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public static JobStatus init(String jobId) {
        // 초기 상태: 단계는 INIT, 버전은 0으로 시작합니다.
        return new JobStatus(jobId, State.PENDING, "작업 대기 중...", "INIT", 0L, null, null, null);
    }
}