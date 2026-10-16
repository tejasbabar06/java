package com.assistant.model;

import java.sql.Timestamp;

/**
 * Model class representing a User entity in the system.
 * Demonstrates OOP encapsulation with private fields, getters, setters, and constructors.
 */
public class User {
    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private Timestamp createdAt;

    // Default Constructor
    public User() {
    }

    // Constructor for new user registration (before database assigns an auto-increment ID)
    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Full Constructor (when retrieving user records from MySQL)
    public User(int userId, String name, String email, String passwordHash, Timestamp createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
