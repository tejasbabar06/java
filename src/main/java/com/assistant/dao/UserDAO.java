package com.assistant.dao;

import com.assistant.model.User;
import com.assistant.util.DBConnection;
import com.assistant.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object (DAO) for User operations.
 * Handles database queries using JDBC PreparedStatements to prevent SQL injection.
 */
public class UserDAO {

    /**
     * Registers a new user in the MySQL database.
     * Hashes the password before storing and retrieves the generated user_id.
     * 
     * @param user User object containing name, email, and plain or hashed password
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerUser(User user) {
        String sql = "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail().trim().toLowerCase());
            pstmt.setString(3, user.getPasswordHash());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error registering user: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a user email is already registered in MySQL.
     * 
     * @param email The email address to check
     * @return true if email exists, false otherwise
     */
    public boolean isEmailRegistered(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error checking email existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds a user by their email address.
     * 
     * @param email The user's email
     * @return User object if found, null otherwise
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT user_id, name, email, password_hash, created_at FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            rs.getTimestamp("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error fetching user by email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Authenticates a user with email and plain-text password using BCrypt.
     * 
     * @param email         User's email
     * @param plainPassword User's plain password
     * @return Authenticated User object if valid, null if credentials fail
     */
    public User login(String email, String plainPassword) {
        if (email == null || plainPassword == null) {
            return null;
        }

        User user = getUserByEmail(email);
        if (user != null && PasswordUtil.checkPassword(plainPassword, user.getPasswordHash())) {
            return user;
        }
        return null;
    }
}
