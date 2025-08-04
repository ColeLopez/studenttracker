package com.cole.controller;

import com.cole.Service.EmailServices;
import com.cole.model.StudentToGraduate;
import com.cole.util.DBUtil;
import com.cole.util.EmailDialogUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.io.FileInputStream;
import javafx.stage.Stage;
import javafx.concurrent.Task;

public class GraduatesController {
    @FXML
    private Button bukRequestButton;

    @FXML
    private TableColumn<StudentToGraduate, String> numberColumn;
    @FXML
    private TableColumn<StudentToGraduate, String> nameColumn;
    @FXML
    private TableColumn<StudentToGraduate, String> slpColumn;
    @FXML
    private TableColumn<StudentToGraduate, String> emailColumn;
    @FXML
    private TableColumn<StudentToGraduate, String> phoneColumn;

    @FXML
    private TableView<StudentToGraduate> graduationTable;
    @FXML
    private TableColumn<StudentToGraduate, Boolean> transcriptColumn;

    private final ObservableList<StudentToGraduate> graduationList = FXCollections.observableArrayList();

    /**
     * Initializes the GraduatesController.
     * This method sets up the table columns and loads the graduation list from the database.
     */
    @FXML
    private void initialize() {
        // Set up property bindings for all columns
        numberColumn.setCellValueFactory(cellData -> cellData.getValue().studentNumberProperty());
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));
        slpColumn.setCellValueFactory(cellData -> cellData.getValue().slpCourseProperty());
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        phoneColumn.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());

        // Set up the checkbox cell for transcript requested
        transcriptColumn.setCellValueFactory(cellData -> cellData.getValue().transcriptRequestedProperty());
        transcriptColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        graduationTable.setEditable(true);

        transcriptColumn.setOnEditCommit(event -> {
            StudentToGraduate student = event.getRowValue();
            boolean newValue = event.getNewValue();
            student.setTranscriptRequested(newValue);
            updateTranscriptRequestedInDB(student.getStudentNumber(), newValue);
        });

        loadGraduationList();

        graduationTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && graduationTable.getSelectionModel().getSelectedItem() != null) {
                StudentToGraduate selected = graduationTable.getSelectionModel().getSelectedItem();
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Send Transcript Request");
                confirm.setHeaderText("Send transcript request for " + selected.getFullName() + "?");
                confirm.setContentText("Are you sure you want to send a transcript request email for this graduate?");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        sendTranscriptEmail(selected);
                    }
                });
            }
        });

        bukRequestButton.setOnAction(event -> handleBulkTranscriptRequest());
    }

    /**
     * Loads the graduation list from the database.
     * This method retrieves all students who are set to graduate and populates the table view.
     */
    public void loadGraduationList() {
        graduationList.clear();
        String query = "SELECT * FROM students_to_graduate";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StudentToGraduate student = new StudentToGraduate(
                        rs.getString("student_number"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("slp_course"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getInt("transcript_requested") == 1
                );
                // Add listener to update DB when checkbox is toggled
                student.transcriptRequestedProperty().addListener((obs, oldVal, newVal) -> {
                    updateTranscriptRequestedInDB(student.getStudentNumber(), newVal);
                });
                graduationList.add(student);
            }
            graduationTable.setItems(graduationList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the transcript requested status in the database for a specific student.
     * This method is called when the checkbox is toggled in the UI.
     *
     * @param studentNumber the student number of the student
     * @param requested true if the transcript is requested, false otherwise
     */
    private void updateTranscriptRequestedInDB(String studentNumber, boolean requested) {
        String sql = "UPDATE students_to_graduate SET transcript_requested = ? WHERE student_number = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requested ? 1 : 0);
            ps.setString(2, studentNumber);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a transcript request email for the selected student.
     * This method retrieves email settings, prompts for password, and sends the email in a background task.
     *
     * @param student the student for whom the transcript request is being sent
     */
    private void sendTranscriptEmail(StudentToGraduate student) {
        // 1. Get SCAA recipient from settings
        String scaaRecipient = ScaaEmailController.getScaaRecipient();
        if (scaaRecipient == null || scaaRecipient.isEmpty()) {
            showError("No SCAA Email", "Please set the SCAA recipient email in settings.");
            return;
        }

        // 2. Get sender, smtp, port from main email settings
        Properties emailProps = new Properties();
        try (FileInputStream fis = new FileInputStream("email_settings.properties")) {
            emailProps.load(fis);
        } catch (Exception e) {
            showError("Email Settings Error", "Could not load email settings.");
            return;
        }
        String sender = emailProps.getProperty("email.sender", "");
        String smtp = emailProps.getProperty("email.smtp", "");
        String port = emailProps.getProperty("email.port", "");

        if (sender.isEmpty() || smtp.isEmpty() || port.isEmpty()) {
            showError("Email Settings Error", "Email settings are incomplete.");
            return;
        }

        // 3. Prompt for password if not already set
        Stage owner = (Stage) graduationTable.getScene().getWindow();
        String password = EmailDialogUtil.getSessionPassword(owner);
        if (password == null || password.isEmpty()) {
            showError("No Password", "Email not sent: password required.");
            return;
        }

        // 4. Compose email
        String subject = "Transcript Request: " + student.getFullName();
        String body = "Dear SCAA,\n\nPlease process the transcript for:\n\n"
                + "Student: " + student.getFullName() + "\n"
                + "Student Number: " + student.getStudentNumber() + "\n"
                + "SLP Course: " + student.getSlpCourse() + "\n\nThank you.";

        // 5. Show progress dialog and send email in background
        Task<Void> emailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Authenticating...");
                Thread.sleep(500);
                updateMessage("Sending email...");
                EmailServices.sendEmail(sender, password, scaaRecipient, null, subject, body, smtp, port);
                updateMessage("Finishing up...");
                Thread.sleep(300);
                return null;
            }
        };
        emailTask.setOnSucceeded(e -> {
            updateTranscriptRequestedInDB(student.getStudentNumber(), true);
            student.setTranscriptRequested(true);
            loadGraduationList();
            showInfo("Email Sent", "Transcript request sent for " + student.getFullName());
        });
        emailTask.setOnFailed(e -> {
            showError("Email Error", emailTask.getException().getMessage());
        });
        EmailDialogUtil.showEmailProgressDialog(owner, emailTask, "Sending Email...");
    }

    /**
     * Handles the bulk transcript request button click.
     * This method checks if there are students to process and sends bulk emails for those who have not requested transcripts.
     */
    private void handleBulkTranscriptRequest() {
        if (graduationList.isEmpty()) {
            showInfo("No Students", "There are no students to process.");
            return;
        }

        // Filter students who have not had their transcript requested
        ObservableList<StudentToGraduate> toSend = graduationList.filtered(s -> !s.isTranscriptRequested());

        if (toSend.isEmpty()) {
            showInfo("No Pending Requests", "All students have already had their transcript requested.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bulk Transcript Request");
        confirm.setHeaderText("Send transcript requests for " + toSend.size() + " students?");
        confirm.setContentText("Are you sure you want to send transcript request emails for all listed graduates?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendBulkTranscriptEmails(toSend);
            }
        });
    }

    /**
     * Sends bulk transcript request emails for the selected students.
     * This method retrieves email settings, prompts for password, and sends emails in a background task.
     *
     * @param students the list of students to send transcript requests for
     */
    private void sendBulkTranscriptEmails(ObservableList<StudentToGraduate> students) {
        // 1. Get SCAA recipient from settings
        String scaaRecipient = ScaaEmailController.getScaaRecipient();
        if (scaaRecipient == null || scaaRecipient.isEmpty()) {
            showError("No SCAA Email", "Please set the SCAA recipient email in settings.");
            return;
        }

        // 2. Get sender, smtp, port from main email settings
        Properties emailProps = new Properties();
        try (FileInputStream fis = new FileInputStream("email_settings.properties")) {
            emailProps.load(fis);
        } catch (Exception e) {
            showError("Email Settings Error", "Could not load email settings.");
            return;
        }
        String sender = emailProps.getProperty("email.sender", "");
        String smtp = emailProps.getProperty("email.smtp", "");
        String port = emailProps.getProperty("email.port", "");

        if (sender.isEmpty() || smtp.isEmpty() || port.isEmpty()) {
            showError("Email Settings Error", "Email settings are incomplete.");
            return;
        }

        // 3. Prompt for password if not already set
        Stage owner = (Stage) graduationTable.getScene().getWindow();
        String password = EmailDialogUtil.getSessionPassword(owner);
        if (password == null || password.isEmpty()) {
            showError("No Password", "Email not sent: password required.");
            return;
        }

        // 4. Send emails in background with progress dialog
        Task<Void> bulkTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int total = students.size();
                int count = 0;
                for (StudentToGraduate student : students) {
                    count++;
                    updateProgress(count - 1, total);
                    updateMessage("Sending for: " + student.getFullName());

                    String subject = "Transcript Request: " + student.getFullName();
                    String body = "Dear SCAA,\n\nPlease process the transcript for:\n\n"
                            + "Student: " + student.getFullName() + "\n"
                            + "Student Number: " + student.getStudentNumber() + "\n"
                            + "SLP Course: " + student.getSlpCourse() + "\n\nThank you.";

                    try {
                        EmailServices.sendEmail(sender, password, scaaRecipient, null, subject, body, smtp, port);
                        updateTranscriptRequestedInDB(student.getStudentNumber(), true);
                        Platform.runLater(() -> student.setTranscriptRequested(true));
                    } catch (Exception e) {
                        // Optionally, log or collect failed students
                    }
                    Thread.sleep(300); // Optional: avoid spamming server
                }
                updateProgress(total, total);
                updateMessage("All emails sent.");
                return null;
            }
        };

        bulkTask.setOnSucceeded(e -> {
            loadGraduationList();
            showInfo("Bulk Email Sent", "Transcript requests sent for " + students.size() + " students.");
        });
        bulkTask.setOnFailed(e -> {
            showError("Bulk Email Error", "An error occurred while sending bulk emails.");
        });

        EmailDialogUtil.showEmailProgressDialog(owner, bulkTask, "Sending Bulk Emails...");
    }

    /**
     * Displays an error alert with the specified title and message.
     * This method is used to show error messages to the user.
     *
     * @param title the title of the error alert
     * @param message the content of the error message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
}
