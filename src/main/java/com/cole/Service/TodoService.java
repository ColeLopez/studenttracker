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
        String sql = "SELECT * FROM todos WHERE user_id = ? AND due_date = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date.toString());
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
        String sql = "INSERT INTO todos (user_id, task_text, due_date, completed, note, recurring) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, task.getUserId());
            ps.setString(2, task.getTaskText());
            ps.setString(3, task.getDueDate().toString());
            ps.setInt(4, task.isCompleted() ? 1 : 0);
            ps.setString(5, task.getNote());
            ps.setString(6, task.getRecurring());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateTask(ToDoTask task) {
        String sql = "UPDATE todos SET task_text=?, due_date=?, completed=?, note=?, recurring=? WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTaskText());
            ps.setString(2, task.getDueDate().toString());
            ps.setInt(3, task.isCompleted() ? 1 : 0);
            ps.setString(4, task.getNote());
            ps.setString(5, task.getRecurring());
            ps.setInt(6, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static ToDoTask mapRow(ResultSet rs) throws SQLException {
        return new ToDoTask(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("task_text"),
            LocalDate.parse(rs.getString("due_date")),
            rs.getInt("completed") == 1,
            rs.getString("note"),
            rs.getString("recurring")
        );
    }
}
