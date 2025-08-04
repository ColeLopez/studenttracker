package com.cole.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.cole.Service.EmailServices;
import com.cole.model.FollowUpRow;
import com.cole.util.DBUtil;
import com.cole.util.EmailDialogUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FollowUpController {
    private static final Logger logger = LoggerFactory.getLogger(FollowUpController.class);

    @FXML private TableView<FollowUpRow> followUpTable;
    @FXML private Button reminderButton;
    @FXML private TableColumn<FollowUpRow, String> studentNumberColumn;
    @FXML private TableColumn<FollowUpRow, String> studentNameColumn;
    @FXML private TableColumn<FollowUpRow, String> emailColumn;
    @FXML private TableColumn<FollowUpRow, String> phoneColumn;
    @FXML private TableColumn<FollowUpRow, String> descriptionColumn;
    @FXML private TableColumn<FollowUpRow, String> dueDateColumn;
    @FXML private TableColumn<FollowUpRow, Boolean> completedColumn;

    private ObservableList<FollowUpRow> followUpRows = FXCollections.observableArrayList();

    /**
     * Initializes the controller and sets up the table view.
     * This method is called automatically by the JavaFX framework after FXML loading.
     */
    @FXML
    private void initialize() {
        setupTableColumns();
        setupRowColorCoding();
        setupCompletedColumn();
        loadFollowUps();
    }

    /**
     * Sets up the table columns with their respective cell value factories.
     * This method binds the data from FollowUpRow to the table columns.
     */
    private void setupTableColumns() {
        studentNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStudentNumber()));
        studentNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStudentName()));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        phoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        dueDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDueDate()));
    }

    /**
     * Sets up the row color coding based on the due date and completion status.
     * Overdue follow-ups will be highlighted in red.
     */
    private void setupRowColorCoding() {
        followUpTable.setRowFactory(tableView -> new TableRow<FollowUpRow>() {
            @Override
            protected void updateItem(FollowUpRow row, boolean empty) {
                super.updateItem(row, empty);
                if (row == null || empty) {
                    setStyle("");
                } else {
                    LocalDate dueDate = LocalDate.parse(row.getDueDate());
                    if (!row.isCompleted() && dueDate.isBefore(LocalDate.now())) {
                        setStyle("-fx-background-color: #ffcccc;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    /**
     * Sets up the completed column with a checkbox cell factory.
     * This allows users to mark follow-ups as completed directly in the table.
     */
    private void setupCompletedColumn() {
        completedColumn.setCellValueFactory(cellData -> cellData.getValue().completedProperty());
        completedColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        completedColumn.setEditable(true);
        followUpTable.setEditable(true);

        completedColumn.setOnEditCommit(event -> {
            FollowUpRow row = event.getRowValue();
            boolean newValue = event.getNewValue();
            row.setCompleted(newValue);
            updateFollowUpCompleted(row.getFollowUpId(), newValue);
            loadFollowUps();
        });
    }

    /**
     * Loads follow-up data from the database and populates the table.
     * This method retrieves all incomplete follow-ups and displays them in the table.
     */
    private void loadFollowUps() {
        followUpRows.clear();
        String sql = 
            "SELECT f.followup_id, s.student_number, s.first_name || ' ' || s.last_name AS full_name, " +
            "s.email, s.phone, f.description, f.due_date, f.completed " +
            "FROM follow_ups f " +
            "JOIN students s ON f.student_id = s.student_id " +
            "WHERE f.completed = 0 " +
            "ORDER BY f.due_date ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                FollowUpRow row = new FollowUpRow(
                    rs.getInt("followup_id"),
                    rs.getString("student_number"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("description"),
                    rs.getString("due_date"),
                    rs.getBoolean("completed")
                );
                row.completedProperty().addListener((obs, oldVal, newVal) -> {
                    updateFollowUpCompleted(row.getFollowUpId(), newVal);
                    loadFollowUps();
                });
                followUpRows.add(row);
            }
        } catch (Exception e) {
            logger.error("Error Retrieving Follow-Ups", e);
            showError("Error Retrieving Follow-Ups", e.getMessage());
        }
        followUpTable.setItems(followUpRows);
    }

    /**
     * Handles the reminder button click event.
     * This method sends an email reminder for upcoming, current, and overdue follow-ups.
     */
    @FXML
    private void handleReminderButton() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);
        List<FollowUpRow> upcoming = filterFollowUps(row -> 
            !row.isCompleted() && 
            !LocalDate.parse(row.getDueDate()).isBefore(today) && 
            !LocalDate.parse(row.getDueDate()).isAfter(sevenDaysFromNow)
        );
        String emailBody = buildEmailBody("Upcoming Follow-Ups (Next 7 Days):", upcoming);
        sendReminderEmail("Upcoming Follow-Ups", emailBody);
    }

    /**
     * Handles the current follow-ups button click event.
     * This method sends an email reminder for follow-ups due today.
     */
    @FXML
    private void handleCurrentFollowupsButton() {
        LocalDate today = LocalDate.now();
        List<FollowUpRow> current = filterFollowUps(row -> 
            !row.isCompleted() && LocalDate.parse(row.getDueDate()).isEqual(today)
        );
        String emailBody = buildEmailBody("Current Follow-Ups (Due Today):", current);
        sendReminderEmail("Current Follow-Ups", emailBody);
    }

    /**
     * Handles the overdue follow-ups button click event.
     * This method sends an email reminder for overdue follow-ups.
     */
    @FXML
    private void handleOverdueFollowupsButton() {
        LocalDate today = LocalDate.now();
        List<FollowUpRow> overdue = filterFollowUps(row -> 
            !row.isCompleted() && LocalDate.parse(row.getDueDate()).isBefore(today)
        );
        StringBuilder emailBody = new StringBuilder("Overdue Follow-Ups:\n\n");
        for (FollowUpRow row : overdue) {
            LocalDate dueDate = LocalDate.parse(row.getDueDate());
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
            emailBody.append("Student: ").append(row.getStudentName())
                    .append(" (").append(row.getStudentNumber()).append(")\n")
                    .append("Due: ").append(row.getDueDate()).append("\n")
                    .append("Days Overdue: ").append(daysOverdue).append("\n")
                    .append("Description: ").append(row.getDescription()).append("\n\n");
        }
        sendReminderEmail("Overdue Follow-Ups", emailBody.toString());
    }
    /**
     * Handles the filter button for today's follow-ups.
     * This method filters the table to show only follow-ups due today.
     */
    @FXML
    private void handleTodayFilterButton() {
        LocalDate today = LocalDate.now();
        ObservableList<FollowUpRow> todayRows = FXCollections.observableArrayList(
            filterFollowUps(row -> !row.isCompleted() && LocalDate.parse(row.getDueDate()).isEqual(today))
        );
        followUpTable.setItems(todayRows);
    }

    /**
     * Handles the filter button for overdue follow-ups.
     * This method filters the table to show only overdue follow-ups.
     */
    @FXML
    private void handleOverdueFilterButton() {
        LocalDate today = LocalDate.now();
        ObservableList<FollowUpRow> overdueRows = FXCollections.observableArrayList(
            filterFollowUps(row -> !row.isCompleted() && LocalDate.parse(row.getDueDate()).isBefore(today))
        );
        followUpTable.setItems(overdueRows);
    }
    /**
     * Handles the filter button for upcoming follow-ups.
     * This method filters the table to show follow-ups due within the next 7 days.
     */
    @FXML
    private void handleUpcomingFilterButton() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);
        ObservableList<FollowUpRow> upcomingRows = FXCollections.observableArrayList(
            filterFollowUps(row -> 
                !row.isCompleted() && 
                !LocalDate.parse(row.getDueDate()).isBefore(today) && 
                !LocalDate.parse(row.getDueDate()).isAfter(sevenDaysFromNow)
            )
        );
        followUpTable.setItems(upcomingRows);
    }

    /**
     * Filters the follow-up rows based on a given predicate.
     * This method is used to filter the follow-ups for reminders and table views.
     *
     * @param predicate the condition to filter follow-up rows
     * @return a list of filtered follow-up rows
     */
    private List<FollowUpRow> filterFollowUps(java.util.function.Predicate<FollowUpRow> predicate) {
        return followUpRows.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Builds the email body for follow-up reminders.
     * This method formats the follow-up rows into a readable string for the email body.
     *
     * @param header the header text for the email
     * @param rows the list of follow-up rows to include in the email
     * @return a formatted string representing the email body
     */
    private String buildEmailBody(String header, List<FollowUpRow> rows) {
        StringBuilder body = new StringBuilder(header).append("\n\n");
        for (FollowUpRow row : rows) {
            body.append("Student: ").append(row.getStudentName())
                .append(" (").append(row.getStudentNumber()).append(")\n")
                .append("Due: ").append(row.getDueDate()).append("\n")
                .append("Description: ").append(row.getDescription()).append("\n\n");
        }
        return body.toString();
    }

    /**
     * Sends an email reminder with the specified subject and body.
     * This method uses the EmailServices to send the email and handles any errors.
     *
     * @param subject the subject of the email
     * @param body the body content of the email
     */
    private void sendReminderEmail(String subject, String body) {
        Properties config = EmailServices.loadEmailSettings();
        if (config == null) {
            showError("Settings Error", "Could not load email settings.");
            return;
        }
        String from = config.getProperty("email.sender", "");
        String smtp = config.getProperty("email.smtp", "");
        String port = config.getProperty("email.port", "");
        if (from.isEmpty() || smtp.isEmpty() || port.isEmpty()) {
            showError("Settings Error", "Email settings are incomplete.");
            return;
        }
        Stage owner = (Stage) followUpTable.getScene().getWindow();
        String password = EmailDialogUtil.getSessionPassword(owner);
        if (password == null || password.isEmpty()) {
            showError("No Password", "Email not sent: password required.");
            return;
        }
        Task<Void> emailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Authenticating...");
                Thread.sleep(500);
                updateMessage("Sending email...");
                EmailServices.sendEmail(from, password, from, null, subject, body, smtp, port);
                updateMessage("Finishing up...");
                Thread.sleep(300);
                return null;
            }
        };
        emailTask.setOnSucceeded(e -> showInfo("Reminders Sent", "Email reminders have been sent to you."));
        emailTask.setOnFailed(e -> {
            logger.error("Email Error", emailTask.getException());
            showError("Email Error", emailTask.getException().getMessage());
        });
        EmailDialogUtil.showEmailProgressDialog(owner, emailTask, "Sending Email...");
    }

    /**
     * Displays an error alert with the specified title and message.
     * This method is used to show error messages to the user.
     *
     * @param title the title of the error alert
     * @param message the content of the error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays an information alert with the specified title and message.
     * This method is used to show informational messages to the user.
     *
     * @param title the title of the information alert
     * @param message the content of the information message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Updates the completion status of a follow-up in the database.
     * This method is called when a follow-up is marked as completed or uncompleted.
     *
     * @param followUpId the ID of the follow-up to update
     * @param completed the new completion status
     */
    private void updateFollowUpCompleted(int followUpId, boolean completed) {
        String sql = "UPDATE follow_ups SET completed = ? WHERE followup_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, completed);
            stmt.setInt(2, followUpId);
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Error updating follow-up completed status", e);
            showError("Update Error", "Could not update follow-up status.");
        }
    }
}
