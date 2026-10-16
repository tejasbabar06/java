package com.assistant.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class to manage MySQL JDBC database connections.
 * Reads database credentials from config.properties to avoid hardcoding.
 */
public class DBConnection {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    // Static block runs once when the class is loaded into memory
    static {
        loadProperties();
    }

    /**
     * Loads database configuration settings from config.properties file,
     * with optional fallback / overrides from environment variables.
     */
    private static void loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                dbUrl = properties.getProperty("db.url");
                dbUser = properties.getProperty("db.user");
                dbPassword = properties.getProperty("db.password");
            } else {
                System.out.println("[DBConnection] Note: config.properties not found in classpath. Checking environment variables...");
            }
        } catch (IOException e) {
            System.err.println("[DBConnection] Failed to load config.properties: " + e.getMessage());
        }

        // Allow environment variables (DB_URL, DB_USER, DB_PASSWORD) to provide or override values
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            dbUrl = envUrl.trim();
        }
        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) {
            dbUser = envUser.trim();
        }
        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) {
            dbPassword = envPass.trim();
        }

        try {
            // Register MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    /**
     * Establishes and returns a new connection to the MySQL database.
     * 
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (dbUrl == null || dbUser == null) {
            throw new SQLException("Database configuration is missing. Check config.properties.");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Helper method to check if the database connection is currently active and valid.
     * 
     * @return true if connection succeeds within 3 seconds, false otherwise.
     */
    public static boolean checkConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Connection check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Getter for current database URL (useful for status displays).
     */
    public static String getDbUrl() {
        return dbUrl;
    }

    /**
     * Getter for configured database user.
     */
    public static String getDbUser() {
        return dbUser;
    }
}
