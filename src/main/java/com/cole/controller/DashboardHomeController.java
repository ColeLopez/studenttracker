package com.cole.controller;

import com.cole.Service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
/**
 * Controller for the Dashboard Home view.
 * This class handles the initialization and loading of dashboard statistics
 * such as student count and upcoming graduations.
 */ 
public class DashboardHomeController {
    @FXML
    private Label inactiveStudentsLabel;

    @FXML
    private Label slpCountLabel;

    @FXML
    private Label pendingFollowUpsLabel;

    @FXML
    private Label upcomingFollowUpsLabel;

    @FXML
    private Label overdueFollowUpsLabel;

    @FXML
    private Label graduatesLabel;

    @FXML
    private Label upcomingGraduatesLabel;

    @FXML
    private ListView todoListView;

    @FXML
    private TextField todoInput;

    @FXML
    private TableView recentActivityTable;


    private static final Logger logger = LoggerFactory.getLogger(DashboardHomeController.class);
    private final DashboardService dashboardService = new DashboardService();

    @FXML private Label activeStudentsLabel;

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
    public void loadDashboardStats() {
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                int studentCount = dashboardService.getStudentCount();
                int graduatedCount = dashboardService.getGraduatedCount();
                int activeCount = dashboardService.getActiveStudentCount();
                return new int[] { studentCount, graduatedCount, activeCount };
            }
        };
        task.setOnSucceeded(e -> {
            int[] stats = task.getValue();
            if (stats[0] >= 0) {
                activeStudentsLabel.setText(stats[0] + " Students");
            } else {
                activeStudentsLabel.setText("Error loading stats.");
            }
            if (stats[1] >= 0) {
                upcomingGraduatesLabel.setText(stats[1] + " Graduating");
            } else {
                upcomingGraduatesLabel.setText("Error loading stats.");
            }
            if (stats[2] >= 0) {
                activeStudentsLabel.setText(stats[2] + " Active");
            } else {
                activeStudentsLabel.setText("Error loading stats.");
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load dashboard stats", task.getException());
            activeStudentsLabel.setText("Error loading stats.");
            upcomingGraduatesLabel.setText("Error loading stats.");
            activeStudentsLabel.setText("Error loading stats.");
        });
        new Thread(task).start();
    }

    public void handleAddTodo() {
        String newTask = todoInput.getText().trim();
        if (!newTask.isEmpty()) {
            todoListView.getItems().add(newTask);
            todoInput.clear();
        }
    }

    public void loadRecentActivities() {

    }

    /**
     * Refreshes the dashboard statistics.
     * This method can be called to reload the statistics manually.
     */
    public void refreshDashboard() {
        loadDashboardStats();
    }
}
