package com.assistant.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Model class representing a Q&A interaction between student and Gemini AI.
 */
public class ChatMessage {
    private int chatId;
    private int paperId;
    private String userQuestion;
    private String aiAnswer;
    private Timestamp createdAt;

    public ChatMessage() {
    }

    public ChatMessage(int paperId, String userQuestion, String aiAnswer) {
        this.paperId = paperId;
        this.userQuestion = userQuestion;
        this.aiAnswer = aiAnswer;
    }

    public ChatMessage(int chatId, int paperId, String userQuestion, String aiAnswer, Timestamp createdAt) {
        this.chatId = chatId;
        this.paperId = paperId;
        this.userQuestion = userQuestion;
        this.aiAnswer = aiAnswer;
        this.createdAt = createdAt;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }

    public String getAiAnswer() {
        return aiAnswer;
    }

    public void setAiAnswer(String aiAnswer) {
        this.aiAnswer = aiAnswer;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedTime() {
        if (createdAt == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(createdAt);
    }
}
