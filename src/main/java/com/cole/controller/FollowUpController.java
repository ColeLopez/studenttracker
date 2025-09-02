package com.cole.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
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

    private final ObservableList<FollowUpRow> followUpRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupTableColumns();
        setupRowColorCoding();
        setupCompletedColumn();
        loadFollowUps();
    }

    private void setupTableColumns() {
        studentNumberColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStudentNumber()));
        studentNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStudentName()));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        phoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhone()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        dueDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDueDate()));
    }

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

    private void setupCompletedColumn() {
        completedColumn.setCellValueFactory(cellData -> cellData.getValue().completedProperty());
        completedColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        completedColumn.setEditable(true);
        followUpTable.setEditable(true);

        // Remove redundant reloads and listeners for better performance
        completedColumn.setOnEditCommit(event -> {
            FollowUpRow row = event.getRowValue();
            boolean newValue = event.getNewValue();
            row.setCompleted(newValue);
            updateFollowUpCompleted(row.getFollowUpId(), newValue);
            // Instead of reloading all, just update the row in-place
            followUpTable.refresh();
        });
    }

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
                followUpRows.add(row);
            }
        } catch (Exception e) {
            logger.error("Error Retrieving Follow-Ups", e);
            showError("Error Retrieving Follow-Ups", e.getMessage());
        }
        followUpTable.setItems(followUpRows);
    }

    @FXML
    private void handleReminderButton() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);
        List<FollowUpRow> upcoming = filterFollowUps(row ->
            !row.isCompleted() &&
            !LocalDate.parse(row.getDueDate()).isBefore(today) &&
            !LocalDate.parse(row.getDueDate()).isAfter(sevenDaysFromNow)
        );
        if (upcoming.isEmpty()) {
            showInfo("No Upcoming Follow-Ups", "There are no upcoming follow-ups in the next 7 days.");
            return;
        }
        String emailBody = buildEmailBody("Upcoming Follow-Ups (Next 7 Days):", upcoming);
        sendReminderEmail("Upcoming Follow-Ups", emailBody);
    }

    @FXML
    private void handleCurrentFollowupsButton() {
        LocalDate today = LocalDate.now();
        List<FollowUpRow> current = filterFollowUps(row ->
            !row.isCompleted() && LocalDate.parse(row.getDueDate()).isEqual(today)
        );
        if (current.isEmpty()) {
            showInfo("No Current Follow-Ups", "There are no follow-ups due today.");
            return;
        }
        String emailBody = buildEmailBody("Current Follow-Ups (Due Today):", current);
        sendReminderEmail("Current Follow-Ups", emailBody);
    }

    @FXML
    private void handleOverdueFollowupsButton() {
        LocalDate today = LocalDate.now();
        List<FollowUpRow> overdue = filterFollowUps(row ->
            !row.isCompleted() && LocalDate.parse(row.getDueDate()).isBefore(today)
        );
        if (overdue.isEmpty()) {
            showInfo("No Overdue Follow-Ups", "There are no overdue follow-ups.");
            return;
        }
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

    @FXML
    private void handleTodayFilterButton() {
        LocalDate today = LocalDate.now();
        followUpTable.setItems(
            followUpRows.filtered(row -> !row.isCompleted() && LocalDate.parse(row.getDueDate()).isEqual(today))
        );
    }

    @FXML
    private void handleOverdueFilterButton() {
        LocalDate today = LocalDate.now();
        followUpTable.setItems(
            followUpRows.filtered(row -> !row.isCompleted() && LocalDate.parse(row.getDueDate()).isBefore(today))
        );
    }

    @FXML
    private void handleUpcomingFilterButton() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysFromNow = today.plusDays(7);
        followUpTable.setItems(
            followUpRows.filtered(row ->
                !row.isCompleted() &&
                !LocalDate.parse(row.getDueDate()).isBefore(today) &&
                !LocalDate.parse(row.getDueDate()).isAfter(sevenDaysFromNow)
            )
        );
    }

    private List<FollowUpRow> filterFollowUps(Predicate<FollowUpRow> predicate) {
        return followUpRows.stream().filter(predicate).collect(Collectors.toList());
    }

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
                Thread.sleep(200); // Reduced sleep for faster feedback
                updateMessage("Sending email...");
                EmailServices.sendEmail(from, password, from, null, subject, body, smtp, port);
                updateMessage("Finishing up...");
                Thread.sleep(100);
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

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
