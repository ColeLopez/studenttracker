package com.cole.controller;

import com.cole.Service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

// ...existing code...

/**
 * Controller for the dashboard home view.
 * <p>
 * Handles UI events and database operations for displaying student and graduation statistics.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Loading student and graduation counts from the database</li>
 *   <li>Updating dashboard labels with statistics</li>
 *   <li>Showing error messages if statistics cannot be loaded</li>
 * </ul>
 * <p>
 * All database operations are performed asynchronously using JavaFX Tasks.
 */
public class DashboardHomeController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardHomeController.class);
    private final DashboardService dashboardService = new DashboardService();

    @FXML private Label studentCountLabel;
    @FXML private Label upcomingGraduationsLabel;

    /**
     * Initializes the controller and loads dashboard statistics.
     * This method is called automatically by the JavaFX framework after FXML loading.
     */
    @FXML
    public void initialize() {
        loadDashboardStats();
    }

    /**
     * Loads student and graduation statistics from the database.
     * Updates the dashboard labels with the retrieved values.
     * Shows error messages if statistics cannot be loaded.
     */
    private void loadDashboardStats() {
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                int studentCount = dashboardService.getStudentCount();
                int graduatedCount = dashboardService.getGraduatedCount();
                return new int[] { studentCount, graduatedCount };
            }
        };
        task.setOnSucceeded(e -> {
            int[] stats = task.getValue();
            if (stats[0] >= 0) {
                studentCountLabel.setText(stats[0] + " Students");
            } else {
                studentCountLabel.setText("Error loading stats.");
            }
            if (stats[1] >= 0) {
                upcomingGraduationsLabel.setText(stats[1] + " Graduates");
            } else {
                upcomingGraduationsLabel.setText("Error loading stats.");
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load dashboard stats", task.getException());
            studentCountLabel.setText("Error loading stats.");
            upcomingGraduationsLabel.setText("Error loading stats.");
        });
        new Thread(task).start();
    }
}
