package com.assistant;

import com.assistant.controller.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main JavaFX Application class.
 * Initializes the primary stage and ScreenManager, loading the Login view by default.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize the screen coordinator with the primary stage
        ScreenManager.init(primaryStage);

        // Display the login screen on launch
        ScreenManager.showLoginScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
