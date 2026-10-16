package com.assistant.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Model representing an AI-generated summary of a research paper.
 */
public class Summary {
    private int summaryId;
    private int paperId;
    private String summaryText;
    private Timestamp createdAt;

    public Summary() {
    }

    public Summary(int paperId, String summaryText) {
        this.paperId = paperId;
        this.summaryText = summaryText;
    }

    public Summary(int summaryId, int paperId, String summaryText, Timestamp createdAt) {
        this.summaryId = summaryId;
        this.paperId = paperId;
        this.summaryText = summaryText;
        this.createdAt = createdAt;
    }

    public int getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(int summaryId) {
        this.summaryId = summaryId;
    }

    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedDate() {
        if (createdAt == null) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(createdAt);
    }

    @Override
    public String toString() {
        return "Summary{" +
                "summaryId=" + summaryId +
                ", paperId=" + paperId +
                ", createdAt=" + createdAt +
                '}';
    }
}
