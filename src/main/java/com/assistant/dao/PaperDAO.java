package com.assistant.dao;

import com.assistant.model.Paper;
import com.assistant.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Research Paper operations.
 * Executes parameterized queries using JDBC PreparedStatement to ensure
 * data isolation per student user and prevent SQL injection.
 */
public class PaperDAO {

    /**
     * Inserts a new research paper into the database.
     * 
     * @param paper Paper model containing paper details
     * @return true if successfully inserted, false otherwise
     */
    public boolean addPaper(Paper paper) {
        String sql = "INSERT INTO papers (user_id, title, author, file_path, extracted_text) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, paper.getUserId());
            pstmt.setString(2, paper.getTitle());
            pstmt.setString(3, paper.getAuthor());
            pstmt.setString(4, paper.getFilePath());
            pstmt.setString(5, paper.getExtractedText());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        paper.setPaperId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[PaperDAO] Error inserting paper: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves all papers uploaded by a specific user, sorted newest first.
     * 
     * @param userId The ID of the currently logged-in student
     * @return List of Paper objects
     */
    public List<Paper> getPapersByUserId(int userId) {
        List<Paper> list = new ArrayList<>();
        String sql = "SELECT paper_id, user_id, title, author, file_path, extracted_text, upload_date "
                   + "FROM papers WHERE user_id = ? ORDER BY upload_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(extractPaperFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaperDAO] Error fetching user papers: " + e.getMessage());
        }
        return list;
    }

    /**
     * Searches papers belonging to the user matching title or author keyword.
     * 
     * @param userId  Current user ID
     * @param keyword Search term
     * @return Matching list of papers
     */
    public List<Paper> searchPapers(int userId, String keyword) {
        List<Paper> list = new ArrayList<>();
        String sql = "SELECT paper_id, user_id, title, author, file_path, extracted_text, upload_date "
                   + "FROM papers WHERE user_id = ? AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ?) "
                   + "ORDER BY upload_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            pstmt.setInt(1, userId);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(extractPaperFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaperDAO] Error searching papers: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves a single paper by paperId and userId (ensures authorization isolation).
     */
    public Paper getPaperById(int paperId, int userId) {
        String sql = "SELECT paper_id, user_id, title, author, file_path, extracted_text, upload_date "
                   + "FROM papers WHERE paper_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paperId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractPaperFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaperDAO] Error fetching paper by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Deletes a paper record by paper_id and user_id (for authorization safety).
     */
    public boolean deletePaper(int paperId, int userId) {
        String sql = "DELETE FROM papers WHERE paper_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paperId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PaperDAO] Error deleting paper: " + e.getMessage());
        }
        return false;
    }

    /**
     * Returns total paper count for the user.
     */
    public int getPaperCount(int userId) {
        return getCountQuery("SELECT COUNT(*) FROM papers WHERE user_id = ?", userId);
    }

    /**
     * Returns total AI summaries generated for the user's papers.
     */
    public int getSummaryCount(int userId) {
        return getCountQuery("SELECT COUNT(*) FROM summaries s JOIN papers p ON s.paper_id = p.paper_id WHERE p.user_id = ?", userId);
    }

    /**
     * Returns total notes created across the user's papers.
     */
    public int getNotesCount(int userId) {
        return getCountQuery("SELECT COUNT(*) FROM notes n JOIN papers p ON n.paper_id = p.paper_id WHERE p.user_id = ?", userId);
    }

    private int getCountQuery(String sql, int userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaperDAO] Error fetching count: " + e.getMessage());
        }
        return 0;
    }

    private Paper extractPaperFromResultSet(ResultSet rs) throws SQLException {
        return new Paper(
                rs.getInt("paper_id"),
                rs.getInt("user_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("file_path"),
                rs.getString("extracted_text"),
                rs.getTimestamp("upload_date")
        );
    }
}
