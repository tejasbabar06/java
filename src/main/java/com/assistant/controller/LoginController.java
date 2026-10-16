package com.assistant.controller;

import com.assistant.dao.UserDAO;
import com.assistant.model.User;
import com.assistant.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Controller handling the user login screen, input validation,
 * and authentication with UserDAO.
 */
public class LoginController {

    private final UserDAO userDAO = new UserDAO();
    private VBox view;

    public LoginController() {
        createView();
    }

    public Parent getView() {
        return view;
    }

    private void createView() {
        view = new VBox(20);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(30));

        // Center card container
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setMaxWidth(440);
        card.setAlignment(Pos.CENTER_LEFT);

        // Header
        Text title = new Text("Welcome Back");
        title.getStyleClass().add("title-text");

        Text subtitle = new Text("Sign in to your AI Research Paper Assistant account");
        subtitle.getStyleClass().add("subtitle-text");

        // Status message label
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);

        // Email input
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField emailField = new TextField();
        emailField.setPromptText("student@college.edu");
        emailField.setPrefHeight(40);

        // Password input
        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passField.setPrefHeight(40);

        // Login button
        Button loginButton = new Button("Login to Account");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(42);

        // Hyperlink to Register
        Hyperlink registerLink = new Hyperlink("Don't have an account? Register here");
        registerLink.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 13px;");
        registerLink.setOnAction(e -> ScreenManager.showRegisterScreen());

        // Event Handling
        loginButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showError(statusLabel, "Please enter both email and password.");
                return;
            }

            // Attempt login via UserDAO
            User authenticatedUser = userDAO.login(email, password);
            if (authenticatedUser != null) {
                // Store user in active session
                SessionManager.setCurrentUser(authenticatedUser);
                ScreenManager.showDashboardScreen();
            } else {
                showError(statusLabel, "Invalid email or password. Please check your credentials.");
            }
        });

        // Allow pressing Enter key in password field to login
        passField.setOnAction(e -> loginButton.fire());

        card.getChildren().addAll(
                title, subtitle, statusLabel,
                emailLabel, emailField,
                passLabel, passField,
                loginButton, registerLink
        );

        view.getChildren().add(card);
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.getStyleClass().removeAll("status-success", "status-error");
        label.getStyleClass().add("status-error");
        label.setVisible(true);
    }
}
