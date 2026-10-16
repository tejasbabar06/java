package com.assistant.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for secure password hashing and verification using BCrypt.
 * Protects user credentials against rainbow table and brute-force attacks.
 */
public class PasswordUtil {

    // Log rounds determine the computational work factor (12 is standard and secure)
    private static final int LOG_ROUNDS = 12;

    /**
     * Hashes a plain-text password using a randomly generated salt.
     * 
     * @param plainPassword The user's plain-text password
     * @return 60-character BCrypt hashed password string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Verifies if an input plain-text password matches a stored BCrypt hash.
     * 
     * @param plainPassword  The plain-text candidate password
     * @param hashedPassword The stored BCrypt hash from the database
     * @return true if password matches, false otherwise
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Malformed hash in database
            return false;
        }
    }
}
