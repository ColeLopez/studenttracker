package com.cole.Service;

import com.cole.model.ToDoTask;
import com.cole.util.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TodoService {
    public static List<ToDoTask> getTasksForUserAndDate(int userId, LocalDate date) {
        List<ToDoTask> tasks = new ArrayList<>();
        String sql;
        boolean filterByDate = date != null;
        if (filterByDate) {
            sql = "SELECT * FROM todos WHERE user_id = ? AND due_date = ?";
        } else {
            sql = "SELECT * FROM todos WHERE user_id = ?";
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (filterByDate) {
                ps.setString(2, date.toString());
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public static List<ToDoTask> getOverdueTasks(int userId, LocalDate today) {
        List<ToDoTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM todos WHERE user_id = ? AND due_date < ? AND completed = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, today.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public static void addTask(ToDoTask task) {
        String sql = "INSERT INTO todos (user_id, task_text, due_date, completed, note, priority, recurring, active, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, task.getUserId());
            ps.setString(2, task.getTaskText());
            ps.setString(3, task.getDueDate().toString());
            ps.setInt(4, task.isCompleted() ? 1 : 0);
            ps.setString(5, task.getNote());
            ps.setString(6, task.getPriority());
            ps.setString(7, task.getRecurring());
            ps.setInt(8, task.isActive() ? 1 : 0);
            if (task.getParentId() != null) {
                ps.setInt(9, task.getParentId());
            } else {
                ps.setNull(9, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTask(ToDoTask task) {
        String sql = "UPDATE todos SET task_text=?, due_date=?, completed=?, note=?, priority=?, recurring=?, active=?, parent_id=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTaskText());
            ps.setString(2, task.getDueDate().toString());
            ps.setInt(3, task.isCompleted() ? 1 : 0);
            ps.setString(4, task.getNote());
            ps.setString(5, task.getPriority());
            ps.setString(6, task.getRecurring());
            ps.setInt(7, task.isActive() ? 1 : 0);
            if (task.getParentId() != null) {
                ps.setInt(8, task.getParentId());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            ps.setInt(9, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<ToDoTask> getRecurringTasksForUser(int userId) {
        List<ToDoTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM todos WHERE user_id = ? AND recurring IS NOT NULL AND active = 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tasks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public static boolean existsForDate(ToDoTask task, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM todos WHERE user_id = ? AND task_text = ? AND due_date = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, task.getUserId());
            ps.setString(2, task.getTaskText());
            ps.setString(3, date.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void deleteTask(int taskId) {
        String sql = "DELETE FROM todos WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setTaskActive(int taskId, boolean active) {
        String sql = "UPDATE todos SET active=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isDateExcluded(int taskId, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM todo_recurring_exclusions WHERE task_id = ? AND excluded_date = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setString(2, date.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addRecurringExclusion(int taskId, LocalDate date) {
        String sql = "INSERT INTO todo_recurring_exclusions (task_id, excluded_date) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setString(2, date.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<LocalDate> getExclusionsForTask(int taskId) {
        List<LocalDate> exclusions = new ArrayList<>();
        String sql = "SELECT excluded_date FROM todo_recurring_exclusions WHERE task_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                exclusions.add(LocalDate.parse(rs.getString("excluded_date")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exclusions;
    }

    public static void removeRecurringExclusion(int taskId, LocalDate date) {
        String sql = "DELETE FROM todo_recurring_exclusions WHERE task_id = ? AND excluded_date = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setString(2, date.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static ToDoTask mapRow(ResultSet rs) throws SQLException {
        ToDoTask task = new ToDoTask(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("task_text"),
            LocalDate.parse(rs.getString("due_date")),
            rs.getInt("completed") == 1,
            rs.getString("note"),
            rs.getString("priority"),
            rs.getString("recurring")
        );
        // Set active flag if present
        try {
            task.setActive(rs.getInt("active") == 1);
        } catch (SQLException e) {
            task.setActive(true);
        }
        // Set parentId if present
        try {
            int parentId = rs.getInt("parent_id");
            if (!rs.wasNull()) {
                task.setParentId(parentId);
            }
        } catch (SQLException e) {
            task.setParentId(null);
        }
        return task;
    }
}
