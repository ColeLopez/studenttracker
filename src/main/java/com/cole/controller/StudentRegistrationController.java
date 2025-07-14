package com.cole.controller;

import com.cole.model.SLP;
import com.cole.util.DBUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for student registration form.
 */
public class StudentRegistrationController {

    @FXML private TextField studentNumberField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private DatePicker enrollmentDatePicker;
    @FXML private ComboBox<SLP> slpComboBox;
    @FXML private ComboBox<String> statusComboBox;

    private final ObservableList<SLP> slps = FXCollections.observableArrayList();

    private static final String[] STATUS_OPTIONS = {"Active", "On Hold", "Graduated"};
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\- ]{7,15}$");
    private static final Logger logger = LoggerFactory.getLogger(StudentRegistrationController.class);

    /**
     * Initializes the controller and loads SLPs and status options.
     */
    @FXML
    public void initialize() {
        loadSLPs();
        statusComboBox.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
    }

    /**
     * Loads SLPs from the database and populates the ComboBox.
     */
    private void loadSLPs() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT slp_id, slp_code, name FROM slps")) {
            while (rs.next()) {
                SLP slp = new SLP(
                        rs.getInt("slp_id"),
                        rs.getString("slp_code"),
                        rs.getString("name")
                );
                slps.add(slp);
            }
            slpComboBox.setItems(slps);
        } catch (SQLException e) {
            logger.error("Failed to load SLPs", e);
            showError("Failed to load SLPs", e.getMessage());
        }
    }

    /**
     * Handles the save action for student registration.
     */
    @FXML
    private void handleSave() {
        String studentNumber = studentNumberField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        LocalDate enrollmentDate = enrollmentDatePicker.getValue();
        SLP selectedSLP = slpComboBox.getSelectionModel().getSelectedItem();
        String status = statusComboBox.getSelectionModel().getSelectedItem();

        String validationError = validateFields(studentNumber, firstName, lastName, email, phone, enrollmentDate, selectedSLP, status);
        if (validationError != null) {
            showError("Validation Error", validationError);
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            // Check for duplicates
            try (PreparedStatement checkStmt = conn.prepareStatement("SELECT 1 FROM students WHERE student_number = ?")) {
                checkStmt.setString(1, studentNumber);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        showError("Duplicate Entry", "Student with this number already exists.");
                        return;
                    }
                }
            }

            // Insert
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO students (student_number, first_name, last_name, email, phone, enrollment_date, current_slp_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            )) {
                insertStmt.setString(1, studentNumber);
                insertStmt.setString(2, firstName);
                insertStmt.setString(3, lastName);
                insertStmt.setString(4, email);
                insertStmt.setString(5, phone);
                insertStmt.setString(6, enrollmentDate.toString());
                insertStmt.setInt(7, selectedSLP.getId());
                insertStmt.setString(8, status);

                insertStmt.executeUpdate();
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Student Registered");
            success.setHeaderText(null);
            success.setContentText("Student registered successfully!");
            success.showAndWait();

            clearForm();
        } catch (SQLException e) {
            logger.error("Database Error", e);
            showError("Database Error", e.getMessage());
        }
    }

    /**
     * Validates the input fields for student registration.
     * @return error message if validation fails, otherwise null
     */
    private String validateFields(String studentNumber, String firstName, String lastName, String email, String phone, LocalDate enrollmentDate, SLP selectedSLP, String status) {
        if (studentNumber.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || enrollmentDate == null || selectedSLP == null || status == null) {
            return "Please fill in all required fields.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Invalid email format.";
        }
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            return "Invalid phone number format.";
        }
        return null;
    }

    /**
     * Clears all form fields.
     */
    private void clearForm() {
        studentNumberField.clear();
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        enrollmentDatePicker.setValue(null);
        slpComboBox.getSelectionModel().clearSelection();
        statusComboBox.getSelectionModel().clearSelection();
    }

    /**
     * Handles the clear action for the form.
     */
    @FXML
    private void handleClear() {
        clearForm();
    }

    /**
     * Shows an error alert dialog.
     */
    private void showError(String title, String message) {
        logger.warn(title + ": " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
