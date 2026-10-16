package com.assistant.controller;

import com.assistant.dao.ChatDAO;
import com.assistant.dao.PaperDAO;
import com.assistant.model.Paper;
import com.assistant.model.User;
import com.assistant.service.PdfService;
import com.assistant.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller handling the primary Dashboard interface.
 * Implements sidebar navigation, summary metric cards, search filtering,
 * TableView with View/Delete actions, and Apache PDFBox 3 text extraction.
 */
public class DashboardController {

    private final PaperDAO paperDAO = new PaperDAO();
    private final ChatDAO chatDAO = new ChatDAO();
    private final PdfService pdfService = new PdfService();
    private final ObservableList<Paper> paperList = FXCollections.observableArrayList();

    private BorderPane mainLayout;
    private TableView<Paper> paperTableView;
    private TextField searchField;

    // Stat metric text labels
    private Text totalPapersCountText;
    private Text totalSummariesCountText;
    private Text totalQuestionsCountText;

    // Sidebar navigation buttons for active state tracking
    private Button navDashboardBtn;
    private Button navMyPapersBtn;

    public DashboardController() {
        createView();
        loadDashboardData();
    }

    public Parent getView() {
        return mainLayout;
    }

    private void createView() {
        mainLayout = new BorderPane();

        // 1. Left Sidebar
        mainLayout.setLeft(createSidebar());

        // 2. Center Content Area (inside a scrollable pane)
        VBox centerContent = new VBox(24);
        centerContent.setPadding(new Insets(28));
        centerContent.setStyle("-fx-background-color: #F8FAFC;");

        // Top Bar: Welcome + Upload Button
        HBox topBar = createTopBar();

        // Metric Statistics Cards
        HBox statsSection = createStatsSection();

        // Search Bar and Table Section
        VBox tableSection = createTableSection();

        centerContent.getChildren().addAll(topBar, statsSection, tableSection);

        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        mainLayout.setCenter(scrollPane);
    }

    /**
     * Creates the left navigation sidebar.
     */
    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);

        // Branding
        Text brand = new Text("AI Paper Assistant");
        brand.getStyleClass().add("sidebar-brand");

        Label subtitle = new Label("Student Research Portal");
        subtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        VBox brandBox = new VBox(4, brand, subtitle);
        brandBox.setPadding(new Insets(0, 0, 15, 0));

        // Navigation Buttons
        navDashboardBtn = new Button("Dashboard");
        navDashboardBtn.getStyleClass().add("nav-button-active");
        navDashboardBtn.setOnAction(e -> {
            setActiveNav(navDashboardBtn);
            searchField.clear();
            loadDashboardData();
        });

        navMyPapersBtn = new Button("My Papers");
        navMyPapersBtn.getStyleClass().add("nav-button");
        navMyPapersBtn.setOnAction(e -> {
            setActiveNav(navMyPapersBtn);
            paperTableView.requestFocus();
            loadDashboardData();
        });

        Button navUploadBtn = new Button("Upload Paper");
        navUploadBtn.getStyleClass().add("nav-button");
        navUploadBtn.setOnAction(e -> handleUploadPaper());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> handleLogout());

        sidebar.getChildren().addAll(
                brandBox,
                navDashboardBtn,
                navMyPapersBtn,
                navUploadBtn,
                spacer,
                logoutBtn
        );

        return sidebar;
    }

    private void setActiveNav(Button activeButton) {
        navDashboardBtn.getStyleClass().removeAll("nav-button-active", "nav-button");
        navMyPapersBtn.getStyleClass().removeAll("nav-button-active", "nav-button");

        navDashboardBtn.getStyleClass().add("nav-button");
        navMyPapersBtn.getStyleClass().add("nav-button");

        activeButton.getStyleClass().removeAll("nav-button");
        activeButton.getStyleClass().add("nav-button-active");
    }

    /**
     * Creates the top greeting bar with the student's name and quick upload button.
     */
    private HBox createTopBar() {
        User user = SessionManager.getCurrentUser();
        String studentName = (user != null) ? user.getName() : "Student";

        VBox greetingBox = new VBox(4);
        Text welcomeText = new Text("Welcome back, " + studentName + "!");
        welcomeText.getStyleClass().add("title-text");

        Label subText = new Label("Manage, analyze, and ask AI questions about your research papers");
        subText.getStyleClass().add("subtitle-text");
        greetingBox.getChildren().addAll(welcomeText, subText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button quickUploadBtn = new Button("+ Upload Research Paper");
        quickUploadBtn.getStyleClass().add("btn-primary");
        quickUploadBtn.setOnAction(e -> handleUploadPaper());

        HBox topBar = new HBox(15, greetingBox, spacer, quickUploadBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        return topBar;
    }

    /**
     * Creates the 3 metric overview cards: Total Papers, AI Summaries, and Questions Asked.
     */
    private HBox createStatsSection() {
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);

        // Card 1: Total Papers
        totalPapersCountText = new Text("0");
        totalPapersCountText.getStyleClass().add("stat-number");
        VBox card1 = createStatCard("TOTAL PAPERS", totalPapersCountText, "#2563EB");

        // Card 2: AI Summaries
        totalSummariesCountText = new Text("0");
        totalSummariesCountText.getStyleClass().add("stat-number");
        VBox card2 = createStatCard("AI SUMMARIES", totalSummariesCountText, "#10B981");

        // Card 3: Questions Asked
        totalQuestionsCountText = new Text("0");
        totalQuestionsCountText.getStyleClass().add("stat-number");
        VBox card3 = createStatCard("QUESTIONS ASKED", totalQuestionsCountText, "#8B5CF6");

        statsBox.getChildren().addAll(card1, card2, card3);
        return statsBox;
    }

    private VBox createStatCard(String title, Text numberText, String accentColor) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Text titleText = new Text(title);
        titleText.getStyleClass().add("stat-title");

        numberText.setStyle("-fx-fill: " + accentColor + ";");

        card.getChildren().addAll(titleText, numberText);
        return card;
    }

    /**
     * Creates the search bar and research papers TableView section.
     */
    private VBox createTableSection() {
        VBox section = new VBox(15);
        section.getStyleClass().add("card");

        // Header and Search Controls
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Text sectionTitle = new Text("Uploaded Research Papers");
        sectionTitle.getStyleClass().add("section-header");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Search by paper title or author...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> performSearch(newVal));

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("btn-secondary");
        searchBtn.setOnAction(e -> performSearch(searchField.getText()));

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            loadDashboardData();
        });

        headerBox.getChildren().addAll(sectionTitle, spacer, searchField, searchBtn, clearBtn);

        // TableView Configuration
        paperTableView = new TableView<>();
        paperTableView.setItems(paperList);
        paperTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        paperTableView.setPrefHeight(380);

        // Double-click row to open viewer
        paperTableView.setRowFactory(tv -> {
            TableRow<Paper> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Paper rowData = row.getItem();
                    ScreenManager.showPaperViewer(rowData);
                }
            });
            return row;
        });

        // Empty state placeholder
        Label placeholderLabel = new Label("No research papers uploaded yet.\nClick '+ Upload Research Paper' above to get started!");
        placeholderLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-text-alignment: center;");
        paperTableView.setPlaceholder(placeholderLabel);

        // Columns
        TableColumn<Paper, String> titleCol = new TableColumn<>("Paper Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(280);

        TableColumn<Paper, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(150);

        TableColumn<Paper, String> dateCol = new TableColumn<>("Upload Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedDate()));
        dateCol.setPrefWidth(140);

        // Actions Column (View + Delete buttons)
        TableColumn<Paper, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(170);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button deleteBtn = new Button("Delete");
            private final HBox actionBox = new HBox(8, viewBtn, deleteBtn);

            {
                viewBtn.getStyleClass().add("btn-secondary");
                deleteBtn.getStyleClass().add("btn-danger");
                actionBox.setAlignment(Pos.CENTER);

                viewBtn.setOnAction(event -> {
                    Paper paper = getTableView().getItems().get(getIndex());
                    ScreenManager.showPaperViewer(paper);
                });

                deleteBtn.setOnAction(event -> {
                    Paper paper = getTableView().getItems().get(getIndex());
                    handleDeletePaper(paper);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBox);
                }
            }
        });

        paperTableView.getColumns().addAll(titleCol, authorCol, dateCol, actionCol);

        section.getChildren().addAll(headerBox, paperTableView);
        return section;
    }

    /**
     * Loads papers and updates metric summary counts for the logged-in user.
     */
    public void loadDashboardData() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        int userId = user.getUserId();

        List<Paper> papers = paperDAO.getPapersByUserId(userId);
        paperList.setAll(papers);

        int totalPapers = paperDAO.getPaperCount(userId);
        int totalSummaries = paperDAO.getSummaryCount(userId);
        int totalQuestions = chatDAO.getChatCount(userId);

        totalPapersCountText.setText(String.valueOf(totalPapers));
        totalSummariesCountText.setText(String.valueOf(totalSummaries));
        totalQuestionsCountText.setText(String.valueOf(totalQuestions));
    }

    /**
     * Searches papers by title or author for the logged-in user.
     */
    private void performSearch(String keyword) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            loadDashboardData();
            return;
        }

        List<Paper> results = paperDAO.searchPapers(user.getUserId(), keyword.trim());
        paperList.setAll(results);
    }

    /**
     * Handles file chooser and Apache PDFBox 3 text extraction and persistence.
     */
    private void handleUploadPaper() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Research Paper PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf")
        );

        File selectedFile = fileChooser.showOpenDialog(ScreenManager.getPrimaryStage());
        if (selectedFile == null) {
            return;
        }

        // 1. Extract text using PdfService (Apache PDFBox 3)
        String extractedText;
        try {
            extractedText = pdfService.extractText(selectedFile);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("PDF Extraction Error");
            alert.setHeaderText("Failed to Read PDF Document");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return;
        }

        // 2. Allow user to confirm Title and specify Author
        String defaultTitle = selectedFile.getName().replaceFirst("(?i)\\.pdf$", "");

        Dialog<Paper> dialog = new Dialog<>();
        dialog.setTitle("Research Paper Details");
        dialog.setHeaderText("Confirm paper details before saving to MySQL:");

        ButtonType saveButtonType = new ButtonType("Save Paper", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField titleInput = new TextField(defaultTitle);
        titleInput.setPrefWidth(300);

        TextField authorInput = new TextField();
        authorInput.setPromptText("e.g. Vaswani et al. (or leave blank)");
        authorInput.setPrefWidth(300);

        Label wordCountInfo = new Label("Extracted Content: " + pdfService.calculateWordCount(extractedText) + " words ready.");
        wordCountInfo.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");

        grid.add(new Label("Paper Title:"), 0, 0);
        grid.add(titleInput, 1, 0);
        grid.add(new Label("Author(s):"), 0, 1);
        grid.add(authorInput, 1, 1);
        grid.add(wordCountInfo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleInput.getText().trim();
                String author = authorInput.getText().trim();
                if (title.isEmpty()) {
                    title = defaultTitle;
                }
                if (author.isEmpty()) {
                    author = "Unknown";
                }
                return new Paper(
                        user.getUserId(),
                        title,
                        author,
                        selectedFile.getAbsolutePath(),
                        extractedText
                );
            }
            return null;
        });

        Optional<Paper> result = dialog.showAndWait();
        if (result.isPresent()) {
            Paper paperToSave = result.get();
            boolean saved = paperDAO.addPaper(paperToSave);
            if (saved) {
                loadDashboardData();
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Upload Successful");
                successAlert.setHeaderText("Paper Saved in MySQL Database!");
                successAlert.setContentText("Paper: " + paperToSave.getTitle() + "\n"
                        + "Author: " + paperToSave.getAuthor() + "\n"
                        + "Extracted Words: " + pdfService.calculateWordCount(extractedText) + "\n\n"
                        + "Click 'View' in the table to open AI Summary, Paragraph Explainer, and Ask AI.");
                successAlert.showAndWait();
            } else {
                Alert errAlert = new Alert(Alert.AlertType.ERROR);
                errAlert.setTitle("Database Error");
                errAlert.setContentText("Could not save the extracted paper into MySQL.");
                errAlert.showAndWait();
            }
        }
    }

    private void handleDeletePaper(Paper paper) {
        User user = SessionManager.getCurrentUser();
        if (user == null || paper == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Research Paper?");
        confirm.setContentText("Are you sure you want to delete '" + paper.getTitle() + "'?\n"
                + "This will also remove all associated summaries and Q&A history (ON DELETE CASCADE).");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = paperDAO.deletePaper(paper.getPaperId(), user.getUserId());
            if (deleted) {
                loadDashboardData();
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setContentText("Could not delete paper from database.");
                errorAlert.showAndWait();
            }
        }
    }

    private void handleLogout() {
        SessionManager.logout();
        ScreenManager.showLoginScreen();
    }
}
