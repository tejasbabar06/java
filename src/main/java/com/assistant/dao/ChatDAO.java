package com.assistant.dao;

import com.assistant.model.ChatMessage;
import com.assistant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Chat & Q&A history.
 * Interacts with the chat_history MySQL table with user authorization checks.
 */
public class ChatDAO {

    /**
     * Saves a Q&A interaction into MySQL chat_history table.
     */
    public boolean saveChatMessage(ChatMessage msg, int userId) {
        if (!isPaperOwnedByUser(msg.getPaperId(), userId)) {
            System.err.println("[ChatDAO] Authorization failed: User " + userId + " does not own paper " + msg.getPaperId());
            return false;
        }

        String sql = "INSERT INTO chat_history (paper_id, user_question, ai_answer) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, msg.getPaperId());
            pstmt.setString(2, msg.getUserQuestion());
            pstmt.setString(3, msg.getAiAnswer());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        msg.setChatId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ChatDAO] Error saving chat message: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves previous questions and answers for a specific paper.
     */
    public List<ChatMessage> getChatHistoryByPaperId(int paperId, int userId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT c.chat_id, c.paper_id, c.user_question, c.ai_answer, c.created_at "
                   + "FROM chat_history c "
                   + "JOIN papers p ON c.paper_id = p.paper_id "
                   + "WHERE c.paper_id = ? AND p.user_id = ? "
                   + "ORDER BY c.created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paperId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChatMessage(
                            rs.getInt("chat_id"),
                            rs.getInt("paper_id"),
                            rs.getString("user_question"),
                            rs.getString("ai_answer"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChatDAO] Error fetching chat history: " + e.getMessage());
        }
        return list;
    }

    /**
     * Returns total number of questions asked by the user across all their papers.
     */
    public int getChatCount(int userId) {
        String sql = "SELECT COUNT(*) FROM chat_history c JOIN papers p ON c.paper_id = p.paper_id WHERE p.user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChatDAO] Error fetching chat count: " + e.getMessage());
        }
        return 0;
    }

    private boolean isPaperOwnedByUser(int paperId, int userId) {
        String sql = "SELECT 1 FROM papers WHERE paper_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paperId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ChatDAO] Error checking paper ownership: " + e.getMessage());
        }
        return false;
    }
}
