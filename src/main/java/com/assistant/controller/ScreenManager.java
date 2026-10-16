package com.assistant.controller;

import com.assistant.model.Paper;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ScreenManager controls scene switching across the application
 * (Login, Registration, Dashboard, and Paper Viewer).
 */
public class ScreenManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Applies global styling and sets the scene on the primary stage.
     */
    public static void setScene(Scene scene, String title, int width, int height) {
        if (primaryStage == null) {
            return;
        }
        var cssUrl = ScreenManager.class.getResource("/css/style.css");
        if (cssUrl != null && !scene.getStylesheets().contains(cssUrl.toExternalForm())) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showLoginScreen() {
        LoginController loginController = new LoginController();
        Scene scene = new Scene(loginController.getView(), 880, 640);
        setScene(scene, "AI Research Paper Assistant - Student Login", 880, 640);
    }

    public static void showRegisterScreen() {
        RegisterController registerController = new RegisterController();
        Scene scene = new Scene(registerController.getView(), 880, 680);
        setScene(scene, "AI Research Paper Assistant - Student Registration", 880, 680);
    }

    public static void showDashboardScreen() {
        DashboardController dashboardController = new DashboardController();
        Scene scene = new Scene(dashboardController.getView(), 1100, 740);
        setScene(scene, "AI Research Paper Assistant - Student Dashboard", 1100, 740);
    }

    public static void showPaperViewer(Paper paper) {
        PaperViewerController viewerController = new PaperViewerController(paper);
        Scene scene = new Scene(viewerController.getView(), 1100, 740);
        String title = (paper != null && paper.getTitle() != null) ? paper.getTitle() : "Paper Viewer";
        setScene(scene, "AI Research Paper Assistant - " + title, 1100, 740);
    }
}
