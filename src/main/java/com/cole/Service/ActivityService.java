package com.cole.Service;

import com.cole.model.RecentActivity;
import com.cole.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivityService {
    // Use this formatter for 'yyyy-MM-dd HH:mm:ss'
    private static final DateTimeFormatter DB_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 
     * @param userId
     * @param type
     * @param description
     */
    public static void logActivity(int userId, String type, String description) {
        String sql = "INSERT INTO activity_log (user_id, activity_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get recent activities for a user.
     * @param limit
     * @return
     */
    public static List<RecentActivity> getRecentActivities(int limit) {
        List<RecentActivity> activities = new ArrayList<>();
        String sql = "SELECT * FROM activity_log ORDER BY activity_time DESC LIMIT ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RecentActivity activity = new RecentActivity(limit, limit, sql, sql, null);
                activity.setId(rs.getInt("id"));
                activity.setUserId(rs.getInt("user_id"));
                activity.setActivityType(rs.getString("activity_type"));
                activity.setDescription(rs.getString("description"));
                activity.setActivityTime(LocalDateTime.parse(rs.getString("activity_time"), DB_TIMESTAMP_FORMAT));
                activities.add(activity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activities;
    }

    /**
     * Get recent activities filtered by userId or role.
     * @param userId
     * @param role
     * @param limit
     * @return
     */
    public static List<RecentActivity> getRecentActivitiesByUserOrRole(Integer userId, String role, int limit) {
        List<RecentActivity> activities = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT a.*, u.username, u.role FROM activity_log a JOIN users u ON a.user_id = u.id");
        if (userId != null) {
            sql.append(" WHERE a.user_id = ?");
        } else if (role != null) {
            sql.append(" WHERE u.role = ?");
        }
        sql.append(" ORDER BY a.activity_time DESC LIMIT ?");
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (userId != null) ps.setInt(idx++, userId);
            else if (role != null) ps.setString(idx++, role);
            ps.setInt(idx, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RecentActivity activity = new RecentActivity(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("activity_type"),
                    rs.getString("description"),
                    rs.getTimestamp("activity_time").toLocalDateTime()
                );
                activity.setUserDisplayName(rs.getString("username"));
                activity.setUserRole(rs.getString("role"));
                activities.add(activity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activities;
    }

    public static List<String> getAllUsernames() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static Integer getUserIdByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
