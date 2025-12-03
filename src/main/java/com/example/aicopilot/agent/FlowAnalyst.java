package com.example.aicopilot.agent;

import com.example.aicopilot.dto.analysis.AnalysisReport;
import com.example.aicopilot.dto.analysis.GraphStructure;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface FlowAnalyst {

    // [Updated] Analyze Prompt: From "Strict Auditor" to "Process Optimization Consultant"
    @SystemMessage("""
        You are a 'Senior Process Optimization Consultant'.
        Your job is to audit the process graph for structural integrity AND provide insightful suggestions for improvement.

        ### 1. Structural Audit (Mandatory Fixes)
        Detect critical issues that break the flow. Use these EXACT type codes:
        - **MISSING_INPUT**: A node (except Start) is unreachable.
        - **MISSING_OUTPUT**: A node (except End) is a dead end.
        - **DISCONNECTED_FLOW**: Isolated islands of nodes.
        - **LOGIC_GAP**: Gateway missing a path (e.g., 'Approve' exists, 'Reject' missing).

        ### 2. Optimization & Insight (Value-Add Suggestions)
        Look for opportunities to make the process better, faster, or more user-friendly. Use type code **OPTIMIZATION** or **UX_INSIGHT**.
        - **Redundancy:** "Two approval steps in a row might be bottleneck. Consider consolidating."
        - **Parallelism:** "Independent tasks (e.g., 'Book Flight', 'Book Hotel') could be parallelized."
        - **Communication:** "After a rejection, adding a 'Notify User' step improves transparency."
        - **Completeness:** "You have a 'Review' step but no decision gateway after it."

        ### 3. Severity Levels
        - **ERROR**: Broken flow (Must fix).
        - **WARNING**: Logical gap or high risk.
        - **INFO**: Optimization idea or best practice suggestion.

        ### Output
        Return a JSON object matching `AnalysisReport` structure containing a list of `results`.
    """)
    @UserMessage("""
        Analyze this process graph snapshot.
        Provide a mix of structural fixes (if any) and open-minded optimization ideas.
        
        [Nodes]
        {{nodesJson}}
        
        [Edges]
        {{edgesJson}}
    """)
    AnalysisReport analyzeGraph(
            @V("nodesJson") String nodesJson,
            @V("edgesJson") String edgesJson
    );

    // [Updated] Fix Prompt: Enforce "Fix Guarantee" and "Completeness Check"
    @SystemMessage("""
        You are an expert 'Surgical Process Repair Agent'.
        Your ONLY goal is to **ELIMINATE** the reported error in the process graph.

        ### ⚡ PRIME DIRECTIVE: "Fix it, and Don't Break Anything Else"
        1. **Targeted Repair:** Focus ONLY on the `targetNodeId` and the specific `errorType`. Do not refactor unrelated parts of the graph.
        2. **Completeness Guarantee:**
           - If the error is `MISSING_OUTPUT`: You MUST create an edge from the target node to a logical next step (or 'node_end_point').
           - If the error is `LOGIC_GAP` (Missing Branch): You MUST add the missing path (e.g., Reject path) AND connect that new path to a valid destination (e.g., loop back to start or end).
           - **CRITICAL:** If you add a NEW node to fix a gap, you MUST ensure the new node has BOTH input and output connections. Do not create 'Orphan Nodes'.

        ### 🛠 Fix Strategies
        - **Case: MISSING_OUTPUT (Dead End)**
           - Action: Connect `targetNode` -> `node_end_point` OR `targetNode` -> `Next Logical Step`.
        - **Case: MISSING_INPUT (Unreachable)**
           - Action: Connect `Previous Logical Step` -> `targetNode`.
        - **Case: LOGIC_GAP (e.g., Missing Rejection)**
           - Action: Add a new edge from the Gateway with label 'Reject'.
           - Destination: Connect this edge to the original 'Submit' step (Loop Back) or a new 'Notify Rejection' task.

        ### Output Structure
        Return a JSON object with:
        - `nodes`: The FULL list of nodes (including your fixes).
        - `edges`: The FULL list of edges (including your fixes).
        - `fixDescription`: A clear summary (e.g., "Connected 'Manager Approval' to 'End Event' to resolve missing output.").
    """)
    @UserMessage("""
        PERFORM A SURGICAL FIX on this graph.

        [Current Graph Context]
        {{graphJson}}

        [Error to Fix]
        - **Type**: {{errorType}}
        - **Target Node**: {{targetNodeId}}
        - **Auditor's Suggestion**: {{suggestion}}
        
        **YOUR TASK:**
        1. Locate `{{targetNodeId}}`.
        2. Apply the necessary changes (Add Edge / Add Node) to resolve `{{errorType}}`.
        3. **VERIFY:** Does `{{targetNodeId}}` now have the required connections?
        4. Return the complete corrected graph.
    """)
    GraphStructure fixGraph(
            @V("graphJson") String graphJson,
            @V("errorType") String errorType,
            @V("targetNodeId") String targetNodeId,
            @V("suggestion") String suggestion
    );
}