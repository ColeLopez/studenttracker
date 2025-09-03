package com.cole.controller;

import com.cole.model.SLP;
import com.cole.model.Module;
import com.cole.Service.SLPModuleService;
import com.cole.util.DBUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.sql.*;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentRegistrationController {

    @FXML private TextField studentNumberField;
    @FXML private TextField firstNameField;
    @FXML private TextField secondNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField idField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField branchField;
    @FXML private DatePicker enrollmentDatePicker;
    @FXML private TextField slpField; // read-only, shows selected SLP
    @FXML private ComboBox<String> statusComboBox;

    private final ObservableList<SLP> slps = FXCollections.observableArrayList();
    private SLP selectedSLP = null;

    private static final String[] STATUS_OPTIONS = {"Active", "On Hold", "Graduated"};
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\- ]{7,15}$");
    private static final Logger logger = LoggerFactory.getLogger(StudentRegistrationController.class);

    /**
     * Initializes the StudentRegistrationController.
     * This method loads SLPs and sets up the status ComboBox.
     */
    @FXML
    public void initialize() {
        loadSLPs();
        statusComboBox.setItems(FXCollections.observableArrayList(STATUS_OPTIONS));
        if (slpField != null) {
            slpField.setEditable(false);
            slpField.setFocusTraversable(false);
            slpField.setMouseTransparent(true); //
        }
    }

    /**
     * Loads SLPs from the database and populates the SLP ComboBox.
     * This method retrieves all SLPs and adds them to the ComboBox for selection.
     */
    private void loadSLPs() {
        slps.clear();
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
        } catch (SQLException e) {
            logger.error("Failed to load SLPs", e);
            showError("Failed to load SLPs", e.getMessage());
        }
    }

    /**
     * Opens a dialog to select an SLP.
     * This method shows a searchable list of SLPs for the user to select from.
     */
    @FXML
    private void handleSelectSLP() {
        Dialog<SLP> dialog = new Dialog<>();
        dialog.setTitle("Select SLP");
        dialog.setHeaderText("Search and select an SLP");

        TextField searchField = new TextField();
        searchField.setPromptText("Type to search...");
        ListView<SLP> listView = new ListView<>();
        FilteredList<SLP> filtered = new FilteredList<>(slps, p -> true);
        listView.setItems(filtered);

        listView.setCellFactory(lv -> new ListCell<SLP>() {
            @Override
            protected void updateItem(SLP item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "" : item.getName());
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(slp -> slp != null && slp.getName() != null && slp.getName().toLowerCase().contains(filter));
        });

        VBox content = new VBox(10, searchField, listView);
        content.setPrefSize(400, 400);
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(okButtonType).disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(slp -> {
            selectedSLP = slp;
            slpField.setText(slp.getName());
        });
    }

    /**
     * Handles the save action for student registration.
     * This method validates input fields, checks for duplicates, and saves the student to the database.
     * It also auto-links modules based on the selected SLP.
     */
    @FXML
    private void handleSave() {
        String studentNumber = studentNumberField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String secondName = secondNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String idNumber = idField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String branch = branchField.getText().trim();
        LocalDate enrollmentDate = enrollmentDatePicker.getValue();
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
                        showError("Duplicate Student", "A student with this number already exists.");
                        return;
                    }
                }
            }

            int newStudentId = -1;
            // Insert student and get generated ID
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO students (student_number, first_name, second_name, last_name, id_number, email, phone, branch, enrollment_date, current_slp_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, studentNumber);
                insertStmt.setString(2, firstName);
                insertStmt.setString(3, secondName);
                insertStmt.setString(4, lastName);
                insertStmt.setString(5, idNumber);
                insertStmt.setString(6, email);
                insertStmt.setString(7, phone);
                insertStmt.setString(8, branch);
                insertStmt.setString(9, enrollmentDate.toString());
                insertStmt.setInt(10, selectedSLP.getId());
                insertStmt.setString(11, status);
                insertStmt.executeUpdate();
                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newStudentId = generatedKeys.getInt(1);
                    }
                }
            }

            // Auto-link modules to student based on SLP (one row per module)
            if (newStudentId != -1) {
                SLPModuleService slpModuleService = new SLPModuleService();
                for (Module module : slpModuleService.getModulesForSLP(selectedSLP.getId())) {
                    try (PreparedStatement linkStmt = conn.prepareStatement(
                            "INSERT INTO student_modules (student_id, module_id, module_code, module_name) VALUES (?, ?, ?, ?)")) {
                        linkStmt.setInt(1, newStudentId);
                        linkStmt.setInt(2, module.getId());
                        linkStmt.setString(3, module.getModuleCode());
                        linkStmt.setString(4, module.getName());
                        linkStmt.executeUpdate();
                    }
                }
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
     * Clears the form fields after successful registration.
     * This method resets all input fields to their default state.
     */
    private void clearForm() {
        studentNumberField.clear();
        firstNameField.clear();
        secondNameField.clear();
        lastNameField.clear();
        idField.clear();
        emailField.clear();
        phoneField.clear();
        branchField.clear();
        enrollmentDatePicker.setValue(null);
        slpField.clear();
        selectedSLP = null;
        statusComboBox.getSelectionModel().clearSelection();
    }

    /**
     * Shows an error alert dialog.
     * @param title the title of the alert
     * @param message the content of the alert
     */
    @FXML
    private void handleClear() {
        clearForm();
    }

    /**
     * Shows an error alert dialog.
     * @param title the title of the alert
     * @param message the content of the alert
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
