package com.assistant.controller;

import com.assistant.dao.ChatDAO;
import com.assistant.dao.QuizDAO;
import com.assistant.dao.SummaryDAO;
import com.assistant.model.ChatMessage;
import com.assistant.model.Flashcard;
import com.assistant.model.Paper;
import com.assistant.model.QuizQuestion;
import com.assistant.model.Summary;
import com.assistant.model.User;
import com.assistant.service.GeminiService;
import com.assistant.service.PdfService;
import com.assistant.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Controller for viewing research papers and interacting with Google Gemini AI.
 * Implements paper viewing, AI summaries, paragraph explanations,
 * contextual Question Answering (Ask AI), Interactive Quizzes, and Study Flashcards
 * using non-blocking background tasks.
 */
public class PaperViewerController {

    private final Paper paper;
    private final PdfService pdfService = new PdfService();
    private final GeminiService geminiService = new GeminiService();
    private final SummaryDAO summaryDAO = new SummaryDAO();
    private final ChatDAO chatDAO = new ChatDAO();
    private final QuizDAO quizDAO = new QuizDAO();

    private BorderPane view;
    private TabPane tabPane;

    // AI Summary Tab components
    private TextArea summaryOutputArea;
    private Button generateSummaryBtn;
    private Button saveSummaryBtn;
    private ProgressIndicator summarySpinner;
    private Label summaryStatusLabel;

    // Explain Paragraph Tab components
    private TextArea paragraphInputArea;
    private TextArea explanationOutputArea;
    private Button explainBtn;
    private ProgressIndicator explainSpinner;
    private Label explainStatusLabel;

    // Ask AI Tab components
    private TextArea chatDisplayArea;
    private TextField questionInputField;
    private Button askQuestionBtn;
    private ProgressIndicator askSpinner;
    private Label askStatusLabel;

    // Quiz Tab components & state
    private List<QuizQuestion> currentQuizQuestions = new ArrayList<>();
    private int currentQuizIndex = 0;
    private ComboBox<String> quizCountCombo;
    private Button generateQuizBtn;
    private Button saveQuizBtn;
    private ProgressIndicator quizSpinner;
    private Label quizStatusLabel;
    private StackPane quizViewStack;
    private VBox quizEmptyPlaceholder;
    private VBox quizActiveBox;
    private VBox quizResultBox;
    private Label quizProgressLabel;
    private ProgressBar quizProgressBar;
    private Label quizQuestionLabel;
    private ToggleGroup quizToggleGroup;
    private RadioButton optARadio;
    private RadioButton optBRadio;
    private RadioButton optCRadio;
    private RadioButton optDRadio;
    private VBox optABox, optBBox, optCBox, optDBox;
    private Button prevQuizBtn;
    private Button nextQuizBtn;
    private Button submitQuizBtn;

    // Flashcards Tab components & state
    private List<Flashcard> currentFlashcards = new ArrayList<>();
    private int currentFlashcardIndex = 0;
    private boolean isFlashcardFlipped = false;
    private Button generateFlashcardsBtn;
    private Button shuffleFlashcardsBtn;
    private ProgressIndicator flashcardSpinner;
    private Label flashcardStatusLabel;
    private StackPane flashcardViewStack;
    private VBox flashcardEmptyPlaceholder;
    private VBox flashcardActiveBox;
    private Label flashcardCounterLabel;
    private ProgressBar flashcardProgressBar;
    private VBox flashcardCardView;
    private Label flashcardSideBadge;
    private Label flashcardIndexPill;
    private Label flashcardTextLabel;
    private Label flashcardHintLabel;
    private Button prevFlashcardBtn;
    private Button flipFlashcardBtn;
    private Button nextFlashcardBtn;

    public PaperViewerController(Paper paper) {
        this.paper = paper;
        createView();
        loadExistingSummary();
        loadExistingChatHistory();
        loadExistingQuiz();
    }

    public Parent getView() {
        return view;
    }

    private void createView() {
        view = new BorderPane();
        view.setStyle("-fx-background-color: #F8FAFC;");

        // 1. Top Header Area
        VBox headerBox = new VBox(12);
        headerBox.setPadding(new Insets(18, 28, 14, 28));
        headerBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1px 0;");

        Button backBtn = new Button("← Back to Dashboard");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> ScreenManager.showDashboardScreen());

        Text titleText = new Text((paper != null) ? paper.getTitle() : "Research Paper Viewer");
        titleText.getStyleClass().add("title-text");

        String author = (paper != null && paper.getAuthor() != null) ? paper.getAuthor() : "Unknown Author";
        String date = (paper != null) ? paper.getFormattedDate() : "N/A";
        String paperText = (paper != null && paper.getExtractedText() != null) ? paper.getExtractedText() : "";
        int wordCount = pdfService.calculateWordCount(paperText);

        Label metadataLabel = new Label("Author: " + author + "   •   Uploaded: " + date + "   •   " + wordCount + " words");
        metadataLabel.getStyleClass().add("subtitle-text");

        headerBox.getChildren().addAll(backBtn, titleText, metadataLabel);
        view.setTop(headerBox);

        // 2. TabPane for Content & AI Tools
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPadding(new Insets(16, 28, 20, 28));

        // Tab 1: Extracted Text
        Tab textTab = new Tab("1. Paper Extracted Content", createTextTab(paperText));

        // Tab 2: AI Summary
        Tab summaryTab = new Tab("2. AI Summary", createSummaryTab(paperText));

        // Tab 3: Explain Paragraph
        Tab explainTab = new Tab("3. Explain Paragraph", createExplainTab());

        // Tab 4: Ask AI Questions (Q&A)
        Tab askAiTab = new Tab("4. Ask AI (Q&A)", createAskAiTab(paperText));

        // Tab 5: Interactive Quiz
        Tab quizTab = new Tab("5. Quiz", createQuizTab(paperText));

        // Tab 6: Flashcards
        Tab flashcardsTab = new Tab("6. Flashcards", createFlashcardsTab(paperText));

        tabPane.getTabs().addAll(textTab, summaryTab, explainTab, askAiTab, quizTab, flashcardsTab);
        view.setCenter(tabPane);
    }

    /**
     * Tab 1: Displays full extracted text with copy & paragraph selection actions.
     */
    private VBox createTextTab(String paperText) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label infoLabel = new Label("Full text extracted via Apache PDFBox 3. Select any text to explain or copy.");
        infoLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button sendToExplainerBtn = new Button("Explain Selected Text →");
        sendToExplainerBtn.getStyleClass().add("btn-secondary");

        Button copyBtn = new Button("Copy Full Text");
        copyBtn.getStyleClass().add("btn-secondary");

        actionRow.getChildren().addAll(infoLabel, spacer, sendToExplainerBtn, copyBtn);

        TextArea contentArea = new TextArea(paperText.isEmpty() ? "No text available." : paperText);
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(paperText);
            clipboard.setContent(content);
            infoLabel.setText("Full text copied to clipboard!");
            infoLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
        });

        sendToExplainerBtn.setOnAction(e -> {
            String selected = contentArea.getSelectedText();
            if (selected == null || selected.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Selection Required");
                alert.setHeaderText("No text selected");
                alert.setContentText("Please highlight a paragraph with your mouse first, or type directly in Tab 3.");
                alert.showAndWait();
                return;
            }
            paragraphInputArea.setText(selected.trim());
            tabPane.getSelectionModel().select(2); // Switch to Tab 3
        });

        box.getChildren().addAll(actionRow, contentArea);
        return box;
    }

    /**
     * Tab 2: Generates and displays AI Summary of the paper using Google Gemini.
     */
    private VBox createSummaryTab(String paperText) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        generateSummaryBtn = new Button("✨ Generate AI Summary");
        generateSummaryBtn.getStyleClass().add("btn-primary");

        saveSummaryBtn = new Button("💾 Save Summary to Database");
        saveSummaryBtn.getStyleClass().add("btn-success");
        saveSummaryBtn.setDisable(true);

        summarySpinner = new ProgressIndicator();
        summarySpinner.setMaxSize(24, 24);
        summarySpinner.setVisible(false);

        summaryStatusLabel = new Label();
        summaryStatusLabel.setStyle("-fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controls.getChildren().addAll(generateSummaryBtn, saveSummaryBtn, summarySpinner, summaryStatusLabel);

        summaryOutputArea = new TextArea();
        summaryOutputArea.setPromptText("Click 'Generate AI Summary' to analyze this research paper using Google Gemini AI...");
        summaryOutputArea.setWrapText(true);
        summaryOutputArea.setEditable(false);
        summaryOutputArea.setStyle("-fx-font-size: 14px; -fx-line-spacing: 5px;");
        VBox.setVgrow(summaryOutputArea, Priority.ALWAYS);

        generateSummaryBtn.setOnAction(e -> {
            if (paperText == null || paperText.trim().isEmpty()) {
                showError("Cannot Summarize", "Paper text is empty or unreadable.");
                return;
            }

            generateSummaryBtn.setDisable(true);
            saveSummaryBtn.setDisable(true);
            summarySpinner.setVisible(true);
            summaryStatusLabel.setText("Connecting to Google Gemini AI...");
            summaryStatusLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");

            Task<String> summaryTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return geminiService.generateSummary(paperText);
                }
            };

            summaryTask.setOnSucceeded(event -> {
                String summary = summaryTask.getValue();
                summaryOutputArea.setText(summary);
                summarySpinner.setVisible(false);
                generateSummaryBtn.setDisable(false);
                saveSummaryBtn.setDisable(false);
                summaryStatusLabel.setText("Summary generated successfully!");
                summaryStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            });

            summaryTask.setOnFailed(event -> {
                Throwable ex = summaryTask.getException();
                summarySpinner.setVisible(false);
                generateSummaryBtn.setDisable(false);
                summaryStatusLabel.setText("Error generating summary.");
                summaryStatusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                showError("AI Summary Failed", ex.getMessage());
            });

            Thread thread = new Thread(summaryTask);
            thread.setDaemon(true);
            thread.start();
        });

        saveSummaryBtn.setOnAction(e -> {
            User user = SessionManager.getCurrentUser();
            String summaryText = summaryOutputArea.getText().trim();
            if (user == null || paper == null || summaryText.isEmpty()) {
                return;
            }

            Summary summary = new Summary(paper.getPaperId(), summaryText);
            boolean saved = summaryDAO.saveSummary(summary, user.getUserId());
            if (saved) {
                summaryStatusLabel.setText("Summary saved to MySQL database!");
                summaryStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                saveSummaryBtn.setDisable(true);
            } else {
                showError("Database Error", "Could not save summary to MySQL database.");
            }
        });

        box.getChildren().addAll(controls, summaryOutputArea);
        return box;
    }

    /**
     * Tab 3: Allows students to paste and simplify difficult research paper paragraphs.
     */
    private VBox createExplainTab() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label inputHeader = new Label("Paste or edit a difficult paragraph from the paper:");
        inputHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");

        paragraphInputArea = new TextArea();
        paragraphInputArea.setPromptText("e.g. Paste a complex paragraph with advanced equations, acronyms, or methodology descriptions here...");
        paragraphInputArea.setPrefHeight(140);
        paragraphInputArea.setWrapText(true);

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        explainBtn = new Button("🔍 Explain Paragraph in Simple English");
        explainBtn.getStyleClass().add("btn-primary");

        explainSpinner = new ProgressIndicator();
        explainSpinner.setMaxSize(24, 24);
        explainSpinner.setVisible(false);

        explainStatusLabel = new Label();

        actionRow.getChildren().addAll(explainBtn, explainSpinner, explainStatusLabel);

        Label outputHeader = new Label("AI Explanation & Simplified Concept Breakdown:");
        outputHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");

        explanationOutputArea = new TextArea();
        explanationOutputArea.setPromptText("The simplified explanation and technical breakdown will appear here...");
        explanationOutputArea.setWrapText(true);
        explanationOutputArea.setEditable(false);
        explanationOutputArea.setStyle("-fx-font-size: 14px; -fx-line-spacing: 5px;");
        VBox.setVgrow(explanationOutputArea, Priority.ALWAYS);

        explainBtn.setOnAction(e -> {
            String text = paragraphInputArea.getText().trim();
            if (text.isEmpty()) {
                showError("Input Required", "Please paste or type a paragraph to explain.");
                return;
            }

            explainBtn.setDisable(true);
            explainSpinner.setVisible(true);
            explainStatusLabel.setText("Simplifying with Gemini AI...");
            explainStatusLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");

            Task<String> explainTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return geminiService.explainParagraph(text);
                }
            };

            explainTask.setOnSucceeded(event -> {
                explanationOutputArea.setText(explainTask.getValue());
                explainSpinner.setVisible(false);
                explainBtn.setDisable(false);
                explainStatusLabel.setText("Explanation complete!");
                explainStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            });

            explainTask.setOnFailed(event -> {
                Throwable ex = explainTask.getException();
                explainSpinner.setVisible(false);
                explainBtn.setDisable(false);
                explainStatusLabel.setText("Failed to generate explanation.");
                explainStatusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                showError("Explanation Failed", ex.getMessage());
            });

            Thread thread = new Thread(explainTask);
            thread.setDaemon(true);
            thread.start();
        });

        box.getChildren().addAll(inputHeader, paragraphInputArea, actionRow, outputHeader, explanationOutputArea);
        return box;
    }

    /**
     * Tab 4: Ask AI questions about the currently uploaded research paper.
     * Answers strictly from paper context; saves and loads from MySQL chat_history.
     */
    private VBox createAskAiTab(String paperText) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label infoLabel = new Label("Ask questions about this research paper. Answers are strictly grounded in the paper's text.");
        infoLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");

        // Conversation history display
        chatDisplayArea = new TextArea();
        chatDisplayArea.setEditable(false);
        chatDisplayArea.setWrapText(true);
        chatDisplayArea.setPromptText("Your questions and Gemini AI answers will appear here...");
        chatDisplayArea.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 13px; -fx-line-spacing: 4px;");
        VBox.setVgrow(chatDisplayArea, Priority.ALWAYS);

        // Input row
        HBox inputRow = new HBox(12);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        questionInputField = new TextField();
        questionInputField.setPromptText("e.g. What is the main dataset used in this paper?");
        questionInputField.setPrefHeight(40);
        HBox.setHgrow(questionInputField, Priority.ALWAYS);

        askQuestionBtn = new Button("Ask Question");
        askQuestionBtn.getStyleClass().add("btn-primary");
        askQuestionBtn.setPrefHeight(40);

        askSpinner = new ProgressIndicator();
        askSpinner.setMaxSize(24, 24);
        askSpinner.setVisible(false);

        askStatusLabel = new Label();
        askStatusLabel.setStyle("-fx-font-size: 12px;");

        inputRow.getChildren().addAll(questionInputField, askQuestionBtn, askSpinner, askStatusLabel);

        // Action when asking question
        Runnable askAction = () -> {
            String question = questionInputField.getText().trim();
            if (question.isEmpty()) {
                showError("Input Required", "Please type a question to ask.");
                return;
            }

            if (paperText == null || paperText.trim().isEmpty()) {
                showError("No Paper Content", "This paper does not contain readable text.");
                return;
            }

            // Set loading state
            askQuestionBtn.setDisable(true);
            questionInputField.setDisable(true);
            askSpinner.setVisible(true);
            askStatusLabel.setText("Searching paper content with Gemini AI...");
            askStatusLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");

            Task<String> askTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return geminiService.askQuestion(paperText, question);
                }
            };

            askTask.setOnSucceeded(event -> {
                String answer = askTask.getValue();

                // Append to chat area
                appendChatMessage("Student", question);
                appendChatMessage("Gemini AI", answer);

                // Persist to MySQL chat_history table
                User user = SessionManager.getCurrentUser();
                if (user != null && paper != null) {
                    ChatMessage msg = new ChatMessage(paper.getPaperId(), question, answer);
                    chatDAO.saveChatMessage(msg, user.getUserId());
                }

                // Reset UI
                questionInputField.clear();
                questionInputField.setDisable(false);
                askQuestionBtn.setDisable(false);
                askSpinner.setVisible(false);
                askStatusLabel.setText("Answer ready!");
                askStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                questionInputField.requestFocus();
            });

            askTask.setOnFailed(event -> {
                Throwable ex = askTask.getException();
                questionInputField.setDisable(false);
                askQuestionBtn.setDisable(false);
                askSpinner.setVisible(false);
                askStatusLabel.setText("Failed to get answer.");
                askStatusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                showError("Q&A Error", ex.getMessage());
            });

            Thread thread = new Thread(askTask);
            thread.setDaemon(true);
            thread.start();
        };

        askQuestionBtn.setOnAction(e -> askAction.run());
        questionInputField.setOnAction(e -> askAction.run());

        box.getChildren().addAll(infoLabel, chatDisplayArea, inputRow);
        return box;
    }

    private void appendChatMessage(String sender, String message) {
        String existing = chatDisplayArea.getText();
        String entry = "[" + sender + "]\n" + message + "\n\n"
                     + "--------------------------------------------------------------------------------\n\n";
        chatDisplayArea.setText(existing + entry);
        chatDisplayArea.positionCaret(chatDisplayArea.getText().length());
    }

    private void loadExistingSummary() {
        User user = SessionManager.getCurrentUser();
        if (user == null || paper == null) {
            return;
        }

        Summary saved = summaryDAO.getLatestSummary(paper.getPaperId(), user.getUserId());
        if (saved != null) {
            summaryOutputArea.setText(saved.getSummaryText());
            summaryStatusLabel.setText("Loaded saved summary from database (" + saved.getFormattedDate() + ")");
            summaryStatusLabel.setStyle("-fx-text-fill: #059669;");
        }
    }

    private void loadExistingChatHistory() {
        User user = SessionManager.getCurrentUser();
        if (user == null || paper == null) {
            return;
        }

        List<ChatMessage> history = chatDAO.getChatHistoryByPaperId(paper.getPaperId(), user.getUserId());
        if (!history.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ChatMessage msg : history) {
                sb.append("[Student - ").append(msg.getFormattedTime()).append("]\n")
                  .append(msg.getUserQuestion()).append("\n\n")
                  .append("[Gemini AI]\n")
                  .append(msg.getAiAnswer()).append("\n\n")
                  .append("--------------------------------------------------------------------------------\n\n");
            }
            chatDisplayArea.setText(sb.toString());
            chatDisplayArea.positionCaret(chatDisplayArea.getText().length());
            askStatusLabel.setText("Restored " + history.size() + " previous Q&A from database.");
            askStatusLabel.setStyle("-fx-text-fill: #059669;");
        }
    }

    /**
     * Tab 5: Generates and administers multiple-choice quiz questions from the paper.
     */
    private VBox createQuizTab(String paperText) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));

        // Top controls row
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label countLabel = new Label("Questions:");
        countLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");

        quizCountCombo = new ComboBox<>();
        quizCountCombo.getItems().addAll("5 Questions", "10 Questions");
        quizCountCombo.setValue("5 Questions");
        quizCountCombo.setPrefHeight(36);

        generateQuizBtn = new Button("📝 Generate Quiz");
        generateQuizBtn.getStyleClass().add("btn-primary");
        generateQuizBtn.setPrefHeight(36);

        saveQuizBtn = new Button("💾 Save Quiz to DB");
        saveQuizBtn.getStyleClass().add("btn-success");
        saveQuizBtn.setPrefHeight(36);
        saveQuizBtn.setDisable(true);

        quizSpinner = new ProgressIndicator();
        quizSpinner.setMaxSize(24, 24);
        quizSpinner.setVisible(false);

        quizStatusLabel = new Label("Choose 5 or 10 questions and click 'Generate Quiz'.");
        quizStatusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");

        controls.getChildren().addAll(countLabel, quizCountCombo, generateQuizBtn, saveQuizBtn, quizSpinner, quizStatusLabel);

        // Main StackPane for Quiz: switches between Empty, Active, and Results
        quizViewStack = new StackPane();
        VBox.setVgrow(quizViewStack, Priority.ALWAYS);

        // 1. Placeholder when no quiz is loaded
        quizEmptyPlaceholder = new VBox(14);
        quizEmptyPlaceholder.setAlignment(Pos.CENTER);
        quizEmptyPlaceholder.setPadding(new Insets(40));
        quizEmptyPlaceholder.getStyleClass().add("card");
        Label placeholderTitle = new Label("No Quiz Generated Yet");
        placeholderTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #1E3A8A; -fx-text-fill: #1E3A8A;");
        Label placeholderSub = new Label("Select 5 or 10 multiple-choice questions above to test your understanding of this research paper.\nQuestions and answer explanations are grounded strictly in the paper's text.");
        placeholderSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-text-alignment: center;");
        placeholderSub.setWrapText(true);
        quizEmptyPlaceholder.getChildren().addAll(placeholderTitle, placeholderSub);

        // 2. Active Quiz Pane
        quizActiveBox = new VBox(16);
        quizActiveBox.setVisible(false);

        HBox progressRow = new HBox(12);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        quizProgressLabel = new Label("Question 1 of 5");
        quizProgressLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E3A8A;");
        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        quizProgressBar = new ProgressBar(0.2);
        quizProgressBar.setPrefWidth(240);
        progressRow.getChildren().addAll(quizProgressLabel, pSpacer, quizProgressBar);

        // Question card
        VBox questionCard = new VBox(16);
        questionCard.getStyleClass().add("card");
        VBox.setVgrow(questionCard, Priority.ALWAYS);

        quizQuestionLabel = new Label("Question text will appear here");
        quizQuestionLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-line-spacing: 4px;");
        quizQuestionLabel.setWrapText(true);

        quizToggleGroup = new ToggleGroup();

        optARadio = new RadioButton();
        optARadio.setToggleGroup(quizToggleGroup);
        optARadio.setWrapText(true);

        optBRadio = new RadioButton();
        optBRadio.setToggleGroup(quizToggleGroup);
        optBRadio.setWrapText(true);

        optCRadio = new RadioButton();
        optCRadio.setToggleGroup(quizToggleGroup);
        optCRadio.setWrapText(true);

        optDRadio = new RadioButton();
        optDRadio.setToggleGroup(quizToggleGroup);
        optDRadio.setWrapText(true);

        optABox = createOptionContainer(optARadio, "A");
        optBBox = createOptionContainer(optBRadio, "B");
        optCBox = createOptionContainer(optCRadio, "C");
        optDBox = createOptionContainer(optDRadio, "D");

        VBox optionsBox = new VBox(10);
        optionsBox.getChildren().addAll(optABox, optBBox, optCBox, optDBox);

        questionCard.getChildren().addAll(quizQuestionLabel, optionsBox);

        // Navigation row
        HBox navRow = new HBox(12);
        navRow.setAlignment(Pos.CENTER_LEFT);

        prevQuizBtn = new Button("← Previous");
        prevQuizBtn.getStyleClass().add("btn-secondary");

        nextQuizBtn = new Button("Next →");
        nextQuizBtn.getStyleClass().add("btn-secondary");

        Region nSpacer = new Region();
        HBox.setHgrow(nSpacer, Priority.ALWAYS);

        submitQuizBtn = new Button("Submit Quiz ✓");
        submitQuizBtn.getStyleClass().add("btn-primary");

        navRow.getChildren().addAll(prevQuizBtn, nextQuizBtn, nSpacer, submitQuizBtn);

        quizActiveBox.getChildren().addAll(progressRow, questionCard, navRow);

        // 3. Quiz Results Pane
        quizResultBox = new VBox(16);
        quizResultBox.setVisible(false);

        quizViewStack.getChildren().addAll(quizEmptyPlaceholder, quizActiveBox, quizResultBox);

        // Events
        generateQuizBtn.setOnAction(e -> {
            if (paperText == null || paperText.trim().isEmpty()) {
                showError("Cannot Generate Quiz", "Paper text is empty or unreadable.");
                return;
            }

            int numQuestions = quizCountCombo.getValue().contains("10") ? 10 : 5;

            generateQuizBtn.setDisable(true);
            saveQuizBtn.setDisable(true);
            quizSpinner.setVisible(true);
            quizStatusLabel.setText("Generating " + numQuestions + " questions with Gemini AI...");
            quizStatusLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");

            Task<List<QuizQuestion>> quizTask = new Task<>() {
                @Override
                protected List<QuizQuestion> call() throws Exception {
                    return geminiService.generateQuiz(paperText, numQuestions);
                }
            };

            quizTask.setOnSucceeded(event -> {
                currentQuizQuestions = quizTask.getValue();
                currentQuizIndex = 0;
                quizSpinner.setVisible(false);
                generateQuizBtn.setDisable(false);
                saveQuizBtn.setDisable(false);
                quizStatusLabel.setText("Quiz ready! " + currentQuizQuestions.size() + " questions generated.");
                quizStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                showActiveQuiz();
                renderCurrentQuizQuestion();
            });

            quizTask.setOnFailed(event -> {
                Throwable ex = quizTask.getException();
                quizSpinner.setVisible(false);
                generateQuizBtn.setDisable(false);
                quizStatusLabel.setText("Quiz generation failed.");
                quizStatusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                showError("Quiz Generation Error", ex.getMessage());
            });

            Thread thread = new Thread(quizTask);
            thread.setDaemon(true);
            thread.start();
        });

        prevQuizBtn.setOnAction(e -> {
            saveCurrentQuizAnswer();
            if (currentQuizIndex > 0) {
                currentQuizIndex--;
                renderCurrentQuizQuestion();
            }
        });

        nextQuizBtn.setOnAction(e -> {
            saveCurrentQuizAnswer();
            if (currentQuizIndex < currentQuizQuestions.size() - 1) {
                currentQuizIndex++;
                renderCurrentQuizQuestion();
            }
        });

        submitQuizBtn.setOnAction(e -> submitQuiz());

        saveQuizBtn.setOnAction(e -> {
            User user = SessionManager.getCurrentUser();
            if (user == null || paper == null || currentQuizQuestions.isEmpty()) {
                return;
            }
            boolean saved = quizDAO.saveQuizQuestions(paper.getPaperId(), currentQuizQuestions, user.getUserId());
            if (saved) {
                quizStatusLabel.setText("Saved " + currentQuizQuestions.size() + " quiz questions to MySQL!");
                quizStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                saveQuizBtn.setDisable(true);
            } else {
                showError("Database Error", "Failed to save quiz questions to database.");
            }
        });

        box.getChildren().addAll(controls, quizViewStack);
        return box;
    }

    private VBox createOptionContainer(RadioButton radio, String key) {
        VBox container = new VBox(radio);
        container.getStyleClass().add("quiz-option-box");
        container.setOnMouseClicked(e -> {
            radio.setSelected(true);
            highlightSelectedOption(key);
        });
        radio.setOnAction(e -> highlightSelectedOption(key));
        return container;
    }

    private void highlightSelectedOption(String key) {
        optABox.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), "A".equals(key));
        optBBox.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), "B".equals(key));
        optCBox.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), "C".equals(key));
        optDBox.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), "D".equals(key));
    }

    private void saveCurrentQuizAnswer() {
        if (currentQuizQuestions.isEmpty() || currentQuizIndex >= currentQuizQuestions.size()) {
            return;
        }
        QuizQuestion q = currentQuizQuestions.get(currentQuizIndex);
        if (optARadio.isSelected()) q.setUserSelectedAnswer("A");
        else if (optBRadio.isSelected()) q.setUserSelectedAnswer("B");
        else if (optCRadio.isSelected()) q.setUserSelectedAnswer("C");
        else if (optDRadio.isSelected()) q.setUserSelectedAnswer("D");
    }

    private void renderCurrentQuizQuestion() {
        if (currentQuizQuestions.isEmpty() || currentQuizIndex >= currentQuizQuestions.size()) {
            return;
        }

        QuizQuestion q = currentQuizQuestions.get(currentQuizIndex);
        int total = currentQuizQuestions.size();

        quizProgressLabel.setText("Question " + (currentQuizIndex + 1) + " of " + total);
        quizProgressBar.setProgress((double) (currentQuizIndex + 1) / total);
        quizQuestionLabel.setText("Q" + (currentQuizIndex + 1) + ". " + q.getQuestion());

        optARadio.setText("A) " + q.getOptionA());
        optBRadio.setText("B) " + q.getOptionB());
        optCRadio.setText("C) " + q.getOptionC());
        optDRadio.setText("D) " + q.getOptionD());

        quizToggleGroup.selectToggle(null);
        String selected = q.getUserSelectedAnswer();
        if ("A".equalsIgnoreCase(selected)) optARadio.setSelected(true);
        else if ("B".equalsIgnoreCase(selected)) optBRadio.setSelected(true);
        else if ("C".equalsIgnoreCase(selected)) optCRadio.setSelected(true);
        else if ("D".equalsIgnoreCase(selected)) optDRadio.setSelected(true);

        highlightSelectedOption(selected != null ? selected.toUpperCase() : "");

        prevQuizBtn.setDisable(currentQuizIndex == 0);
        nextQuizBtn.setDisable(currentQuizIndex == total - 1);
    }

    private void submitQuiz() {
        if (currentQuizQuestions.isEmpty()) return;
        saveCurrentQuizAnswer();

        long answered = currentQuizQuestions.stream().filter(QuizQuestion::isAnswered).count();
        int total = currentQuizQuestions.size();

        if (answered < total) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Incomplete Quiz");
            confirm.setHeaderText("You have answered " + answered + " of " + total + " questions.");
            confirm.setContentText("Do you want to submit anyway and view your score?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        int score = 0;
        for (QuizQuestion q : currentQuizQuestions) {
            if (q.isCorrect()) {
                score++;
            }
        }

        showQuizResults(score, total);
    }

    private void showQuizResults(int score, int total) {
        quizActiveBox.setVisible(false);
        quizEmptyPlaceholder.setVisible(false);
        quizResultBox.getChildren().clear();
        quizResultBox.setVisible(true);

        int percentage = (int) Math.round(((double) score / total) * 100);

        // Score summary card
        VBox scoreCard = new VBox(10);
        scoreCard.setAlignment(Pos.CENTER);
        scoreCard.getStyleClass().add("card");
        scoreCard.setPadding(new Insets(20));

        Label title = new Label("🎉 Quiz Completed!");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E3A8A;");

        Label scoreText = new Label("Final Score: " + score + " / " + total + "  (" + percentage + "%)");
        String color = percentage >= 70 ? "#059669" : (percentage >= 50 ? "#D97706" : "#DC2626");
        scoreText.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        String feedback = percentage >= 80 ? "Outstanding! You demonstrated an excellent understanding of this research paper."
                        : (percentage >= 50 ? "Good effort! Review the explanations below to solidify key concepts."
                        : "Keep studying! Review the correct answers and paper explanations below.");
        Label feedbackLabel = new Label(feedback);
        feedbackLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER);

        Button retakeBtn = new Button("🔄 Retake Quiz");
        retakeBtn.getStyleClass().add("btn-secondary");
        retakeBtn.setOnAction(e -> retakeQuiz());

        Button newQuizBtn = new Button("✨ Generate New Quiz");
        newQuizBtn.getStyleClass().add("btn-primary");
        newQuizBtn.setOnAction(e -> {
            showEmptyQuiz();
            generateQuizBtn.fire();
        });

        actionButtons.getChildren().addAll(retakeBtn, newQuizBtn);
        scoreCard.getChildren().addAll(title, scoreText, feedbackLabel, actionButtons);

        // Review list of all questions
        VBox reviewList = new VBox(14);
        reviewList.setPadding(new Insets(10, 0, 10, 0));

        for (int i = 0; i < currentQuizQuestions.size(); i++) {
            QuizQuestion q = currentQuizQuestions.get(i);
            VBox item = new VBox(10);
            item.getStyleClass().add("card");
            item.setPadding(new Insets(14, 18, 14, 18));

            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label qNum = new Label("Question " + (i + 1) + ":");
            qNum.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E293B;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label badge = new Label(q.isCorrect() ? "✓ Correct" : "✗ Incorrect");
            badge.getStyleClass().add(q.isCorrect() ? "badge-pill-green" : "badge-pill-red");

            topRow.getChildren().addAll(qNum, sp, badge);

            Label questionContent = new Label(q.getQuestion());
            questionContent.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
            questionContent.setWrapText(true);

            String userChoice = q.getUserSelectedAnswer();
            String userChoiceText = (userChoice != null && !userChoice.isEmpty())
                    ? userChoice + ") " + q.getOptionText(userChoice)
                    : "No answer selected";
            Label yourAns = new Label("Your Answer: " + userChoiceText);
            yourAns.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (q.isCorrect() ? "#059669" : "#DC2626") + "; -fx-font-weight: bold;");

            Label correctAns = new Label("Correct Answer: " + q.getCorrectAnswer() + ") " + q.getOptionText(q.getCorrectAnswer()));
            correctAns.setStyle("-fx-font-size: 13px; -fx-text-fill: #059669; -fx-font-weight: bold;");

            VBox explanationBox = new VBox(4);
            explanationBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 12px;");
            Label expTitle = new Label("Paper Explanation:");
            expTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #475569;");
            Label expText = new Label(q.getExplanation() != null ? q.getExplanation() : "Grounded strictly in research paper content.");
            expText.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
            expText.setWrapText(true);
            explanationBox.getChildren().addAll(expTitle, expText);

            item.getChildren().addAll(topRow, questionContent, yourAns, correctAns, explanationBox);
            reviewList.getChildren().add(item);
        }

        ScrollPane scroll = new ScrollPane(reviewList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        quizResultBox.getChildren().addAll(scoreCard, scroll);
    }

    private void retakeQuiz() {
        for (QuizQuestion q : currentQuizQuestions) {
            q.setUserSelectedAnswer(null);
        }
        currentQuizIndex = 0;
        showActiveQuiz();
        renderCurrentQuizQuestion();
        quizStatusLabel.setText("Retaking quiz. Select your answers.");
        quizStatusLabel.setStyle("-fx-text-fill: #2563EB;");
    }

    private void showActiveQuiz() {
        quizActiveBox.setVisible(true);
        quizResultBox.setVisible(false);
        quizEmptyPlaceholder.setVisible(false);
    }

    private void showEmptyQuiz() {
        quizActiveBox.setVisible(false);
        quizResultBox.setVisible(false);
        quizEmptyPlaceholder.setVisible(true);
    }

    private void loadExistingQuiz() {
        User user = SessionManager.getCurrentUser();
        if (user == null || paper == null) {
            return;
        }

        List<QuizQuestion> saved = quizDAO.getQuizByPaperId(paper.getPaperId(), user.getUserId());
        if (!saved.isEmpty()) {
            currentQuizQuestions = saved;
            currentQuizIndex = 0;
            saveQuizBtn.setDisable(true);
            quizStatusLabel.setText("Loaded saved quiz (" + saved.size() + " questions) from database.");
            quizStatusLabel.setStyle("-fx-text-fill: #059669;");
            showActiveQuiz();
            renderCurrentQuizQuestion();
        }
    }

    /**
     * Tab 6: Generates and provides an interactive study deck of 10 research paper flashcards.
     */
    private VBox createFlashcardsTab(String paperText) {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));

        // Top controls row
        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);

        generateFlashcardsBtn = new Button("🃏 Generate Flashcards (10 Cards)");
        generateFlashcardsBtn.getStyleClass().add("btn-primary");
        generateFlashcardsBtn.setPrefHeight(36);

        shuffleFlashcardsBtn = new Button("🔀 Shuffle Cards");
        shuffleFlashcardsBtn.getStyleClass().add("btn-secondary");
        shuffleFlashcardsBtn.setPrefHeight(36);
        shuffleFlashcardsBtn.setDisable(true);

        flashcardSpinner = new ProgressIndicator();
        flashcardSpinner.setMaxSize(24, 24);
        flashcardSpinner.setVisible(false);

        flashcardStatusLabel = new Label("Click 'Generate Flashcards' to create 10 study cards.");
        flashcardStatusLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");

        controls.getChildren().addAll(generateFlashcardsBtn, shuffleFlashcardsBtn, flashcardSpinner, flashcardStatusLabel);

        // Flashcards Stack
        flashcardViewStack = new StackPane();
        VBox.setVgrow(flashcardViewStack, Priority.ALWAYS);

        // Empty placeholder
        flashcardEmptyPlaceholder = new VBox(14);
        flashcardEmptyPlaceholder.setAlignment(Pos.CENTER);
        flashcardEmptyPlaceholder.setPadding(new Insets(40));
        flashcardEmptyPlaceholder.getStyleClass().add("card");
        Label placeholderTitle = new Label("No Flashcards Generated Yet");
        placeholderTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #1E3A8A; -fx-text-fill: #1E3A8A;");
        Label placeholderSub = new Label("Click 'Generate Flashcards' to automatically create 10 question-and-answer revision cards from this paper.\nFlip cards to test your knowledge of key terminology, methods, and results.");
        placeholderSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-text-alignment: center;");
        placeholderSub.setWrapText(true);
        flashcardEmptyPlaceholder.getChildren().addAll(placeholderTitle, placeholderSub);

        // Active flashcard container
        flashcardActiveBox = new VBox(20);
        flashcardActiveBox.setAlignment(Pos.CENTER);
        flashcardActiveBox.setVisible(false);

        // Counter & Progress row
        HBox counterRow = new HBox(12);
        counterRow.setAlignment(Pos.CENTER);
        counterRow.setMaxWidth(680);

        flashcardCounterLabel = new Label("Card 1 of 10");
        flashcardCounterLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E3A8A;");

        Region cSpacer = new Region();
        HBox.setHgrow(cSpacer, Priority.ALWAYS);

        flashcardProgressBar = new ProgressBar(0.1);
        flashcardProgressBar.setPrefWidth(260);

        counterRow.getChildren().addAll(flashcardCounterLabel, cSpacer, flashcardProgressBar);

        // Flashcard physical view
        flashcardCardView = new VBox(16);
        flashcardCardView.setAlignment(Pos.CENTER);
        flashcardCardView.getStyleClass().add("flashcard-surface");
        flashcardCardView.setMaxWidth(680);
        flashcardCardView.setMinHeight(260);

        HBox cardTopRow = new HBox();
        cardTopRow.setAlignment(Pos.CENTER_LEFT);
        flashcardSideBadge = new Label("FRONT: QUESTION");
        flashcardSideBadge.getStyleClass().add("badge-pill-blue");
        Region badgeSpacer = new Region();
        HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
        flashcardIndexPill = new Label("Card 1 of 10");
        flashcardIndexPill.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        cardTopRow.getChildren().addAll(flashcardSideBadge, badgeSpacer, flashcardIndexPill);

        flashcardTextLabel = new Label("Question text here");
        flashcardTextLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-text-alignment: center; -fx-line-spacing: 5px;");
        flashcardTextLabel.setWrapText(true);
        flashcardTextLabel.setAlignment(Pos.CENTER);
        VBox.setVgrow(flashcardTextLabel, Priority.ALWAYS);

        flashcardHintLabel = new Label("Click 'Flip Card' below to reveal the answer");
        flashcardHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");

        flashcardCardView.getChildren().addAll(cardTopRow, flashcardTextLabel, flashcardHintLabel);

        // Card navigation controls
        HBox cardNavRow = new HBox(16);
        cardNavRow.setAlignment(Pos.CENTER);
        cardNavRow.setMaxWidth(680);

        prevFlashcardBtn = new Button("← Previous Card");
        prevFlashcardBtn.getStyleClass().add("btn-secondary");
        prevFlashcardBtn.setPrefHeight(40);

        flipFlashcardBtn = new Button("🔄 Flip Card (Show Answer)");
        flipFlashcardBtn.getStyleClass().add("btn-primary");
        flipFlashcardBtn.setPrefHeight(40);
        flipFlashcardBtn.setPrefWidth(220);

        nextFlashcardBtn = new Button("Next Card →");
        nextFlashcardBtn.getStyleClass().add("btn-secondary");
        nextFlashcardBtn.setPrefHeight(40);

        cardNavRow.getChildren().addAll(prevFlashcardBtn, flipFlashcardBtn, nextFlashcardBtn);

        flashcardActiveBox.getChildren().addAll(counterRow, flashcardCardView, cardNavRow);

        flashcardViewStack.getChildren().addAll(flashcardEmptyPlaceholder, flashcardActiveBox);

        // Events
        generateFlashcardsBtn.setOnAction(e -> {
            if (paperText == null || paperText.trim().isEmpty()) {
                showError("Cannot Generate Flashcards", "Paper text is empty or unreadable.");
                return;
            }

            generateFlashcardsBtn.setDisable(true);
            shuffleFlashcardsBtn.setDisable(true);
            flashcardSpinner.setVisible(true);
            flashcardStatusLabel.setText("Generating 10 study flashcards with Gemini AI...");
            flashcardStatusLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");

            Task<List<Flashcard>> task = new Task<>() {
                @Override
                protected List<Flashcard> call() throws Exception {
                    return geminiService.generateFlashcards(paperText);
                }
            };

            task.setOnSucceeded(event -> {
                currentFlashcards = task.getValue();
                currentFlashcardIndex = 0;
                isFlashcardFlipped = false;
                flashcardSpinner.setVisible(false);
                generateFlashcardsBtn.setDisable(false);
                shuffleFlashcardsBtn.setDisable(false);
                flashcardStatusLabel.setText("10 Flashcards ready! Click Flip Card to study.");
                flashcardStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");

                flashcardEmptyPlaceholder.setVisible(false);
                flashcardActiveBox.setVisible(true);
                renderCurrentFlashcard();
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();
                flashcardSpinner.setVisible(false);
                generateFlashcardsBtn.setDisable(false);
                flashcardStatusLabel.setText("Flashcard generation failed.");
                flashcardStatusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                showError("Flashcards Error", ex.getMessage());
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        flipFlashcardBtn.setOnAction(e -> flipFlashcard());

        flashcardCardView.setOnMouseClicked(e -> flipFlashcard());

        prevFlashcardBtn.setOnAction(e -> {
            if (currentFlashcardIndex > 0) {
                currentFlashcardIndex--;
                isFlashcardFlipped = false;
                renderCurrentFlashcard();
            }
        });

        nextFlashcardBtn.setOnAction(e -> {
            if (currentFlashcardIndex < currentFlashcards.size() - 1) {
                currentFlashcardIndex++;
                isFlashcardFlipped = false;
                renderCurrentFlashcard();
            }
        });

        shuffleFlashcardsBtn.setOnAction(e -> {
            if (!currentFlashcards.isEmpty()) {
                Collections.shuffle(currentFlashcards);
                currentFlashcardIndex = 0;
                isFlashcardFlipped = false;
                renderCurrentFlashcard();
                flashcardStatusLabel.setText("Shuffled " + currentFlashcards.size() + " flashcards! Showing Card 1.");
                flashcardStatusLabel.setStyle("-fx-text-fill: #2563EB;");
            }
        });

        box.getChildren().addAll(controls, flashcardViewStack);
        return box;
    }

    private void renderCurrentFlashcard() {
        if (currentFlashcards.isEmpty() || currentFlashcardIndex >= currentFlashcards.size()) {
            return;
        }

        Flashcard card = currentFlashcards.get(currentFlashcardIndex);
        int total = currentFlashcards.size();

        flashcardCounterLabel.setText("Card " + (currentFlashcardIndex + 1) + " of " + total);
        flashcardIndexPill.setText("Card " + (currentFlashcardIndex + 1) + " of " + total);
        flashcardProgressBar.setProgress((double) (currentFlashcardIndex + 1) / total);

        if (!isFlashcardFlipped) {
            flashcardSideBadge.setText("FRONT: QUESTION");
            flashcardSideBadge.getStyleClass().setAll("badge-pill-blue");
            flashcardTextLabel.setText(card.getQuestion());
            flashcardHintLabel.setText("Click 'Flip Card' below to reveal the answer");
            flipFlashcardBtn.setText("🔄 Flip Card (Show Answer)");
        } else {
            flashcardSideBadge.setText("BACK: ANSWER");
            flashcardSideBadge.getStyleClass().setAll("badge-pill-green");
            flashcardTextLabel.setText(card.getAnswer());
            flashcardHintLabel.setText("Click 'Flip Card' below to return to question");
            flipFlashcardBtn.setText("🔄 Flip Back (Show Question)");
        }

        prevFlashcardBtn.setDisable(currentFlashcardIndex == 0);
        nextFlashcardBtn.setDisable(currentFlashcardIndex == total - 1);
    }

    private void flipFlashcard() {
        if (currentFlashcards.isEmpty()) return;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), flashcardCardView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.1);
        fadeOut.setOnFinished(e -> {
            isFlashcardFlipped = !isFlashcardFlipped;
            renderCurrentFlashcard();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(120), flashcardCardView);
            fadeIn.setFromValue(0.1);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
