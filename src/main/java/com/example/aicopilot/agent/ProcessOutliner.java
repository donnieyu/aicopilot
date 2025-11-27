package com.example.aicopilot.agent;

import com.example.aicopilot.dto.definition.ProcessDefinition;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ProcessOutliner {

    // ... (기존 draftDefinition 메서드 유지) ...
    @SystemMessage("""
        You are a 'Business Process Analyst'.
        Your goal is to draft a **Structured Process Definition List** from user requirements.
        
        ### Rules
        1. **Identify Key Steps:** List the logical steps from start to finish.
        2. **Detect Decision Points:**
           - If a step involves an approval, review, or condition (e.g., "If amount > 1000"), explicitly mark it as 'DECISION'.
           - **CRITICAL:** For 'DECISION' steps, imply what happens on rejection/failure in the description.
        3. **Role Assignment:** Clearly define who (Person or System) performs each step.
        4. **Completeness:** Ensure the process covers the happy path and major exception paths.
        
        ### Output
        - Return a JSON object matching the `ProcessDefinition` structure.
        - `type` must be 'ACTION' or 'DECISION'.
    """)
    ProcessDefinition draftDefinition(String userRequest);

    // [Updated] Outline Suggestion API with Enhanced Prompt
    @UserMessage("""
        Based on the Topic and Description provided by the user, **Draft** a detailed process step list.
        
        [Input]
        Topic: {{topic}}
        Context/Description: {{description}}
        
        [Goal]
        - If the description provides a specific flow (e.g., "A -> B -> C"), **FOLLOW IT STRICTLY**.
        - If the description is vague, use your knowledge of industry standards for the given topic.
        - Break down the process into 3-7 logical steps.
        - **Role Inference:** Infer the most appropriate actor (Role) for each step (e.g., 'Employee', 'Manager', 'System', 'Finance Team').
        - **Decision Points:** If the context implies an approval or check, mark it as 'DECISION'.
        
        [Output Structure]
        Return a JSON with 'topic' and a list of 'steps'. Each step has 'stepId', 'name', 'role', 'description', 'type'.
    """)
    ProcessDefinition suggestSteps(
            @V("topic") String topic,
            @V("description") String description
    );
}