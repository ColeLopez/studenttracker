package com.cole.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.cole.model.FollowUpRow;
import com.cole.util.DBUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.VBox;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    private String sessionPassword = null; // Add this field

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

        completedColumn.setOnEditCommit(event -> {
            FollowUpRow row = event.getRowValue();
            boolean newValue = event.getNewValue();
            row.setCompleted(newValue);
            updateFollowUpCompleted(row.getFollowUpId(), newValue);
            loadFollowUps();
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

    @FXML
    private void handleCurrentFollowupsButton() {
        LocalDate today = LocalDate.now();
        List<FollowUpRow> current = filterFollowUps(row -> 
            !row.isCompleted() && LocalDate.parse(row.getDueDate()).isEqual(today)
        );
        String emailBody = buildEmailBody("Current Follow-Ups (Due Today):", current);
        sendReminderEmail("Current Follow-Ups", emailBody);
    }

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

    @FXML
    private void handleTodayFilterButton() {
        LocalDate today = LocalDate.now();
        ObservableList<FollowUpRow> todayRows = FXCollections.observableArrayList(
            filterFollowUps(row -> !row.isCompleted() && LocalDate.parse(row.getDueDate()).isEqual(today))
        );
        followUpTable.setItems(todayRows);
    }

    @FXML
    private void handleOverdueFilterButton() {
        LocalDate today = LocalDate.now();
        ObservableList<FollowUpRow> overdueRows = FXCollections.observableArrayList(
            filterFollowUps(row -> !row.isCompleted() && LocalDate.parse(row.getDueDate()).isBefore(today))
        );
        followUpTable.setItems(overdueRows);
    }

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

    private List<FollowUpRow> filterFollowUps(java.util.function.Predicate<FollowUpRow> predicate) {
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
        Properties config = loadEmailSettings();
        if (config == null) return;

        String from = config.getProperty("email.sender", "");
        String smtp = config.getProperty("email.smtp", "");
        String port = config.getProperty("email.port", "");

        if (from.isEmpty() || smtp.isEmpty() || port.isEmpty()) {
            showError("Settings Error", "Email settings are incomplete.");
            return;
        }

        // Only prompt if sessionPassword is not set
        if (sessionPassword == null || sessionPassword.isEmpty()) {
            sessionPassword = promptForPassword();
        }
        if (sessionPassword == null || sessionPassword.isEmpty()) {
            showError("No Password", "Email not sent: password required.");
            return;
        }

        // Create a progress dialog with a status label
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Sending Email...");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(60, 60);

        Label statusLabel = new Label("Starting...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
        statusLabel.setWrapText(true);
        statusLabel.setPrefWidth(260);
        statusLabel.setAlignment(Pos.CENTER); // Center the label's text

        VBox box = new VBox(20, progressIndicator, statusLabel);
        box.setStyle("-fx-padding: 30; -fx-background-color: #f8f8f8;");
        box.setPrefWidth(300);
        box.setAlignment(Pos.CENTER); // Center all children in the VBox

        progressStage.setScene(new javafx.scene.Scene(box));
        progressStage.setResizable(false);

        // Create the background task
        Task<Void> emailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Authenticating...");
                Thread.sleep(500); // Simulate authentication delay
                updateMessage("Sending email...");
                sendEmail(from, sessionPassword, from, subject, body, smtp, port);
                updateMessage("Finishing up...");
                Thread.sleep(300); // Simulate finish delay
                return null;
            }
        };

        // Bind the label to the task's message
        statusLabel.textProperty().bind(emailTask.messageProperty());

        emailTask.setOnSucceeded(e -> {
            progressStage.close();
            showInfo("Reminders Sent", "Email reminders have been sent to you.");
        });
        emailTask.setOnFailed(e -> {
            progressStage.close();
            logger.error("Email Error", emailTask.getException());
            showError("Email Error", emailTask.getException().getMessage());
        });

        // Show the progress dialog and start the task
        progressStage.show();
        new Thread(emailTask).start();
    }

    private Properties loadEmailSettings() {
        Properties config = new Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream("email_settings.properties")) {
            config.load(fis);
            return config;
        } catch (Exception e) {
            logger.error("Settings Error", e);
            showError("Settings Error", "Could not load email settings.");
            return null;
        }
    }

    private void sendEmail(String from, String password, String to, String subject, String body, String smtp, String port) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtp);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    private String promptForPassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Email Password");
        dialog.setHeaderText("Please enter your email password:");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        TextField visibleField = new TextField();
        visibleField.setPromptText("Password");
        visibleField.setManaged(false);
        visibleField.setVisible(false);

        CheckBox showPasswordCheck = new CheckBox("Show password");
        passwordField.textProperty().bindBidirectional(visibleField.textProperty());

        showPasswordCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                visibleField.setText(passwordField.getText());
                dialog.getDialogPane().setContent(new VBox(visibleField, showPasswordCheck));
                visibleField.setManaged(true);
                visibleField.setVisible(true);
                passwordField.setManaged(false);
                passwordField.setVisible(false);
            } else {
                passwordField.setText(visibleField.getText());
                dialog.getDialogPane().setContent(new VBox(passwordField, showPasswordCheck));
                passwordField.setManaged(true);
                passwordField.setVisible(true);
                visibleField.setManaged(false);
                visibleField.setVisible(false);
            }
        });

        dialog.getDialogPane().setContent(new VBox(passwordField, showPasswordCheck));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return showPasswordCheck.isSelected() ? visibleField.getText() : passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
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
