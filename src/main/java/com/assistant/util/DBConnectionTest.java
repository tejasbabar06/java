package com.assistant.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone test class to verify MySQL database connectivity and table schema.
 * Can be run directly from IDE (Spring Tool Suite / IntelliJ) as a standard Java Application.
 */
public class DBConnectionTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  AI Research Paper Assistant - Database Connection Test  ");
        System.out.println("==========================================================");

        System.out.println("[INFO] Testing database connection with settings from config.properties...");
        System.out.println("[INFO] URL:  " + DBConnection.getDbUrl());
        System.out.println("[INFO] User: " + DBConnection.getDbUser());
        System.out.println("----------------------------------------------------------");

        // Try-with-resources automatically closes Connection and Statement
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            if (conn != null && !conn.isClosed()) {
                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("[SUCCESS] Connected to database server successfully!");
                System.out.println("[INFO] Database Product : " + metaData.getDatabaseProductName());
                System.out.println("[INFO] Server Version   : " + metaData.getDatabaseProductVersion());
                System.out.println("[INFO] Active Catalog    : " + conn.getCatalog());
                System.out.println("----------------------------------------------------------");

                // Verify tables exist in the database
                System.out.println("[INFO] Verifying application tables in schema...");
                List<String> foundTables = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery("SHOW TABLES;")) {
                    while (rs.next()) {
                        foundTables.add(rs.getString(1));
                    }
                }

                String[] expectedTables = {"users", "papers", "summaries", "notes", "quiz", "chat_history"};
                boolean allFound = true;
                for (String expected : expectedTables) {
                    if (foundTables.contains(expected)) {
                        System.out.println("  [OK] Table found: " + expected);
                    } else {
                        System.out.println("  [MISSING] Table NOT found: " + expected);
                        allFound = false;
                    }
                }

                System.out.println("----------------------------------------------------------");
                if (allFound) {
                    System.out.println("[RESULT] SUCCESS! All 6 tables are verified and ready for use.");
                } else {
                    System.out.println("[WARN] Some tables are missing. Please execute src/main/resources/db/schema.sql.");
                }
            }
        } catch (SQLException e) {
            System.err.println("\n[ERROR] Database connection failed!");
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message: " + e.getMessage());
            System.err.println("\n=== TROUBLESHOOTING TIPS ===");
            System.err.println("1. Verify MySQL Server service is running (e.g., MySQL96 / MySQL80 in Windows Services).");
            System.err.println("2. Check credentials in src/main/resources/config.properties (db.user and db.password).");
            System.err.println("3. Ensure the database 'ai_research_assistant' exists (run src/main/resources/db/schema.sql).");
            System.err.println("4. Check if MySQL port 3306 is not blocked by a firewall or another service.");
        } finally {
            // Clean up MySQL driver's background connection cleanup thread
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
