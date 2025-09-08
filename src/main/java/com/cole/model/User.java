package com.cole.model;

/**
 * Represents a user in the system.
 * Extend this class with user fields (e.g., id, username, passwordHash, etc.) as needed.
 */
public class User {
    private int id;
    private String username;
    private String role; // e.g., "ADMIN", "MANAGER", "USER"

    public User(int id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
