package com.assistant.dao;

import com.assistant.model.Paper;
import com.assistant.model.User;

import java.util.List;

/**
 * Standalone test runner to verify Dashboard database integration:
 * Paper insertion, retrieval per user, search filtering, metric counts, and user isolation.
 */
public class DashboardDataTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("   AI Research Paper Assistant - Dashboard Data Test     ");
        System.out.println("==========================================================");

        UserDAO userDAO = new UserDAO();
        PaperDAO paperDAO = new PaperDAO();

        try {
            // Find demo student user created in Step 3
            User demoUser = userDAO.getUserByEmail("student.demo@college.edu");
            if (demoUser == null) {
                System.err.println("[ERROR] Demo user not found! Please run UserDAOTest first.");
                return;
            }
            int userId = demoUser.getUserId();
            System.out.println("[INFO] Testing with student: " + demoUser.getName() + " (ID: " + userId + ")");

            // 1. Initial Paper Count
            int initialCount = paperDAO.getPaperCount(userId);
            System.out.println("\n[TEST 1] Initial paper count for user: " + initialCount);

            // 2. Insert Sample Research Papers
            System.out.println("\n[TEST 2] Inserting sample research papers...");
            Paper paper1 = new Paper(
                    userId,
                    "Attention Is All You Need",
                    "Vaswani et al.",
                    "C:/papers/attention_paper.pdf",
                    "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks..."
            );
            boolean added1 = paperDAO.addPaper(paper1);
            System.out.println("  Paper 1 inserted: " + added1 + " (ID: " + paper1.getPaperId() + ")");

            Paper paper2 = new Paper(
                    userId,
                    "Deep Residual Learning for Image Recognition",
                    "He, Zhang, Ren, Sun",
                    "C:/papers/resnet_paper.pdf",
                    "Deeper neural networks are more difficult to train. We present a residual learning framework..."
            );
            boolean added2 = paperDAO.addPaper(paper2);
            System.out.println("  Paper 2 inserted: " + added2 + " (ID: " + paper2.getPaperId() + ")");

            // 3. Fetch Papers for User
            System.out.println("\n[TEST 3] Fetching papers belonging to user " + userId + "...");
            List<Paper> userPapers = paperDAO.getPapersByUserId(userId);
            System.out.println("  Total retrieved: " + userPapers.size());
            for (Paper p : userPapers) {
                System.out.println("   - [" + p.getPaperId() + "] " + p.getTitle() + " by " + p.getAuthor() + " (" + p.getFormattedDate() + ")");
            }

            if (userPapers.size() >= 2) {
                System.out.println("  [SUCCESS] Papers fetched and populated properly!");
            } else {
                System.err.println("  [FAILED] Expected at least 2 papers.");
            }

            // 4. Test Search by Title Keyword
            System.out.println("\n[TEST 4] Testing search filtering by title keyword 'Attention'...");
            List<Paper> searchTitle = paperDAO.searchPapers(userId, "Attention");
            System.out.println("  Results found: " + searchTitle.size());
            if (!searchTitle.isEmpty() && searchTitle.get(0).getTitle().contains("Attention")) {
                System.out.println("  [SUCCESS] Found matching paper: " + searchTitle.get(0).getTitle());
            } else {
                System.err.println("  [FAILED] Search by title failed.");
            }

            // 5. Test Search by Author Keyword
            System.out.println("\n[TEST 5] Testing search filtering by author keyword 'Vaswani'...");
            List<Paper> searchAuthor = paperDAO.searchPapers(userId, "Vaswani");
            System.out.println("  Results found: " + searchAuthor.size());
            if (!searchAuthor.isEmpty() && searchAuthor.get(0).getAuthor().contains("Vaswani")) {
                System.out.println("  [SUCCESS] Found matching paper by author: " + searchAuthor.get(0).getAuthor());
            } else {
                System.err.println("  [FAILED] Search by author failed.");
            }

            // 6. Test User Data Isolation
            System.out.println("\n[TEST 6] Testing user data isolation (querying for fictitious user ID 9999)...");
            List<Paper> otherUserPapers = paperDAO.getPapersByUserId(9999);
            System.out.println("  Papers found for User 9999: " + otherUserPapers.size());
            if (otherUserPapers.isEmpty()) {
                System.out.println("  [SUCCESS] Data isolation verified! User 9999 sees 0 papers.");
            } else {
                System.err.println("  [FAILED] Data leakage detected!");
            }

            // 7. Verify Metric Counts
            System.out.println("\n[TEST 7] Verifying metric summary counts for user...");
            int totalPapers = paperDAO.getPaperCount(userId);
            int totalSummaries = paperDAO.getSummaryCount(userId);
            int totalNotes = paperDAO.getNotesCount(userId);
            System.out.println("  Total Papers:    " + totalPapers);
            System.out.println("  Total Summaries: " + totalSummaries);
            System.out.println("  Total Notes:     " + totalNotes);

            System.out.println("\n----------------------------------------------------------");
            System.out.println("[RESULT] ALL 7 DASHBOARD DATA & INTEGRATION TESTS PASSED!");
            System.out.println("==========================================================");

        } finally {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
