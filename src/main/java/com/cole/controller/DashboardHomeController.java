package com.cole.controller;

import com.cole.Service.DashboardService;
import com.cole.Service.TodoService;
import com.cole.model.ToDoTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

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

    @FXML private DatePicker todoDatePicker;
    @FXML private ListView<ToDoTask> todoListView;
    @FXML private TextField todoInput;
    @FXML private TextField todoNoteInput;
    @FXML private ComboBox<String> todoRecurringCombo;
    @FXML private Label todoSummaryLabel;

    private ObservableList<ToDoTask> todoTasks = FXCollections.observableArrayList();
    private static final Logger logger = LoggerFactory.getLogger(DashboardHomeController.class);
    private final DashboardService dashboardService = new DashboardService();
    private int currentUserId; // Set this from your login/session logic

    @FXML private Label activeStudentsLabel;

    /**
     * Initializes the controller and loads dashboard statistics.
     * This method is called automatically by the JavaFX framework after FXML loading.
     */
    @FXML
    public void initialize() {
        todoDatePicker.setValue(LocalDate.now());
        todoRecurringCombo.setItems(FXCollections.observableArrayList("None", "DAILY", "WEEKLY", "MONTHLY"));
        todoRecurringCombo.getSelectionModel().select("None");
        todoListView.setItems(todoTasks);

        todoListView.setCellFactory(lv -> new ListCell<ToDoTask>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    ToDoTask task = getItem();
                    if (task != null) {
                        task.setCompleted(checkBox.isSelected());
                        TodoService.updateTask(task);
                        refreshTodoTasks();
                    }
                });
            }
            @Override
            protected void updateItem(ToDoTask task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    checkBox.setSelected(task.isCompleted());
                    String display = task.getTaskText();
                    if (task.getNote() != null && !task.getNote().isEmpty()) {
                        display += " (" + task.getNote() + ")";
                    }
                    if (task.getDueDate().isBefore(LocalDate.now()) && !task.isCompleted()) {
                        setStyle("-fx-text-fill: red;");
                    } else if (task.isCompleted()) {
                        setStyle("-fx-text-fill: gray; -fx-strikethrough: true;");
                    } else {
                        setStyle("");
                    }
                    setText(display);
                    setGraphic(checkBox);
                }
            }
        });

        todoDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshTodoTasks());
        loadDashboardStats();
        refreshTodoTasks();
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

    @FXML
    private void handleAddTodo() {
        String text = todoInput.getText().trim();
        String note = todoNoteInput.getText().trim();
        String recurring = todoRecurringCombo.getValue();
        if (text.isEmpty()) return;
        if ("None".equals(recurring)) recurring = null;

        ToDoTask newTask = new ToDoTask(
            0, currentUserId, text, todoDatePicker.getValue(), false, note, recurring
        );
        TodoService.addTask(newTask);
        todoInput.clear();
        todoNoteInput.clear();
        todoRecurringCombo.getSelectionModel().select("None");
        refreshTodoTasks();
    }

    private void refreshTodoTasks() {
        List<ToDoTask> loaded = TodoService.getTasksForUserAndDate(currentUserId, todoDatePicker.getValue());
        todoTasks.setAll(loaded);

        long completed = loaded.stream().filter(ToDoTask::isCompleted).count();
        long overdue = loaded.stream().filter(t -> !t.isCompleted() && t.getDueDate().isBefore(LocalDate.now())).count();
        todoSummaryLabel.setText(completed + " of " + loaded.size() + " completed, " + overdue + " overdue");
    }

    /**
     * Refreshes the dashboard statistics.
     * This method can be called to reload the statistics manually.
     */
    public void refreshDashboard() {
        loadDashboardStats();
    }

    // Add a method to set the current user ID after login
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        refreshTodoTasks();
    }
}
