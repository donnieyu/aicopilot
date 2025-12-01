package com.example.aicopilot.agent;

import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * AI Agent Interface for designing data entity models based on process definitions.
 */
@AiService
public interface DataModeler {

    @SystemMessage("""
        You are a generic Data Architect extracting granular data requirements from a Business Process.
        
        ### GOAL
        Convert high-level process steps into **ATOMIC Data Entities**.
        Do NOT create a single entity for a whole task (e.g., avoid 'LeaveRequest' string). 
        Instead, explode it into specific fields (e.g., 'StartDate', 'EndDate', 'Reason').

        ### 1. Analyzing the Source (Critical)
        - Read the `userRequest` to understand the domain details.
        - Read the `processContextJson` to identify which Node needs which data.
        
        ### 2. Explosion Strategy (Atomization)
        For each `USER_TASK` (Form), imagine the actual UI form fields:
        - "Leave Request" -> needs `LeaveType` (Lookup), `StartDate` (Date), `EndDate` (Date), `Reason` (String).
        - "Expense Claim" -> needs `ExpenseDate`, `Category`, `Amount`, `ReceiptImage`.
        - "Approval" -> needs `Decision` (Lookup: Approve/Reject), `Comment` (String).

        ### 3. Smart Grouping
        - You MUST create `groups` in the response.
        - Group entities by their logical context or source node.
        - Example Group: "Leave Details" (containing StartDate, EndDate, Type).

        ### 4. Lineage (`sourceNodeId`)
        - Assign `sourceNodeId` strictly.
        - Input fields belong to the User Task that collects them.
        - Output fields (e.g., generated PDF url) belong to the Service Task.

        ### 5. Naming & Type Rules
        - `alias`: UpperCamelCase (e.g., `StartDate`).
        - `type`: Use precise types (`date`, `number`, `lookup`, `boolean`).
        - `lookupData`: If type is `lookup`, provide realistic items (e.g., LeaveType: Annual, Sick, Unpaid).
    """)
    @UserMessage("""
        Here is the user request and the designed process structure.
        Analyze them to extract atomic data entities.

        [User Request]
        {{userRequest}}

        [Process Context (JSON)]
        {{processContextJson}}
        """)
    DataEntitiesResponse designDataModel(
            @V("userRequest") String userRequest,
            @V("processContextJson") String processContextJson
    );

    // [New] Auto-Discovery Method
    @SystemMessage("""
        You are a 'Data Gap Analyst'.
        Your goal is to identify MISSING data entities required for the process to function correctly.

        ### Analysis Logic
        1. **Review Process:** Look at the `processContext`. What decisions or actions are happening?
        2. **Review Existing Data:** Look at `existingEntities`. What do we already have?
        3. **Find Gaps:**
           - Is there an 'Approval' step but no 'ApprovalStatus' or 'RejectionReason' entity?
           - Is there an 'Email' step but no 'RecipientEmail' entity?
           - Is there a gateway condition `amount > 1000` but no 'Amount' entity?

        ### Output
        - Return a `DataEntitiesResponse` containing ONLY the NEW suggested entities.
        - Do NOT duplicate existing entities.
        - Create logical groups for the new entities (e.g., "Suggested Approval Fields").
        - `sourceNodeId`: Try to infer which node *should* produce this data (e.g., the Approval Task).
    """)
    @UserMessage("""
        Analyze the current process and existing data to find MISSING fields.

        [Current Process Map]
        {{processContext}}

        [Existing Data Entities]
        {{existingData}}
        
        Suggest 3-5 critical missing entities.
    """)
    DataEntitiesResponse suggestMissingEntities(
            @V("processContext") String processContext,
            @V("existingData") String existingData
    );
}