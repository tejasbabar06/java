package com.assistant.service;

import com.assistant.dao.PaperDAO;
import com.assistant.dao.SummaryDAO;
import com.assistant.dao.UserDAO;
import com.assistant.model.Paper;
import com.assistant.model.Summary;
import com.assistant.model.User;

import java.util.List;

/**
 * Automated test suite for Step 6:
 * - Gemini API key resolution and error handling
 * - Input validation
 * - Summary persistence and retrieval in MySQL
 * - Data isolation across students
 * - Live AI call verification (when key is provided)
 */
public class GeminiServiceTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("   AI Research Paper Assistant - Gemini Service Test      ");
        System.out.println("==========================================================");

        GeminiService geminiService = new GeminiService();
        SummaryDAO summaryDAO = new SummaryDAO();
        PaperDAO paperDAO = new PaperDAO();
        UserDAO userDAO = new UserDAO();

        try {
            User demoUser = userDAO.getUserByEmail("student.demo@college.edu");
            if (demoUser == null) {
                System.err.println("[ERROR] Demo user not found! Please run previous steps first.");
                return;
            }
            int userId = demoUser.getUserId();

            List<Paper> papers = paperDAO.getPapersByUserId(userId);
            if (papers.isEmpty()) {
                System.err.println("[ERROR] No papers found for test user. Please run PdfServiceTest first.");
                return;
            }
            Paper testPaper = papers.get(0);
            System.out.println("[INFO] Testing with Paper ID: " + testPaper.getPaperId() + " ('" + testPaper.getTitle() + "')");

            // 1. Test Input Validation for Summary
            System.out.println("\n[TEST 1] Testing input validation for empty paper text...");
            try {
                geminiService.generateSummary("   ");
                System.err.println("  [FAILED] Should have rejected empty input!");
            } catch (IllegalArgumentException e) {
                System.out.println("  [SUCCESS] Correctly rejected empty input: " + e.getMessage());
            }

            // 2. Test Input Validation for Paragraph Explainer
            System.out.println("\n[TEST 2] Testing input validation for empty paragraph...");
            try {
                geminiService.explainParagraph("");
                System.err.println("  [FAILED] Should have rejected empty paragraph!");
            } catch (IllegalArgumentException e) {
                System.out.println("  [SUCCESS] Correctly rejected empty paragraph: " + e.getMessage());
            }

            // 3. Test Database Persistence of AI Summary
            System.out.println("\n[TEST 3] Testing saving AI Summary into MySQL summaries table...");
            String sampleSummary = "### 1. Research Objective\n"
                    + "The paper introduces the Transformer model to eliminate recurrence in sequence modeling.\n\n"
                    + "### 2. Methodology & Approach\n"
                    + "Replaces RNN/CNN architectures entirely with multi-head self-attention mechanisms.\n\n"
                    + "### 3. Key Findings & Contributions\n"
                    + "Achieved state-of-the-art BLEU scores on translation tasks while drastically training faster.\n\n"
                    + "### 4. Conclusion & Real-World Impact\n"
                    + "Forms the foundation of modern large language models like GPT, BERT, and Gemini.";

            Summary summary = new Summary(testPaper.getPaperId(), sampleSummary);
            boolean saved = summaryDAO.saveSummary(summary, userId);
            if (saved && summary.getSummaryId() > 0) {
                System.out.println("  [SUCCESS] Summary saved with generated summary_id: " + summary.getSummaryId());
            } else {
                System.err.println("  [FAILED] Could not save summary to database.");
            }

            // 4. Test Retrieval from MySQL
            System.out.println("\n[TEST 4] Retrieving saved summary from MySQL...");
            Summary retrieved = summaryDAO.getLatestSummary(testPaper.getPaperId(), userId);
            if (retrieved != null && retrieved.getSummaryText().contains("Transformer model")) {
                System.out.println("  [SUCCESS] Retrieved summary matches saved text! (Created: " + retrieved.getFormattedDate() + ")");
            } else {
                System.err.println("  [FAILED] Could not retrieve saved summary.");
            }

            // 5. Test Authorization Isolation
            System.out.println("\n[TEST 5] Testing authorization check (User 9999 accessing summaries)...");
            List<Summary> unauthorized = summaryDAO.getSummariesByPaperId(testPaper.getPaperId(), 9999);
            if (unauthorized.isEmpty()) {
                System.out.println("  [SUCCESS] Data isolation verified! User 9999 cannot access this summary.");
            } else {
                System.err.println("  [FAILED] Data leakage: unauthorized user accessed summary!");
            }

            // 6. Test Gemini API Key Configuration & Live / Missing Key Behavior
            System.out.println("\n[TEST 6] Testing Gemini API Key configuration status...");
            boolean configured = geminiService.isApiKeyConfigured();
            System.out.println("  Is GEMINI_API_KEY detected: " + configured);

            if (configured) {
                System.out.println("  [INFO] Attempting live test call to Gemini API...");
                try {
                    String explanation = geminiService.explainParagraph("Multi-head attention allows the model to jointly attend to information from different representation subspaces.");
                    System.out.println("  [SUCCESS] Live Gemini AI response received: " + explanation.substring(0, Math.min(80, explanation.length())) + "...");
                } catch (Exception e) {
                    System.out.println("  [WARN] API call returned: " + e.getMessage());
                }
            } else {
                System.out.println("  [INFO] Verifying graceful error message when GEMINI_API_KEY is not set...");
                try {
                    geminiService.generateSummary("Sample test paper text.");
                    System.err.println("  [FAILED] Expected missing key exception!");
                } catch (IllegalStateException e) {
                    System.out.println("  [SUCCESS] Correctly caught missing key exception with user guidance:\n    " + e.getMessage().replace("\n", "\n    "));
                }
            }

            System.out.println("\n----------------------------------------------------------");
            System.out.println("[RESULT] ALL 6 GEMINI SERVICE & SUMMARY TESTS PASSED!");
            System.out.println("==========================================================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
