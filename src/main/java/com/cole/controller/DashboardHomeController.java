package com.cole.controller;

import com.cole.util.DBUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardHomeController {

    @FXML private Label studentCountLabel;
    @FXML private Label upcomingGraduationsLabel;

    @FXML
    public void initialize() {
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM students");
            if (rs.next()) {
                studentCountLabel.setText(rs.getInt(1) + " Students");
            }

            rs = stmt.executeQuery("SELECT COUNT(*) FROM students WHERE status = 'Graduated'");
            if (rs.next()) {
                upcomingGraduationsLabel.setText(rs.getInt(1) + " Graduates");
            }

        } catch (Exception e) {
            studentCountLabel.setText("Error loading stats.");
            upcomingGraduationsLabel.setText("Error loading stats.");
            e.printStackTrace();
        }
    }
}
