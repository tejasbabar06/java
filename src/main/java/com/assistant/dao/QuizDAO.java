package com.assistant.dao;

import com.assistant.model.QuizQuestion;
import com.assistant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing multiple-choice quiz questions.
 * Handles persistence to the MySQL 'quiz' table with user ownership verification.
 */
public class QuizDAO {

    /**
     * Saves a list of generated quiz questions for a research paper.
     * Replaces previous quiz records for this paper.
     */
    public boolean saveQuizQuestions(int paperId, List<QuizQuestion> questions, int userId) {
        if (!isPaperOwnedByUser(paperId, userId)) {
            System.err.println("[QuizDAO] Authorization failed: User " + userId + " does not own paper " + paperId);
            return false;
        }

        if (questions == null || questions.isEmpty()) {
            return false;
        }

        String deleteSql = "DELETE FROM quiz WHERE paper_id = ?";
        String insertSql = "INSERT INTO quiz (paper_id, question, option_a, option_b, option_c, option_d, correct_answer) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, paperId);
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (QuizQuestion q : questions) {
                    insertStmt.setInt(1, paperId);
                    insertStmt.setString(2, q.getQuestion());
                    insertStmt.setString(3, q.getOptionA());
                    insertStmt.setString(4, q.getOptionB());
                    insertStmt.setString(5, q.getOptionC());
                    insertStmt.setString(6, q.getOptionD());
                    insertStmt.setString(7, q.getCorrectAnswer());
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            System.err.println("[QuizDAO] Error saving quiz questions: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves saved quiz questions for a research paper.
     */
    public List<QuizQuestion> getQuizByPaperId(int paperId, int userId) {
        List<QuizQuestion> list = new ArrayList<>();
        if (!isPaperOwnedByUser(paperId, userId)) {
            return list;
        }

        String sql = "SELECT quiz_id, paper_id, question, option_a, option_b, option_c, option_d, correct_answer "
                   + "FROM quiz WHERE paper_id = ? ORDER BY quiz_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paperId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    QuizQuestion q = new QuizQuestion(
                            rs.getInt("quiz_id"),
                            rs.getInt("paper_id"),
                            rs.getString("question"),
                            rs.getString("option_a"),
                            rs.getString("option_b"),
                            rs.getString("option_c"),
                            rs.getString("option_d"),
                            rs.getString("correct_answer"),
                            "" // Explanation is generated at quiz creation time
                    );
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            System.err.println("[QuizDAO] Error fetching quiz questions: " + e.getMessage());
        }
        return list;
    }

    private boolean isPaperOwnedByUser(int paperId, int userId) {
        String sql = "SELECT COUNT(*) FROM papers WHERE paper_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paperId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[QuizDAO] Error verifying paper ownership: " + e.getMessage());
        }
        return false;
    }
}
