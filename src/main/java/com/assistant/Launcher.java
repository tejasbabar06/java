package com.assistant;

/**
 * Entry point launcher for the AI Research Paper Assistant.
 * Calling MainApp through a separate Launcher class avoids JavaFX module-path
 * runtime check issues when running as a standalone or shaded JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
