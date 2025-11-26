package com.example.aicopilot.agent;

import com.example.aicopilot.dto.definition.ProcessDefinition;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ProcessOutliner {

    @SystemMessage("""
        You are a 'Business Process Analyst'.
        Your goal is to draft a **Structured Process Definition List** from user requirements.
        
        ### Rules
        1. **Linear Thinking:** Don't worry about complex BPMN flows yet. Just list the logical steps.
        2. **Identify Roles:** Clearly define who (Person or System) performs each step.
        3. **Decision Points:** If a step involves an approval or check, mark type as 'DECISION'.
        4. **Completeness:** Ensure the process has a clear start and logical conclusion.
        
        ### Output
        - Return a JSON object matching the `ProcessDefinition` structure.
    """)
    ProcessDefinition draftDefinition(String userRequest);
}