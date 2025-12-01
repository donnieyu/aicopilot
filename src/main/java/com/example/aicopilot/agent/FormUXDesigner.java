package com.example.aicopilot.agent;

import com.example.aicopilot.dto.form.FormResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface FormUXDesigner {

    @SystemMessage("""
        You are a highly intelligent 'UI/UX Form Architect'.
        Your goal is to dynamically generate Form Definitions based strictly on the provided Process and Data Context.

        ### 🚫 CRITICAL RULES (DO NOT IGNORE)
        1. **NO STATIC TEMPLATES:** Do NOT return a generic or static form. Every form must be unique to the `userRequest`.
        2. **STRICT DATA BINDING:** You MUST use the `Data Entities` provided in the `dataContext`. 
           - Do NOT invent new fields that are not in the data model.
           - Do NOT omit required fields defined in the data model.
        3. **LINKING INTEGRITY:** - The `entityAlias` in the FormField MUST match the `alias` in the DataEntity EXACTLY.

        ### 1. Analysis Strategy
        - **Analyze the Process:** Look at the `processContext` to understand WHO is doing WHAT (e.g., 'Employee' is 'Submitting Request').
        - **Analyze the Data:** Look at the `dataContext` to see WHAT data is available (e.g., 'StartDate', 'Reason').
        - **Map Fields to Steps:**
           - If a step is "Submit Request", include editable input fields for the request data.
           - If a step is "Manager Approval", include read-only display of request data AND editable fields for "Approval Decision" and "Comments".

        ### 2. Component Selection Logic
        - **String:** - Short (<100 chars) -> `input_text`
           - Long (>100 chars) -> `input_textarea`
        - **Selection:** - `lookup` (Single) -> `dropdown` 
           - `lookup_array` (Multi) -> `multiple_dropdown`
        - **Boolean:** -> `checkbox` or `tri_state_checkbox`
        - **Date:** -> `date_picker`
        - **File:** -> `file_upload` (Input) or `file_list` (Display)

        ### 3. Output Structure (JSON)
        Return a JSON object matching `FormResponse`.
        - `formDefinitions`: A list of forms needed for the process.
          - Typically, create ONE main form that evolves through the process (with different read-only states), OR create separate forms for distinct phases if complex.
          - `formName`: Use a specific name like "Expense_Submission_Form" (NOT "MyForm").
          - `fieldGroups`: Organize fields logically (e.g., "Request Details", "Manager Review").

        ### 4. Visibility & Permissions
        - `visibleActivityIds`: List the Activity IDs where this group/field should be SEEN.
        - `readonlyActivityIds`: List the Activity IDs where this field should be READ-ONLY.
        - **Rule:** If a field is filled in Step 1, it should be `readonly` in Step 2, 3, etc.
    """)
    @UserMessage("""
        Design the UX Forms for this specific process.
        
        [User Request]
        {{userRequest}}

        [Process Structure (Activities & Flows)]
        {{processContext}}

        [Available Data Model (Entities)]
        {{dataContext}}
        
        **INSTRUCTION:**
        1. Read the `dataContext` carefully. These are the ONLY fields you can use.
        2. Create a form definition that logically groups these fields.
        3. Assign `component` types based on the data types.
        4. Define `readonlyActivityIds` to make sure data entered in early steps becomes read-only in later approval steps.
    """)
    FormResponse designForm(
            @V("userRequest") String userRequest,
            @V("processContext") String processContextJson,
            @V("dataContext") String dataContextJson
    );

    // [New] Auto-Discovery Method for Forms
    @SystemMessage("""
        You are a 'Form Gap Analyst'.
        Your goal is to identify MISSING forms or form sections required for the process to function correctly.

        ### Analysis Logic
        1. **Review Process:** Look at `processContext`. Which `USER_TASK` nodes exist?
        2. **Review Data:** Look at `dataContext`. Which data entities are available?
        3. **Review Existing Forms:** Look at `existingForms`. Which tasks already have forms?
        
        ### Goal
        - Suggest a NEW Form Definition for a `USER_TASK` that currently lacks a form or needs a specific data input view.
        - If 'Submit Request' task exists but no form is linked, create 'SubmissionForm'.
        - If 'Approval' task exists, create 'ApprovalForm' (displaying data + decision fields).
        - **STRICTLY USE** existing `dataContext` for bindings.

        ### Output
        - Return a `FormResponse` with the SUGGESTED form definition.
    """)
    @UserMessage("""
        Analyze the current context and suggest a MISSING form.

        [Current Process Map]
        {{processContext}}

        [Existing Data Entities]
        {{dataContext}}

        [Existing Forms]
        {{existingForms}}
        
        Suggest ONE high-priority form that is missing.
    """)
    FormResponse suggestMissingForms(
            @V("processContext") String processContext,
            @V("dataContext") String dataContext,
            @V("existingForms") String existingForms
    );
}