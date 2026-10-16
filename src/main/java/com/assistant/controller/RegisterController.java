package com.assistant.controller;

import com.assistant.dao.UserDAO;
import com.assistant.model.User;
import com.assistant.util.PasswordUtil;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

/**
 * Controller handling user registration, client-side input validation,
 * password hashing, and database persistence.
 */
public class RegisterController {

    private final UserDAO userDAO = new UserDAO();
    private VBox view;

    public RegisterController() {
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
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        card.setAlignment(Pos.CENTER_LEFT);

        // Header
        Text title = new Text("Create an Account");
        title.getStyleClass().add("title-text");

        Text subtitle = new Text("Register as a student to analyze research papers");
        subtitle.getStyleClass().add("subtitle-text");

        // Status message label
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);

        // Name input
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField nameField = new TextField();
        nameField.setPromptText("John Doe");
        nameField.setPrefHeight(38);

        // Email input
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField emailField = new TextField();
        emailField.setPromptText("student@college.edu");
        emailField.setPrefHeight(38);

        // Password input
        Label passLabel = new Label("Password (min 6 characters)");
        passLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Create a password");
        passField.setPrefHeight(38);

        // Confirm Password input
        Label confirmPassLabel = new Label("Confirm Password");
        confirmPassLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Re-enter password");
        confirmPassField.setPrefHeight(38);

        // Register button
        Button registerButton = new Button("Register Account");
        registerButton.getStyleClass().add("btn-primary");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setPrefHeight(42);

        // Hyperlink to Login
        Hyperlink loginLink = new Hyperlink("Already have an account? Sign In");
        loginLink.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 13px;");
        loginLink.setOnAction(e -> ScreenManager.showLoginScreen());

        // Event Handling
        registerButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            String password = passField.getText().trim();
            String confirmPassword = confirmPassField.getText().trim();

            // 1. Validation: Empty fields
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showError(statusLabel, "Please fill in all fields.");
                return;
            }

            // 2. Validation: Email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                showError(statusLabel, "Please enter a valid email address.");
                return;
            }

            // 3. Validation: Password length
            if (password.length() < 6) {
                showError(statusLabel, "Password must be at least 6 characters long.");
                return;
            }

            // 4. Validation: Password match
            if (!password.equals(confirmPassword)) {
                showError(statusLabel, "Passwords do not match. Please try again.");
                return;
            }

            // 5. Validation: Unique email
            if (userDAO.isEmailRegistered(email)) {
                showError(statusLabel, "An account with this email is already registered.");
                return;
            }

            // 6. Hash password with BCrypt
            String hashedPassword = PasswordUtil.hashPassword(password);
            User newUser = new User(name, email, hashedPassword);

            // 7. Persist to MySQL database via UserDAO
            boolean success = userDAO.registerUser(newUser);
            if (success) {
                showSuccess(statusLabel, "Registration successful! Redirecting to login...");
                registerButton.setDisable(true);

                // Wait 1.5 seconds so user reads success message, then switch to login
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(event -> ScreenManager.showLoginScreen());
                delay.play();
            } else {
                showError(statusLabel, "Registration failed due to a database error. Please try again.");
            }
        });

        card.getChildren().addAll(
                title, subtitle, statusLabel,
                nameLabel, nameField,
                emailLabel, emailField,
                passLabel, passField,
                confirmPassLabel, confirmPassField,
                registerButton, loginLink
        );

        view.getChildren().add(card);
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.getStyleClass().removeAll("status-success", "status-error");
        label.getStyleClass().add("status-error");
        label.setVisible(true);
    }

    private void showSuccess(Label label, String message) {
        label.setText(message);
        label.getStyleClass().removeAll("status-success", "status-error");
        label.getStyleClass().add("status-success");
        label.setVisible(true);
    }
}
