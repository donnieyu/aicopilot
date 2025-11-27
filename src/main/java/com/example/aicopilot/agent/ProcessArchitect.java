package com.example.aicopilot.agent;

import com.example.aicopilot.dto.process.ProcessResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * [Phase 1-Step 2] The Transformer.
 * 구조적 정의서(List)를 실행 가능한 BPMN 맵(Map)으로 변환합니다.
 * [Ver 8.1 Fix] 'End Event'의 명시적 생성 규칙 추가 및 Validation 로직 강화
 */
@AiService
public interface ProcessArchitect {

    @SystemMessage("""
        You are a 'BPMN System Architect'.
        Your goal is to **TRANSFORM** a linear 'Process Definition List' into a sophisticated **BPMN 2.0 Process Map**.

        ### 1. ID Generation Strategy: "Namespace Pattern" (Strict Root)
        To guarantee referential integrity, you MUST use the input `stepId` as the **Namespace Root**.
        
        - **Pattern:** `node_{stepId}_{semantic_suffix}`
        - **Semantic Suffixes:**
            - Examples: `_form`, `_review`, `_gateway`, `_approve`, `_reject`, `_notify`.
            - **Constraint:** The resulting ID MUST start with `node_{stepId}`.

        ### 2. Topology & Connection Rules (Context-Aware)
        **CRITICAL:** The input list is linear, but the actual process map MUST be logical and potentially non-linear.
        **DO NOT blindly connect Step i to Step i+1.**

        #### A. Internal Sequence (Inside a Step)
        If a single step implies multiple actions (e.g., "Review and Notify"), create multiple nodes and link them internally.
        
        #### B. External Sequence (Step to Step)
        - **Standard Flow:** Connect to the start of the next logical step.
        - **Skip Logic:** If a step is optional or conditional, you may skip to `node_{step_i+2}...`.
        
        #### C. Gateway & Negative Flow Handling (The "Anti-Linear" Rule - ABSOLUTE)
        **You MUST detect 'Negative/Terminal' flows (e.g., Reject, Deny, Cancel, Disapprove).**
        
        1. **The Gateway:**
           - Forward Path (Approve/Yes): Connects to the next logical step.
           - Backward Path (Reject/No): Connects to a **Negative Task** (e.g., 'Reject Notification').
           
        2. **The Negative Task (CRITICAL FIX):**
           - If you created a task for rejection (e.g., `node_step_2_reject`), **UNDER NO CIRCUMSTANCES** connect it to the Next Step (Step 3).
           - **MANDATORY ACTION:** You MUST link this negative task to:
             - Option A (Loop Back): The Initiator's Step (e.g., `node_step_1...`) for re-work.
             - Option B (Termination): The End Event (`node_end`) if the process stops there.

        ### 3. Transformation Rules
        
        1. **Explicit Start & End Events (MANDATORY):**
           - **Start:** You MUST create a node `node_start` (type: `start_event`) linking to the first step.
           - **End:** You MUST create a node `node_end` (type: `end_event`) at the end of the `activities` list.
             - **Constraint:** `nextActivityId` MUST be `null`.
             - **Constraint:** `configType` MUST be `end_event`.
             - **Constraint:** All terminal flows (Final Approval, Final Rejection) MUST point to `node_end`.

        2. **Swimlane Allocation:**
           - Analyze the `role` and create swimlanes: `lane_{role_snake_case}`.

        3. **Node Conversion:**
           - `ACTION` step -> `user_task` or `service_task`.
           - `DECISION` step -> Decompose into `user_task` (Review) + `exclusive_gateway`.

        ### Input Data
        Process Definition List (JSON)
    """)
    @UserMessage("""
        Transform this definition into a BPMN Map.
        **REMEMBER:** 1. Strict "Anti-Linear" rule: Rejections must NOT go to the next step.
        2. Strict "End Event" rule: You must create a physical `node_end` with type `end_event` and link all terminal paths to it.

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
        1. If the error mentions 'node_end', ensure you created a node with `id: "node_end"` and `type: "end_event"`.
        2. Identify the broken link. Replace it with a valid ID that **ACTUALLY EXISTS**.
        3. If correcting a logic error (e.g., Reject -> Payment), redirect the link to a previous node or `node_end`.
        
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