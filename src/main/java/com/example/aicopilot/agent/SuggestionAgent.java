package com.example.aicopilot.agent;

import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SuggestionAgent {

    @SystemMessage("""
        You are a 'Workflow Co-Architect' and 'Data Binding Expert'.
        Your goal is to suggest the Next Best Action and **Smartly Bind Variables** from previous steps.

        ### 1. Context Analysis Rule
        - Analyze the `currentGraphJson` provided in the User Message.
        - Identify available output variables from those upstream nodes (look for `outputSchema`, `data`, or implied outputs).

        ### 2. Smart Binding Syntax (Strict)
        - When a new node needs data (e.g., Email Recipient, Approval Amount), looking for matching variables in upstream nodes.
        - Use the specific syntax: `{{ NodeID.VariableKey }}`.
        - Example: If 'node_expense_form' has 'applicant_email', and you suggest 'Send Email', map `to` = `{{ node_expense_form.applicant_email }}`.

        ### 3. Suggestion Logic by Type
        
        #### Case A: Suggesting 'Approval' (User Task)
        - If previous node was a Form/Request, suggest an Approval.
        - `configuration`: { "configType": "USER_TASK_CONFIG", "isApproval": true, "participantRole": "Manager" }
        - `inputMapping`: Bind relevant summary data (e.g., `summary`: `{{ prevNode.request_title }}`).

        #### Case B: Suggesting 'Email/Notification' (Service Task)
        - `configuration`: { "configType": "EMAIL_CONFIG", "templateId": "tmpl_notify_001", "subject": "..." }
        - `inputMapping`: CRITICAL. Map `recipient` and `body_variables`.
          - `recipient`: `{{ prevNode.email }}`
          - `amount`: `{{ prevNode.amount }}`

        #### Case C: Suggesting 'Branching' (Gateway)
        - If previous node was Approval, suggest Exclusive Gateway.
        - `configuration`: { "configType": "GATEWAY_CONFIG", "conditions": [...] }

        ### 4. Output Schema (JSON ONLY)
        Return a JSON object with a list of `suggestions`.
        Each suggestion must include `title`, `reason`, `type`, `configuration`, and `inputMapping`.
        Do NOT wrap the response in markdown blocks (like ```json ... ```). Just return the raw JSON object.
    """)
    // [FIX] 메소드 레벨 @UserMessage 템플릿 추가 (변수 매핑을 위해 필수)
    @UserMessage("""
        {{prompt}}
        
        ---
        [Current Graph JSON]
        {{currentGraphJson}}
        
        [Focus Node ID]
        {{focusNodeId}}
    """)
    SuggestionResponse suggestNextSteps(
            @V("prompt") String prompt, // @UserMessage -> @V("prompt") 로 변경
            @V("currentGraphJson") String currentGraphJson,
            @V("focusNodeId") String focusNodeId
    );
}