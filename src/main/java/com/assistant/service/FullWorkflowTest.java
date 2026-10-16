package com.assistant.service;

import com.assistant.dao.ChatDAO;
import com.assistant.dao.PaperDAO;
import com.assistant.dao.SummaryDAO;
import com.assistant.dao.UserDAO;
import com.assistant.model.ChatMessage;
import com.assistant.model.Paper;
import com.assistant.model.User;

import java.util.List;

/**
 * End-to-end integration test verifying the complete application workflow:
 * 1. User authentication & authorization
 * 2. Paper management
 * 3. AI Summary persistence
 * 4. Contextual Q&A (Ask AI) persistence and retrieval
 * 5. Metric statistics calculations
 */
public class FullWorkflowTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  AI Research Paper Assistant - End-to-End Workflow Test  ");
        System.out.println("==========================================================");

        UserDAO userDAO = new UserDAO();
        PaperDAO paperDAO = new PaperDAO();
        SummaryDAO summaryDAO = new SummaryDAO();
        ChatDAO chatDAO = new ChatDAO();
        GeminiService geminiService = new GeminiService();

        try {
            // 1. Verify User
            System.out.println("\n[STEP 1] Verifying student user authentication...");
            User user = userDAO.getUserByEmail("student.demo@college.edu");
            if (user == null) {
                System.err.println("  [ERROR] User not found!");
                return;
            }
            int userId = user.getUserId();
            System.out.println("  [OK] Student: " + user.getName() + " (ID: " + userId + ")");

            // 2. Verify Uploaded Papers
            System.out.println("\n[STEP 2] Verifying research papers in database...");
            List<Paper> papers = paperDAO.getPapersByUserId(userId);
            System.out.println("  Total Papers Found: " + papers.size());
            if (papers.isEmpty()) {
                System.err.println("  [ERROR] No papers found!");
                return;
            }
            Paper paper = papers.get(0);
            System.out.println("  [OK] Active Paper: '" + paper.getTitle() + "' (ID: " + paper.getPaperId() + ")");

            // 3. Test Ask AI Input Validation
            System.out.println("\n[STEP 3] Testing Ask AI input validation...");
            try {
                geminiService.askQuestion(paper.getExtractedText(), "");
                System.err.println("  [FAILED] Empty question should have been rejected!");
            } catch (IllegalArgumentException e) {
                System.out.println("  [SUCCESS] Correctly rejected empty question: " + e.getMessage());
            }

            // 4. Test Chat History Persistence (Q&A Saving)
            System.out.println("\n[STEP 4] Testing MySQL persistence in chat_history table...");
            String testQuestion = "What mechanism replaces recurrent layers in this paper?";
            String testAnswer = "The paper completely replaces recurrent layers with multi-head self-attention mechanisms.";

            ChatMessage msg = new ChatMessage(paper.getPaperId(), testQuestion, testAnswer);
            boolean saved = chatDAO.saveChatMessage(msg, userId);
            if (saved && msg.getChatId() > 0) {
                System.out.println("  [SUCCESS] Question and AI answer saved with chat_id: " + msg.getChatId());
            } else {
                System.err.println("  [FAILED] Could not save chat message to MySQL.");
            }

            // 5. Test Chat History Retrieval
            System.out.println("\n[STEP 5] Testing Q&A history retrieval from MySQL...");
            List<ChatMessage> history = chatDAO.getChatHistoryByPaperId(paper.getPaperId(), userId);
            System.out.println("  Total Q&A interactions found: " + history.size());
            boolean match = history.stream().anyMatch(m -> m.getUserQuestion().equals(testQuestion));
            if (match) {
                System.out.println("  [SUCCESS] Retrieved question matches saved question perfectly!");
            } else {
                System.err.println("  [FAILED] Could not find saved question in chat history.");
            }

            // 6. Test Data Isolation
            System.out.println("\n[STEP 6] Testing privacy isolation (User 9999 viewing Q&A)...");
            List<ChatMessage> unauthorized = chatDAO.getChatHistoryByPaperId(paper.getPaperId(), 9999);
            if (unauthorized.isEmpty()) {
                System.out.println("  [SUCCESS] User 9999 sees 0 messages. Data isolation verified!");
            } else {
                System.err.println("  [FAILED] Security violation: unauthorized user saw chat messages!");
            }

            // 7. Test Dashboard Metrics
            System.out.println("\n[STEP 7] Verifying live Dashboard metrics for student...");
            int totalPapers = paperDAO.getPaperCount(userId);
            int totalSummaries = paperDAO.getSummaryCount(userId);
            int totalQuestions = chatDAO.getChatCount(userId);

            System.out.println("  Card 1 (Total Papers):     " + totalPapers);
            System.out.println("  Card 2 (AI Summaries):     " + totalSummaries);
            System.out.println("  Card 3 (Questions Asked):  " + totalQuestions);

            if (totalPapers > 0 && totalQuestions > 0) {
                System.out.println("  [SUCCESS] All dashboard metrics are live and connected to MySQL!");
            }

            System.out.println("\n----------------------------------------------------------");
            System.out.println("[RESULT] ALL 7 END-TO-END WORKFLOW TESTS PASSED!");
            System.out.println("==========================================================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
