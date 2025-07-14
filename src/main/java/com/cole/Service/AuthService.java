package com.cole.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cole.util.DBUtil;
import com.cole.util.PasswordUtil;

public class AuthService {
    
    public boolean login(String username, String password) {
        String query = "SELECT password_hash, salt FROM users WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                String hashedInput = PasswordUtil.hashPassword(password, salt);

                return hashedInput.equals(hash);
            } else {
                System.out.println("Username not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    public boolean register(String username, String password) {
        // Implement registration logic here
        // This is a placeholder implementation
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);
        
        String insert = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insert)) {
            
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, salt);
            
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
