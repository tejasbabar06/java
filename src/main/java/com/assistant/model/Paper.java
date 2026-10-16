package com.assistant.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Model class representing a Research Paper entity.
 * Holds metadata, file location, and extracted text content.
 */
public class Paper {
    private int paperId;
    private int userId;
    private String title;
    private String author;
    private String filePath;
    private String extractedText;
    private Timestamp uploadDate;

    // Default constructor
    public Paper() {
    }

    // Constructor for inserting new paper
    public Paper(int userId, String title, String author, String filePath, String extractedText) {
        this.userId = userId;
        this.title = title;
        this.author = author;
        this.filePath = filePath;
        this.extractedText = extractedText;
    }

    // Full constructor when retrieving from MySQL
    public Paper(int paperId, int userId, String title, String author, String filePath, String extractedText, Timestamp uploadDate) {
        this.paperId = paperId;
        this.userId = userId;
        this.title = title;
        this.author = author;
        this.filePath = filePath;
        this.extractedText = extractedText;
        this.uploadDate = uploadDate;
    }

    // Getters and Setters
    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return (author == null || author.trim().isEmpty()) ? "Unknown" : author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public Timestamp getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Timestamp uploadDate) {
        this.uploadDate = uploadDate;
    }

    /**
     * Formats timestamp into user-friendly date string for JavaFX TableView.
     */
    public String getFormattedDate() {
        if (uploadDate == null) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(uploadDate);
    }

    @Override
    public String toString() {
        return "Paper{" +
                "paperId=" + paperId +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", uploadDate=" + uploadDate +
                '}';
    }
}
