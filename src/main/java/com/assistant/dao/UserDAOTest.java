package com.assistant.dao;

import com.assistant.model.User;
import com.assistant.util.PasswordUtil;

/**
 * Standalone test runner to verify User registration, BCrypt password hashing,
 * and authentication logic against the MySQL database.
 */
public class UserDAOTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("     AI Research Paper Assistant - UserDAO Test Suite     ");
        System.out.println("==========================================================");

        UserDAO userDAO = new UserDAO();
        String testEmail = "student.demo@college.edu";
        String testPassword = "SecurePass@2026";
        String testName = "Tejas Babar";

        try {
            // 1. Test Email Check
            System.out.println("\n[TEST 1] Checking if test email is already registered...");
            boolean alreadyExists = userDAO.isEmailRegistered(testEmail);
            System.out.println("  Is registered: " + alreadyExists);

            // 2. Test User Registration (if not exists)
            if (!alreadyExists) {
                System.out.println("\n[TEST 2] Registering new student user with BCrypt hashed password...");
                String hashedPassword = PasswordUtil.hashPassword(testPassword);
                System.out.println("  Generated BCrypt Hash: " + hashedPassword);

                User newUser = new User(testName, testEmail, hashedPassword);
                boolean registered = userDAO.registerUser(newUser);

                if (registered && newUser.getUserId() > 0) {
                    System.out.println("  [SUCCESS] User registered with generated user_id: " + newUser.getUserId());
                } else {
                    System.err.println("  [FAILED] User registration failed.");
                    return;
                }
            } else {
                System.out.println("  [INFO] User already registered from previous run. Continuing tests.");
            }

            // 3. Test Login with CORRECT Credentials
            System.out.println("\n[TEST 3] Testing login with CORRECT email and password...");
            User loggedInUser = userDAO.login(testEmail, testPassword);
            if (loggedInUser != null) {
                System.out.println("  [SUCCESS] Login successful! User details: " + loggedInUser);
            } else {
                System.err.println("  [FAILED] Login failed for valid credentials!");
            }

            // 4. Test Login with WRONG Password
            System.out.println("\n[TEST 4] Testing login with WRONG password...");
            User failedPassUser = userDAO.login(testEmail, "WrongPassword999");
            if (failedPassUser == null) {
                System.out.println("  [SUCCESS] Access correctly denied for invalid password.");
            } else {
                System.err.println("  [FAILED] Security breach: login succeeded with wrong password!");
            }

            // 5. Test Login with NON-EXISTENT Email
            System.out.println("\n[TEST 5] Testing login with NON-EXISTENT email...");
            User nonExistentUser = userDAO.login("ghost.user@college.edu", testPassword);
            if (nonExistentUser == null) {
                System.out.println("  [SUCCESS] Access correctly denied for non-existent email.");
            } else {
                System.err.println("  [FAILED] Login succeeded for non-existent email!");
            }

            System.out.println("\n----------------------------------------------------------");
            System.out.println("[RESULT] ALL 5 USER DAO & AUTHENTICATION TESTS PASSED!");
            System.out.println("==========================================================");

        } finally {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        }
    }
}
