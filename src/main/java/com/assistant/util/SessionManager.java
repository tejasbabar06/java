package com.assistant.util;

import com.assistant.model.User;

/**
 * Manages the current user session across JavaFX screens.
 * Allows controllers to retrieve the logged-in user's ID for database queries.
 */
public class SessionManager {

    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }
}
