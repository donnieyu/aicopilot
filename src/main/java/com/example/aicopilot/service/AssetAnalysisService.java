package com.example.aicopilot.service;

import com.example.aicopilot.dto.analysis.AssetAnalysisResponse;
import com.example.aicopilot.dto.definition.ProcessDefinition; // [New] Import
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetAnalysisService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    /**
     * Analyzes the uploaded file to extract a COMPLETE Process Definition.
     * This bypasses the Outliner stage and prepares data for direct Map Transformation.
     */
    public ProcessDefinition analyzeAssetToDefinition(MultipartFile file) {
        try {
            String mimeType = file.getContentType();
            String filename = file.getOriginalFilename();
            log.info("Analyzing asset for direct definition: {} ({})", filename, mimeType);

            UserMessage userMessage;

            if (mimeType != null && mimeType.startsWith("image/")) {
                // 1. Image Processing (Vision)
                String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
                userMessage = UserMessage.from(
                        TextContent.from(getBPMNAnalysisPrompt() + "\n\n[Instruction]\nAnalyze the attached BPMN Process Map image. Extract the exact flow structure into the requested JSON format."),
                        ImageContent.from(base64Image, mimeType)
                );
            } else {
                // 2. Text/Document Processing (Fallback for non-images)
                String extractedText = extractTextFromFile(file);
                userMessage = UserMessage.from(
                        getBPMNAnalysisPrompt() + "\n\n[Extracted File Content]\n" + extractedText
                );
            }

            // Call AI
            Response<AiMessage> response = chatLanguageModel.generate(userMessage);
            String jsonResponse = response.content().text();

            // Parse JSON (Clean up markdown code blocks if present)
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

            // Map directly to ProcessDefinition (Topic + Steps)
            return objectMapper.readValue(jsonResponse, ProcessDefinition.class);

        } catch (Exception e) {
            log.error("Failed to analyze asset to definition", e);
            throw new RuntimeException("Asset analysis failed: " + e.getMessage());
        }
    }

    /**
     * Legacy method for simple text extraction (Outliner compatibility).
     */
    public AssetAnalysisResponse analyzeAsset(MultipartFile file) {
        return null;
    }

    // [Updated] Highly Specialized Prompt for BPMN/Flowchart Image Reverse Engineering with Strict Swimlane Detection
    private String getBPMNAnalysisPrompt() {
        return """
            You are an expert **BPMN 2.0 Reverse Engineer** and **Process Architect**.
            Your goal is to digitize a specific Process Map Image into a structured `ProcessDefinition` JSON with 100% fidelity.

            ### 🎯 MISSION: "Vision to Structured Data with Swimlanes"
            Extract the business logic from the image into a linear list of steps.
            **CRITICAL:** You MUST identify **Swimlanes (Pools/Lanes)** in the image to determine the correct `role` for each step. This is the primary method for role assignment.
            Since the output is a linear list, you MUST describe the flow connections (branching, looping) inside the `description` field so the downstream system can reconstruct the graph.

            ### 1. Visual Decoding Rules (Vision Analysis)
            - **Identify Swimlanes First:** Look for large horizontal or vertical containers (boxes) that divide the entire chart. Read the header text for each container (e.g., "Employee", "Manager", "HR", "Finance"). These headers are the **Roles**.
            - **Map Steps to Roles:** For every Task or Gateway node, visually identify which Swimlane container it resides in. You MUST use that Swimlane's header text exactly as the `role` for that step.
            - **Ignore Start/End Nodes:** Do NOT create steps for visual 'Start Event' (Circles) or 'End Event'. Only list the Tasks and Gateways between them.
            - **Text Extraction:** Read the label inside each shape exactly for the `name`.

            ### 2. Logic & topology Extraction (Crucial)
            - **Gateways (◇ Diamonds):**
              - Mark `type: "DECISION"`.
              - **IMPORTANT:** In the `description`, explicitly state the conditions found on the outgoing arrows.
              - *Example:* "Exclusive Gateway. If 'Approved' -> Go to Payment. If 'Rejected' -> Return to Request."
            - **Rejection/Loops:**
              - If an arrow goes BACK to a previous step, mention this in the `description`.
              - *Example:* "Manager review task. If rejected, the process loops back to the 'Submit Expense' step."

            ### 3. Output Data Structure (Strict JSON)
            Return ONLY the raw JSON matching `ProcessDefinition`. No markdown.

            {
              "topic": "Expense Reimbursement Process",
              "steps": [
                {
                  "stepId": "1",
                  "name": "Submit Expense",
                  "role": "Employee", // MUST be extracted from the "Employee" Swimlane header
                  "description": "Employee fills out the expense form and uploads receipts.",
                  "type": "ACTION"
                },
                {
                  "stepId": "2",
                  "name": "Manager Approval",
                  "role": "Manager", // MUST be extracted from the "Manager" Swimlane header
                  "description": "Manager reviews the cost. If 'Approved' proceeds to HR. If 'Rejected', it loops back to 'Submit Expense' for correction.",
                  "type": "DECISION"
                },
                {
                  "stepId": "3",
                  "name": "Process Payment",
                  "role": "HR", // MUST be extracted from the "HR" Swimlane header
                  "description": "HR system executes the transfer via API.",
                  "type": "ACTION"
                }
              ]
            }
            """;
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }

        try (InputStream is = file.getInputStream()) {
            switch (extension) {
                case "xlsx":
                case "xls":
                    return parseExcel(is);
                case "pdf":
                    return parsePdf(is);
                case "csv":
                case "txt":
                case "md":
                    return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                default:
                    throw new IllegalArgumentException("Unsupported file type: " + extension);
            }
        }
    }

    private String parseExcel(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0); // Read first sheet
            for (Row row : sheet) {
                for (Cell cell : row) {
                    sb.append(cell.toString()).append("\t");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String parsePdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}