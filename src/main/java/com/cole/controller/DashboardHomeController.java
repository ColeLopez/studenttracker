package com.cole.controller;

import com.cole.Service.DashboardService;
import com.cole.Service.TodoService;
import com.cole.model.ToDoTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;
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
    private TableView recentActivityTable;

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
    @FXML private ComboBox<String> todoPriorityCombo;
    @FXML private Label todoSummaryLabel;
    @FXML private CheckBox overdueFilterCheckBox;
    @FXML private ComboBox<String> filterCombo;
    @FXML private TextField searchField;

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
        todoPriorityCombo.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        todoPriorityCombo.getSelectionModel().select("Medium");
        todoRecurringCombo.setItems(FXCollections.observableArrayList("None", "DAILY", "WEEKLY", "MONTHLY"));
        todoRecurringCombo.getSelectionModel().select("None");
        todoListView.setItems(todoTasks);

        todoListView.setCellFactory(lv -> {
            ListCell<ToDoTask> cell = new ListCell<>() {
                private final CheckBox checkBox = new CheckBox();
                private final FontIcon deleteIcon = new FontIcon("fas-trash");
                private final Button deleteBtn = new Button();
                private final Label label = new Label();
                private final HBox hbox = new HBox(8, checkBox, label, deleteBtn);

                {
                    label.setWrapText(true);
                    label.setMaxWidth(220); // Adjust as needed for your layout
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // Style the icon and button
                    deleteIcon.setIconSize(18);
                    deleteIcon.setIconSize(18);
                    deleteIcon.setIconColor(javafx.scene.paint.Color.web("#d90429"));
                    deleteBtn.setGraphic(deleteIcon);
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                    checkBox.setOnAction(e -> {
                        ToDoTask task = getItem();
                        if (task != null) {
                            task.setCompleted(checkBox.isSelected());
                            TodoService.updateTask(task);
                            refreshTodoTasks();
                        }
                    });
                    deleteBtn.setOnAction(e -> {
                        ToDoTask task = getItem();
                        if (task != null) {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Delete Task");
                            alert.setHeaderText("Are you sure you want to delete this task?");
                            alert.setContentText(task.getTaskText());
                            alert.showAndWait().ifPresent(result -> {
                                if (result == ButtonType.OK) {
                                    TodoService.deleteTask(task.getId());
                                    refreshTodoTasks();
                                }
                            });
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
                            display += "\n" + task.getNote();
                        }
                        // Priority color
                        String color = switch (task.getPriority() == null ? "Medium" : task.getPriority()) {
                            case "High" -> "#d90429";
                            case "Low" -> "#2b9348";
                            default -> "#212529";
                        };
                        label.setText(display);
                        label.setStyle("-fx-text-fill: " + color + ";" +
                                (task.isCompleted() ? " -fx-strikethrough: true;" : "") +
                                (task.getDueDate().isBefore(LocalDate.now()) && !task.isCompleted() ? " -fx-background-color: #ffe5e5;" : ""));
                        setGraphic(hbox);
                    }
                }
            };
            cell.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !cell.isEmpty()) {
                    showEditTaskDialog(cell.getItem());
                }
            });
            return cell;
        });

        todoDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshTodoTasks());
        overdueFilterCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> refreshTodoTasks());
        filterCombo.setItems(FXCollections.observableArrayList("All", "Completed", "Pending", "Overdue", "High", "Medium", "Low"));
        filterCombo.getSelectionModel().select("All");
        filterCombo.valueProperty().addListener((obs, old, val) -> refreshTodoTasks());
        searchField.textProperty().addListener((obs, old, val) -> refreshTodoTasks());
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
        String priority = todoPriorityCombo.getValue();
        String recurring = todoRecurringCombo.getValue();
        if (text.isEmpty()) return;
        if ("None".equals(recurring)) recurring = null;

        ToDoTask newTask = new ToDoTask(
            0, currentUserId, text, todoDatePicker.getValue(), false, note, priority, recurring
        );
        TodoService.addTask(newTask);
        todoInput.clear();
        todoNoteInput.clear();
        todoPriorityCombo.getSelectionModel().select("Medium");
        todoRecurringCombo.getSelectionModel().select("None");
        refreshTodoTasks();
    }

    private void refreshTodoTasks() {
        checkAndGenerateRecurringTasks();
        List<ToDoTask> loaded;
        if (overdueFilterCheckBox.isSelected()) {
            loaded = TodoService.getOverdueTasks(currentUserId, LocalDate.now());
        } else {
            loaded = TodoService.getTasksForUserAndDate(currentUserId, todoDatePicker.getValue());
        }
        String filter = filterCombo.getValue();
        String search = searchField.getText().toLowerCase();
        List<ToDoTask> filtered = loaded.stream()
            .filter(t -> {
                if ("Completed".equals(filter)) return t.isCompleted();
                if ("Pending".equals(filter)) return !t.isCompleted();
                if ("Overdue".equals(filter)) return !t.isCompleted() && t.getDueDate().isBefore(LocalDate.now());
                if ("High".equals(filter) || "Medium".equals(filter) || "Low".equals(filter))
                    return filter.equals(t.getPriority());
                return true;
            })
            .filter(t -> t.getTaskText().toLowerCase().contains(search) || (t.getNote() != null && t.getNote().toLowerCase().contains(search)))
            .toList();
        todoTasks.setAll(filtered);

        long completed = filtered.stream().filter(ToDoTask::isCompleted).count();
        long overdue = filtered.stream().filter(t -> !t.isCompleted() && t.getDueDate().isBefore(LocalDate.now())).count();
        todoSummaryLabel.setText(completed + " of " + filtered.size() + " completed, " + overdue + " overdue");
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

    /**
     * Shows a dialog to edit a to-do task.
     *
     * @param task The task to edit.
     */
    private void showEditTaskDialog(ToDoTask task) {
        Dialog<ToDoTask> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");

        TextField taskField = new TextField(task.getTaskText());
        TextField noteField = new TextField(task.getNote());
        DatePicker datePicker = new DatePicker(task.getDueDate());
        ComboBox<String> recurringBox = new ComboBox<>(FXCollections.observableArrayList("None", "DAILY", "WEEKLY", "MONTHLY"));
        recurringBox.setValue(task.getRecurring() == null ? "None" : task.getRecurring());
        ComboBox<String> priorityBox = new ComboBox<>(FXCollections.observableArrayList("High", "Medium", "Low"));
        priorityBox.setValue(task.getPriority() == null ? "Medium" : task.getPriority());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Task:"), taskField);
        grid.addRow(1, new Label("Note:"), noteField);
        grid.addRow(2, new Label("Due Date:"), datePicker);
        grid.addRow(3, new Label("Recurring:"), recurringBox);
        grid.addRow(4, new Label("Priority:"), priorityBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                task.setTaskText(taskField.getText());
                task.setNote(noteField.getText());
                task.setDueDate(datePicker.getValue());
                task.setRecurring("None".equals(recurringBox.getValue()) ? null : recurringBox.getValue());
                task.setPriority(priorityBox.getValue());
                return task;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedTask -> {
            TodoService.updateTask(updatedTask);
            refreshTodoTasks();
        });
    }

    private void checkAndGenerateRecurringTasks() {
        List<ToDoTask> recTasks = TodoService.getRecurringTasksForUser(currentUserId);
        LocalDate today = LocalDate.now();
        for (ToDoTask task : recTasks) {
            boolean shouldGenerate = switch (task.getRecurring()) {
                case "DAILY" -> !TodoService.existsForDate(task, today);
                case "WEEKLY" -> today.getDayOfWeek() == task.getDueDate().getDayOfWeek() && !TodoService.existsForDate(task, today);
                case "MONTHLY" -> today.getDayOfMonth() == task.getDueDate().getDayOfMonth() && !TodoService.existsForDate(task, today);
                default -> false;
            };
            if (shouldGenerate) {
                ToDoTask newTask = new ToDoTask(0, currentUserId, task.getTaskText(), today, false, task.getNote(), task.getPriority(), task.getRecurring());
                TodoService.addTask(newTask);
            }
        }
    }
}
