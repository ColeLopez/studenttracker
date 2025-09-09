package com.cole.util;

import com.cole.model.User;

public class UserSession {
    private static UserSession instance;
    private User user;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public int getUserId() {
        return user != null ? user.getId() : -1;
    }

    public static boolean isHighLevelUser() {
        String role = getInstance().getUserRole();
        return "ADMIN".equalsIgnoreCase(role)
            || "MANAGER".equalsIgnoreCase(role)
            || "TRAINING_ADVISOR".equalsIgnoreCase(role);
    }

    public String getUserRole() {
        return user != null ? user.getRole() : null;
    }

    public void clear() {
        user = null;
    }
}
