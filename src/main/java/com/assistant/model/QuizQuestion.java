package com.assistant.model;

/**
 * Model class representing a multiple-choice quiz question generated from a research paper.
 */
public class QuizQuestion {

    private int quizId;
    private int paperId;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer; // "A", "B", "C", or "D"
    private String explanation;
    private String userSelectedAnswer; // "A", "B", "C", "D", or null

    public QuizQuestion() {
    }

    public QuizQuestion(String question, String optionA, String optionB, String optionC, String optionD, String correctAnswer, String explanation) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = (correctAnswer != null) ? correctAnswer.trim().toUpperCase() : "";
        this.explanation = explanation;
    }

    public QuizQuestion(int quizId, int paperId, String question, String optionA, String optionB, String optionC, String optionD, String correctAnswer, String explanation) {
        this(question, optionA, optionB, optionC, optionD, correctAnswer, explanation);
        this.quizId = quizId;
        this.paperId = paperId;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public int getPaperId() {
        return paperId;
    }

    public void setPaperId(int paperId) {
        this.paperId = paperId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = (correctAnswer != null) ? correctAnswer.trim().toUpperCase() : "";
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getUserSelectedAnswer() {
        return userSelectedAnswer;
    }

    public void setUserSelectedAnswer(String userSelectedAnswer) {
        this.userSelectedAnswer = userSelectedAnswer;
    }

    public boolean isAnswered() {
        return userSelectedAnswer != null && !userSelectedAnswer.trim().isEmpty();
    }

    public boolean isCorrect() {
        return isAnswered() && userSelectedAnswer.equalsIgnoreCase(correctAnswer);
    }

    public String getOptionText(String key) {
        if (key == null) return "";
        return switch (key.trim().toUpperCase()) {
            case "A" -> optionA;
            case "B" -> optionB;
            case "C" -> optionC;
            case "D" -> optionD;
            default -> "";
        };
    }
}
