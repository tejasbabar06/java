package com.assistant.dao;

import com.assistant.model.Summary;
import com.assistant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for AI Summary operations.
 * Enforces ownership checks to ensure only the student who owns the paper
 * can view or save summaries for it.
 */
public class SummaryDAO {

    /**
     * Saves an AI-generated summary into MySQL after confirming paper ownership.
     * 
     * @param summary Summary object to insert
     * @param userId  ID of the active student
     * @return true if successfully saved, false otherwise
     */
    public boolean saveSummary(Summary summary, int userId) {
        // 1. Verify that the user owns the paper
        if (!isPaperOwnedByUser(summary.getPaperId(), userId)) {
            System.err.println("[SummaryDAO] Authorization failed: User " + userId + " does not own paper " + summary.getPaperId());
            return false;
        }

        String sql = "INSERT INTO summaries (paper_id, summary_text) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, summary.getPaperId());
            pstmt.setString(2, summary.getSummaryText());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        summary.setSummaryId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[SummaryDAO] Error saving summary: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves all saved summaries for a specific paper owned by the user.
     * 
     * @param paperId The ID of the research paper
     * @param userId  The ID of the active student
     * @return List of Summary objects, sorted newest first
     */
    public List<Summary> getSummariesByPaperId(int paperId, int userId) {
        List<Summary> list = new ArrayList<>();
        String sql = "SELECT s.summary_id, s.paper_id, s.summary_text, s.created_at "
                   + "FROM summaries s "
                   + "JOIN papers p ON s.paper_id = p.paper_id "
                   + "WHERE s.paper_id = ? AND p.user_id = ? "
                   + "ORDER BY s.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paperId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Summary(
                            rs.getInt("summary_id"),
                            rs.getInt("paper_id"),
                            rs.getString("summary_text"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SummaryDAO] Error fetching summaries: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves the latest summary for a paper, or null if none saved.
     */
    public Summary getLatestSummary(int paperId, int userId) {
        List<Summary> list = getSummariesByPaperId(paperId, userId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Helper to verify that a paper belongs to the specified user.
     */
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
            System.err.println("[SummaryDAO] Error verifying ownership: " + e.getMessage());
        }
        return false;
    }
}
