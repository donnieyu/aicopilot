package com.example.aicopilot.agent;

import com.example.aicopilot.dto.process.ProcessResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * [Phase 1-Step 2] The Transformer.
 * 구조적 정의서(List)를 실행 가능한 BPMN 맵(Map)으로 변환합니다.
 * [Ver 7.3 Final] ID Anchoring을 유지하되, One-to-Many 노드 확장의 유연성을 명시했습니다.
 */
@AiService
public interface ProcessArchitect {

    @SystemMessage("""
        You are a 'BPMN System Architect'.
        Your goal is to **TRANSFORM** a linear 'Process Definition List' into a standard **BPMN 2.0 Process Map**.

        ### 1. ID Generation Strategy: "Namespace Pattern" (Strict Root)
        To guarantee referential integrity, you MUST use the input `stepId` as the **Namespace Root**.
        
        - **Pattern:** `node_{stepId}_{semantic_suffix}`
        - **Semantic Suffixes:**
            - You are FREE to create multiple nodes for a single step if the logic requires it.
            - Examples: `_form`, `_review`, `_gateway`, `_email_notify`, `_api_call`.
            - **Constraint:** The resulting ID MUST start with `node_{stepId}`.

        ### 2. Topology & Connection Rules (Logic-Driven)
        
        #### A. Internal Sequence (Inside a Step)
        If a single step explodes into multiple nodes (e.g., Step 1 -> Form + Email):
        - Link them internally first: `node_step_1_form` -> `node_step_1_email`.
        
        #### B. External Sequence (Step to Step)
        - **Default Rule:** The last node of Step `i` connects to the first node of Step `i+1`.
          - Target Calculation: Find `stepId` of index i+1 -> Target is `node_{next_step_id}_{primary_suffix}`.
        
        #### C. Gateway Routing (The Controller)
        - **Role:** Gateways control the flow overriding the Default Rule.
        - **Forward:** Connect to `node_{next_step_id}...` (Approve).
        - **Backward:** Connect to `node_{previous_step_id}...` (Reject/Loop).
        - **Skip:** Connect to `node_{step_i+2}...` (Skip logic).

        ### 3. Transformation Rules
        
        1. **Swimlane Allocation:**
           - Analyze the `role` and create swimlanes: `lane_{role_snake_case}`.

        2. **Node Conversion:**
           - `ACTION` step -> `USER_TASK` or `SERVICE_TASK`.
           - `DECISION` step -> Decompose into `USER_TASK` (isApproval: true) + `EXCLUSIVE_GATEWAY`.

        3. **Configuration:**
           - Fill `configuration` with correct `configType` ('USER_TASK_CONFIG', 'GATEWAY_CONFIG', 'EMAIL_CONFIG').

        ### Input Data
        Process Definition List (JSON)
    """)
    @UserMessage("""
        Transform this definition into a BPMN Map.

        [Process Definition List]
        {{definitionJson}}
    """)
    ProcessResponse transformToMap(@V("definitionJson") String definitionJson);

    // [Self-Correction] 오류 수정 메서드
    @UserMessage("""
        The transformed map has validation errors. Fix the JSON based on the error.
        
        ### Error Message
        {{errorMessage}}
        
        ### Instruction for FIX
        1. Identify the broken link (`nextActivityId` or `targetActivityId`).
        2. Replace it with a valid ID that **ACTUALLY EXISTS** in the generated nodes.
        3. If linking to another step, ensure you use the correct Root Anchor (e.g., `node_step_X...`).
        
        ### Original Definition
        {{definitionJson}}
        
        ### Invalid Map Generated
        {{invalidMapJson}}
        
        Return the CORRECTED JSON.
    """)
    ProcessResponse fixMap(
            @V("definitionJson") String definitionJson,
            @V("invalidMapJson") String invalidMapJson,
            @V("errorMessage") String errorMessage
    );
}