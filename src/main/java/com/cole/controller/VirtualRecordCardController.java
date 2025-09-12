package com.cole.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cole.Service.GraduationService;
import com.cole.Service.StudentReportsService;
import com.cole.model.Student;
import com.cole.model.StudentModule;
import com.cole.model.StudentReportData;
import com.cole.util.DBUtil;
import com.cole.model.SLP;
import com.cole.Service.SLPService;
import com.cole.Service.ActivityService;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Virtual Record Card, handling student details, exam results, notes, and follow-ups.
 */
public class VirtualRecordCardController {

    @FXML
    private Button handleStudentReportButton;


    /** Callback to refresh the parent student view after changes (e.g., delete). */
    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    // Note model
    public static class Note {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty text;
        private final String dateAdded;

        public Note(int id, String text, String dateAdded) {
            this.id = id;
            this.text = new javafx.beans.property.SimpleStringProperty(text);
            this.dateAdded = dateAdded;
        }
        public int getId() { return id; }
        public String getText() { return text.get(); }
        public void setText(String t) { text.set(t); }
        public javafx.beans.property.StringProperty textProperty() { return text; }
        public String getDateAdded() { return dateAdded; }
        @Override public String toString() { return getText() + " [" + dateAdded + "]"; }
    }

    // Follow-up model
    public static class FollowUp {
        private final int id;
        private final javafx.beans.property.SimpleStringProperty dueDate;
        private final javafx.beans.property.SimpleStringProperty description;
        private final javafx.beans.property.SimpleBooleanProperty completed;

        public FollowUp(int id, String dueDate, String description, boolean completed) {
            this.id = id;
            this.dueDate = new javafx.beans.property.SimpleStringProperty(dueDate);
            this.description = new javafx.beans.property.SimpleStringProperty(description);
            this.completed = new javafx.beans.property.SimpleBooleanProperty(completed);
        }
        public int getId() { return id; }
        public String getDueDate() { return dueDate.get(); }
        public void setDueDate(String d) { dueDate.set(d); }
        public String getDescription() { return description.get(); }
        public void setDescription(String d) { description.set(d); }
        public boolean isCompleted() { return completed.get(); }
        public void setCompleted(boolean c) { completed.set(c); }
        public javafx.beans.property.StringProperty dueDateProperty() { return dueDate; }
        public javafx.beans.property.StringProperty descriptionProperty() { return description; }
        public javafx.beans.property.BooleanProperty completedProperty() { return completed; }
    }

    // EditableDoubleCell for exam results
    public static class EditableDoubleCell extends javafx.scene.control.TableCell<StudentModule, Double> {
        private final javafx.scene.control.TextField textField = new javafx.scene.control.TextField();
        private final String examType;
        private final VirtualRecordCardController controller;

        /**
         * Constructor for EditableDoubleCell.
         * @param examType The type of exam (e.g., "formative", "summative", "supplementary").
         * @param controller The controller instance to handle saving results.
         */
        public EditableDoubleCell(String examType, VirtualRecordCardController controller) {
            this.examType = examType;
            this.controller = controller;
            textField.setOnAction(e -> commitEdit(parseDouble(textField.getText())));
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) commitEdit(parseDouble(textField.getText()));
            });
        }

        /**
         * Starts editing the cell, setting up the text field and checking edit permissions.
         */
        @Override
        public void startEdit() {
            StudentModule sm = getStudentModule();
            int passRate = (sm != null) ? sm.getPassRate() : 50;
            boolean canEdit = canEditCell(sm, passRate);
            if (!canEdit) return;
            super.startEdit();
            textField.setText(getItem() == null ? "" : getItem().toString());
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            textField.requestFocus();
        }

        /**
         * Cancels the edit, reverting to the original text.
         */
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() == null ? "" : getItem().toString());
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        /**
         * Updates the item in the cell, applying color coding based on exam type and pass rate.
         * @param item The new item to display in the cell.
         * @param empty Whether the cell is empty.
         */
        @Override
        public void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                StudentModule sm = getStudentModule();
                int passRate = (sm != null) ? sm.getPassRate() : 50;
                boolean canEdit = canEditCell(sm, passRate);
                if (isEditing()) {
                    textField.setText(item == null ? "" : item.toString());
                    setGraphic(textField);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    setText(item == null ? "" : item.toString());
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                }
                // Color code
                if (item != null && item >= passRate) {
                    setStyle("-fx-background-color: #c8e6c9;");
                } else if (item != null) {
                    setStyle("-fx-background-color: #ffcdd2;");
                } else {
                    setStyle("");
                }
                // Disable cell visually if not editable
                if (!canEdit && ("summative".equals(examType) || "supplementary".equals(examType))) {
                    setStyle(getStyle() + ";-fx-opacity: 0.5;");
                }
            }
        }

        /**
         * Commits the edit with validation and saves the result.
         * @param newValue The new value to commit.
         */
        @Override
        public void commitEdit(Double newValue) {
            StudentModule sm = getStudentModule();
            int passRate = (sm != null) ? sm.getPassRate() : 50;
            boolean canEdit = canEditCell(sm, passRate);
            if (!canEdit) {
                cancelEdit();
                return;
            }
            super.commitEdit(newValue);
            if (sm == null) return;
            controller.saveExamResult(sm, examType, newValue);
            switch (examType) {
                case "formative": sm.setFormative(newValue); break;
                case "summative": sm.setSummative(newValue); break;
                case "supplementary": sm.setSupplementary(newValue); break;
            }
        }

        private Double parseDouble(String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return null; }
        }

        private StudentModule getStudentModule() {
            return getTableRow() != null ? (StudentModule) getTableRow().getItem() : null;
        }

        private boolean canEditCell(StudentModule sm, int passRate) {
            if (sm == null) return false;
            switch (examType) {
                case "summative":
                    return sm.getFormative() >= passRate;
                case "supplementary":
                    return sm.getFormative() >= passRate && sm.getSummative() < passRate;
                default:
                    return true;
            }
        }

        public static javafx.util.Callback<TableColumn<StudentModule, Double>, TableCell<StudentModule, Double>> forExamType(String examType, VirtualRecordCardController controller) {
            return col -> new EditableDoubleCell(examType, controller);
        }
    }

    // FXML fields for module table and follow-up table
    @FXML private TableView<StudentModule> moduleTable;
    @FXML private TableColumn<StudentModule, String> moduleCodeColumn;
    @FXML private TableColumn<StudentModule, Double> formativeColumn;
    @FXML private TableColumn<StudentModule, Double> summativeColumn;
    @FXML private TableColumn<StudentModule, Double> supplementaryColumn;
    @FXML private TableColumn<StudentModule, String> moduleNameColumn;
    @FXML private TableColumn<StudentModule, String> examTypeColumn;
    @FXML private TableColumn<StudentModule, Boolean> bookIssuedColumn;
    @FXML private TableColumn<StudentModule, String> signatureColumn; // New column for signatures
    @FXML private TableColumn<StudentModule, String> dateIssuedColumn; // New column for date issued

    @FXML private TableView<FollowUp> followUpTable;
    @FXML private TableColumn<FollowUp, String> dueDateColumn;
    @FXML private TableColumn<FollowUp, String> descriptionColumn;
    @FXML private TableColumn<FollowUp, Boolean> completedColumn;

    // FXML fields for buttons and labels
    @FXML private Button handleAddFollowUpButton;
    @FXML private Button handleEditStudentButton;
    @FXML private Button handleDeleteStudentButton;
    @FXML private Button handleCloseButton;
    @FXML private Button handleReregisterButton; // Add this in your FXML

    /**
     * Handles closing the Virtual Record Card window.
     */
    @FXML
    private void handleClose() {
        // Try to close the window containing the close button
        if (handleCloseButton != null) {
            javafx.scene.Scene scene = handleCloseButton.getScene();
            if (scene != null && scene.getWindow() != null) {
                scene.getWindow().hide();
            }
        }
    }

    /**
     * Handles deleting the selected student from the database and closes the window.
     */
    @FXML
    private void handleDeleteStudent() {
        if (selectedStudent == null) {
            showError("No student selected", "Please select a student to delete.");
            return;
        }
        // Confirm deletion
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Student");
        confirm.setHeaderText("Are you sure you want to delete this student?");
        confirm.setContentText("This action cannot be undone.");
        java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
            return;
        }

        String oldStudentNumber = selectedStudent.getStudentNumber();

        // Delete all related data for the student, then the student record itself
        String[] sqls = new String[] {
            "DELETE FROM student_modules WHERE student_id = ?",
            "DELETE FROM notes WHERE student_id = ?",
            "DELETE FROM follow_ups WHERE student_id = ?",
            // Add more related tables here if needed
            "DELETE FROM students WHERE student_id = ?"
        };
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            for (String sql : sqls) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, selectedStudent.getId());
                    stmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Error deleting student and related data", e);
            showError("Error deleting student", e.getMessage());
            return;
        }

        // Log the deletion activity
        int userId = com.cole.util.UserSession.getInstance().getUserId();
        String studentNum = selectedStudent.getStudentNumber();
        String firstName = selectedStudent.getFirstName();
        String lastName = selectedStudent.getLastName();
        String slp = selectedStudent.getSlp();
        String email = selectedStudent.getEmail();
        String idNumber = selectedStudent.getIdNumber();
        String phone = selectedStudent.getPhoneNumber();
        String branch = selectedStudent.getBranch();
        String status = selectedStudent.getStatus();

        ActivityService.logActivity(
            userId,
            "STUDENT_DELETED",
            "Deleted student " + studentNum + " (" + firstName + " " + lastName + "), SLP: " + slp +
            ", Email: " + email + ", ID: " + idNumber + ", Phone: " + phone + ", Branch: " + branch + ", Status: " + status
        );

        // Close the window
        handleClose();
        // Notify parent to refresh student list
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }

    /**
     * Shows an error dialog with the given title and message.
     * @param title The title of the error dialog.
     * @param message The error message to display.
     */
    @FXML
private void handleStudentReport() {
    if (selectedStudent == null) {
        showError("No student selected", "Please select a student to export.");
        return;
    }

    // Prompt user for file location
    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
    fileChooser.setTitle("Export Student Report");
    fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
    fileChooser.setInitialFileName(selectedStudent.getStudentNumber() + "_record_card.pdf");
    java.io.File file = fileChooser.showSaveDialog(null);
    if (file == null) return;

    // Gather all data for the report using your service
    StudentReportsService reportsService = new StudentReportsService();
    StudentReportData reportData = reportsService.getStudentReportData(selectedStudent.getStudentNumber());

    if (reportData == null) {
        showError("Export Failed", "Could not gather student data for export.");
        return;
    }

    try {
        reportsService.exportStudentSummaryPdf(reportData, file);
    } catch (Exception e) {
        showError("Export Failed", e.getMessage());
    }
}
    
    /**
     * Shows an error dialog with the given title and message.
     * @param title The title of the error dialog.
     * @param message The error message to display.
     */
    @FXML
    private List<Object> handleEditStudent() {
        if (selectedStudent == null) {
            showError("No student selected", "Please select a student to edit.");
            return null;
        }
        // Create dialog fields pre-filled with current values
        javafx.scene.control.TextField firstNameField = new javafx.scene.control.TextField(selectedStudent.getFirstName());
        javafx.scene.control.TextField secondNameField = new javafx.scene.control.TextField(selectedStudent.getSecondName());   
        javafx.scene.control.TextField lastNameField = new javafx.scene.control.TextField(selectedStudent.getLastName());
        javafx.scene.control.TextField idNumberField = new javafx.scene.control.TextField(selectedStudent.getIdNumber());
        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField(selectedStudent.getEmail());
        javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField(selectedStudent.getPhoneNumber());
        javafx.scene.control.TextField branchField = new javafx.scene.control.TextField(selectedStudent.getBranch());
        // ComboBox for SLP
        javafx.scene.control.ComboBox<SLP> slpCombo = new javafx.scene.control.ComboBox<>();
        SLPService slpService = new SLPService();
        javafx.collections.ObservableList<SLP> slpOptions = FXCollections.observableArrayList(slpService.getAllSLPs());
        slpCombo.setItems(slpOptions);

        // Select the student's current SLP
        SLP currentSlpObj = null;
        for (SLP slp : slpOptions) {
            if (slp.getName().equals(selectedStudent.getSlp())) {
                currentSlpObj = slp;
                break;
            }
        }
        if (currentSlpObj != null) {
            slpCombo.getSelectionModel().select(currentSlpObj);
        } else if (!slpOptions.isEmpty()) {
            slpCombo.getSelectionModel().select(0);
        }

        // ComboBox for Status
        javafx.scene.control.ComboBox<String> statusCombo = new javafx.scene.control.ComboBox<>();
        javafx.collections.ObservableList<String> statusOptions = FXCollections.observableArrayList("Active", "On Hold", "Graduated");
        statusCombo.setItems(statusOptions);
        statusCombo.getSelectionModel().select(selectedStudent.getStatus());

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new javafx.scene.control.Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Second Name:"), 0, 1);
        grid.add(secondNameField, 1, 1);
        grid.add(new javafx.scene.control.Label("Last Name:"), 0, 2);
        grid.add(lastNameField, 1, 2);
        grid.add(new javafx.scene.control.Label("ID Number:"), 0, 3);
        grid.add(idNumberField, 1, 3);
        grid.add(new javafx.scene.control.Label("Email:"), 0, 4);
        grid.add(emailField, 1, 4);
        grid.add(new javafx.scene.control.Label("Phone:"), 0, 5);
        grid.add(phoneField, 1, 5);
        grid.add(new javafx.scene.control.Label("SLP:"), 0, 6);
        grid.add(slpCombo, 1, 6);
        grid.add(new javafx.scene.control.Label("Branch:"), 0, 7);
        grid.add(branchField, 1, 7);
        grid.add(new javafx.scene.control.Label("Status:"), 0, 8);
        grid.add(statusCombo, 1, 8);

        javafx.scene.control.Dialog<java.util.List<Object>> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Edit Student");
        dialog.setHeaderText("Edit student details");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == javafx.scene.control.ButtonType.OK) {
                return java.util.Arrays.asList(
                    firstNameField.getText(),
                    secondNameField.getText(),
                    lastNameField.getText(),
                    idNumberField.getText(),
                    emailField.getText(),
                    phoneField.getText(),
                    slpCombo.getSelectionModel().getSelectedItem(), // SLP object
                    branchField.getText(),
                    statusCombo.getSelectionModel().getSelectedItem()
                );
            }
            return null;
        });
        java.util.Optional<java.util.List<Object>> result = dialog.showAndWait();
        if (result.isPresent()) {
            java.util.List<Object> values = result.get();
            SLP newSlp = (SLP) values.get(6); // SLP object
            int newSlpId = newSlp.getId();
            String newSlpName = newSlp.getName();
            String oldSlp = selectedStudent.getSlp();

            // Store old values before update (at the start of handleEditStudent)
            String oldFirstName = selectedStudent.getFirstName();
            String oldSecondName = selectedStudent.getSecondName();
            String oldLastName = selectedStudent.getLastName();
            String oldIdNumber = selectedStudent.getIdNumber();
            String oldEmail = selectedStudent.getEmail();
            String oldPhone = selectedStudent.getPhoneNumber();
            String oldBranch = selectedStudent.getBranch();
            String oldStatus = selectedStudent.getStatus();

            try (Connection conn = DBUtil.getConnection()) {
                String sql = "UPDATE students SET first_name = ?, second_name = ?, last_name = ?, id_number = ?, email = ?, phone = ?, branch = ?, current_slp_id = ?, status = ? WHERE student_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (String) values.get(0)); // first_name
                    stmt.setString(2, (String) values.get(1)); // second_name
                    stmt.setString(3, (String) values.get(2)); // last_name
                    stmt.setString(4, (String) values.get(3)); // id_number
                    stmt.setString(5, (String) values.get(4)); // email
                    stmt.setString(6, (String) values.get(5)); // phone
                    stmt.setString(7, (String) values.get(7)); // branch
                    stmt.setInt(8, newSlpId);                  // current_slp_id
                    stmt.setString(9, (String) values.get(8)); // status
                    stmt.setInt(10, selectedStudent.getId());  // student_id
                    stmt.executeUpdate();
                }

                // Update local object using property setters
                selectedStudent.firstNameProperty().set((String) values.get(0));
                selectedStudent.secondNameProperty().set((String) values.get(1));
                selectedStudent.lastNameProperty().set((String) values.get(2));
                selectedStudent.idNumberProperty().set((String) values.get(3));
                selectedStudent.emailProperty().set((String) values.get(4));
                selectedStudent.phoneNumberProperty().set((String) values.get(5));
                selectedStudent.slpProperty().set(newSlpName);
                selectedStudent.branchProperty().set((String) values.get(7));
                selectedStudent.statusProperty().set((String) values.get(8));
                loadStudentDetails();

                // If SLP changed, unlink old modules and link new ones, and add automated note
                if (!oldSlp.equals(newSlpName)) {
                    // 1. Unlink all modules for this student
                    String unlinkSql = "DELETE FROM student_modules WHERE student_id = ?";
                    try (PreparedStatement unlinkStmt = conn.prepareStatement(unlinkSql)) {
                        unlinkStmt.setInt(1, selectedStudent.getId());
                        unlinkStmt.executeUpdate();
                    }
                    // 2. Link new modules for the new SLP
                    String getModulesSql = "SELECT m.module_id, m.module_code, m.name FROM modules m JOIN slp_modules sm ON m.module_id = sm.module_id WHERE sm.slp_id = ?";
                    try (PreparedStatement getModulesStmt = conn.prepareStatement(getModulesSql)) {
                        getModulesStmt.setInt(1, newSlpId);
                        try (ResultSet rs = getModulesStmt.executeQuery()) {
                            while (rs.next()) {
                                int moduleId = rs.getInt("module_id");
                                String moduleCode = rs.getString("module_code");
                                String moduleName = rs.getString("name");
                                String insertModuleSql = "INSERT INTO student_modules (student_id, module_id, module_code, module_name, formative, summative, supplementary, received_book) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                                try (PreparedStatement insertModuleStmt = conn.prepareStatement(insertModuleSql)) {
                                    insertModuleStmt.setInt(1, selectedStudent.getId());
                                    insertModuleStmt.setInt(2, moduleId);
                                    insertModuleStmt.setString(3, moduleCode);
                                    insertModuleStmt.setString(4, moduleName);
                                    insertModuleStmt.setInt(5, 0); // formative
                                    insertModuleStmt.setInt(6, 0); // summative
                                    insertModuleStmt.setInt(7, 0); // supplementary
                                    insertModuleStmt.setInt(8, 0); // received_book
                                    insertModuleStmt.executeUpdate();
                                }
                            }
                        }
                    }
                    // 3. Add automated note
                    String noteSql = "INSERT INTO notes (student_id, note_text, date_added) VALUES (?, ?, date('now'))";
                    try (PreparedStatement noteStmt = conn.prepareStatement(noteSql)) {
                        noteStmt.setInt(1, selectedStudent.getId());
                        noteStmt.setString(2, "SLP changed from '" + oldSlp + "' to '" + newSlpName + "'. Modules relinked.");
                        noteStmt.executeUpdate();
                    }
                    // 4. Reload modules and notes
                    loadStudentModules();
                    loadNotes();
                }
            } catch (SQLException e) {
                logger.error("Error updating student", e);
                showError("Error updating student", e.getMessage());
            }

            // Log the activity for student edit
            int userId = com.cole.util.UserSession.getInstance().getUserId();
            String studentNum = selectedStudent.getStudentNumber();

            String newFirstName = (String) values.get(0);
            String newSecondName = (String) values.get(1);
            String newLastName = (String) values.get(2);
            String newIdNumber = (String) values.get(3);
            String newEmail = (String) values.get(4);
            String newPhone = (String) values.get(5);
            String newBranch = (String) values.get(7);
            String newStatus = (String) values.get(8);
            // String newSlpName = newSlp.getName(); // Removed duplicate declaration

            if (!oldFirstName.equals(newFirstName)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed first name for student " + studentNum + " from '" + oldFirstName + "' to '" + newFirstName + "'");
            }
            if (!oldSecondName.equals(newSecondName)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed second name for student " + studentNum + " from '" + oldSecondName + "' to '" + newSecondName + "'");
            }
            if (!oldLastName.equals(newLastName)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed last name for student " + studentNum + " from '" + oldLastName + "' to '" + newLastName + "'");
            }
            if (!oldIdNumber.equals(newIdNumber)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed ID number for student " + studentNum + " from '" + oldIdNumber + "' to '" + newIdNumber + "'");
            }
            if (!oldEmail.equals(newEmail)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed email for student " + studentNum + " from '" + oldEmail + "' to '" + newEmail + "'");
            }
            if (!oldPhone.equals(newPhone)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed phone for student " + studentNum + " from '" + oldPhone + "' to '" + newPhone + "'");
            }
            if (!oldBranch.equals(newBranch)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed branch for student " + studentNum + " from '" + oldBranch + "' to '" + newBranch + "'");
            }
            if (!oldStatus.equals(newStatus)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed status for student " + studentNum + " from '" + oldStatus + "' to '" + newStatus + "'");
            }
            if (!oldSlp.equals(newSlpName)) {
                ActivityService.logActivity(userId, "STUDENT_EDITED", "Changed SLP for student " + studentNum + " from '" + oldSlp + "' to '" + newSlpName + "'");
            }

            // Always check graduation flags after editing student
            GraduationService graduationService = new GraduationService();
            graduationService.checkAndUpdateGraduationFlags();

            // Refresh parent view if callback is set
            if (refreshCallback != null) {
                refreshCallback.run();
            }
        }
        return null;
    }

    /**
     * Handles reregistration for a student: removes (or marks) the old module and adds the new one with a reregistration tag.
     * @param oldModule The StudentModule to be replaced.
     * @param newModuleId The module_id of the new module to register.
     */
    public void handleReregistration(StudentModule oldModule, int newModuleId) {
        if (selectedStudent == null || oldModule == null) {
            showError("No student/module selected", "Please select a student and module to reregister.");
            return;
        }
        // Mark old module as replaced and clear signature
        String markOldSql = "UPDATE student_modules SET status = 'replaced', signature_path = NULL, received_book = 0 WHERE student_id = ? AND module_id = ?";
        String fetchModuleSql = "SELECT module_code, name FROM modules WHERE module_id = ?";
        String addNewSql = "INSERT INTO student_modules (student_id, module_id, module_code, module_name, formative, summative, supplementary, received_book) VALUES (?, ?, ?, ?, 0, 0, 0, 0)";
        String addNoteSql = "INSERT INTO notes (student_id, note_text, date_added) VALUES (?, ?, date('now'))";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (
                PreparedStatement markStmt = conn.prepareStatement(markOldSql);
                PreparedStatement fetchStmt = conn.prepareStatement(fetchModuleSql);
                PreparedStatement addStmt = conn.prepareStatement(addNewSql);
                PreparedStatement noteStmt = conn.prepareStatement(addNoteSql)
            ) {
                // 1. Mark old module as replaced and clear signature
                markStmt.setInt(1, selectedStudent.getId());
                markStmt.setInt(2, oldModule.getModuleId());
                markStmt.executeUpdate();

                // 2. Fetch module_code and name for the new module
                fetchStmt.setInt(1, newModuleId);
                String moduleCode = null;
                String moduleName = null;
                try (ResultSet rs = fetchStmt.executeQuery()) {
                    if (rs.next()) {
                        moduleCode = rs.getString("module_code");
                        moduleName = rs.getString("name");
                    }
                }
                if (moduleCode == null || moduleName == null) {
                    conn.rollback();
                    showError("Reregistration Error", "Module details not found.");
                    return;
                }

                // 3. Add new module as reregistration
                addStmt.setInt(1, selectedStudent.getId());
                addStmt.setInt(2, newModuleId);
                addStmt.setString(3, moduleCode);
                addStmt.setString(4, moduleName);
                addStmt.executeUpdate();

                // 4. Add automated note
                String noteText = "Module reregistered: replaced '" + oldModule.getModuleCode() + " - "
                        + oldModule.getModuleName() + "' with '" + moduleCode + " - " + moduleName + "'.";
                noteStmt.setInt(1, selectedStudent.getId());
                noteStmt.setString(2, noteText);
                noteStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error during reregistration", e);
                showError("Reregistration Error", e.getMessage());
                return;
            }
        } catch (SQLException e) {
            logger.error("Error during reregistration (connection)", e);
            showError("Reregistration Error", e.getMessage());
            return;
        }
        // Reload modules and notes to update UI
        loadStudentModules();
        loadNotes();
    }

    /**
     * FXML handler for reregistration button. You should wire this up in your FXML.
     * This is a stub: you need to implement module selection dialog.
     */
    @FXML
    private void handleReregistration() {
        // Get selected module from table
        StudentModule oldModule = moduleTable.getSelectionModel().getSelectedItem();
        if (oldModule == null) {
            showError("No module selected", "Please select a module to reregister.");
            return;
        }
        // Only show modules for the student's SLP, with the same module code as the failed module, not already registered
        ObservableList<ModuleOption> availableModules = FXCollections.observableArrayList();
        String sql = "SELECT m.module_id, m.module_code, m.name FROM modules m " +
                "JOIN slp_modules sm ON m.module_id = sm.module_id " +
                "JOIN slps s ON sm.slp_id = s.slp_id " +
                "JOIN students st ON st.student_id = ? " +
                "WHERE sm.slp_id = st.current_slp_id AND m.module_code = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedStudent.getId());
            stmt.setString(2, oldModule.getModuleCode());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                availableModules.add(new ModuleOption(
                    rs.getInt("module_id"),
                    rs.getString("module_code"),
                    rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error loading available modules for reregistration", e);
            showError("Error", "Could not load available modules.");
            return;
        }
        if (availableModules.isEmpty()) {
            showError("No Modules", "No available modules for reregistration for this SLP and module code.");
            return;
        }
        // Show ChoiceDialog for module selection
        javafx.scene.control.ChoiceDialog<ModuleOption> dialog = new javafx.scene.control.ChoiceDialog<>(availableModules.get(0), availableModules);
        dialog.setTitle("Select Module for Reregistration");
        dialog.setHeaderText("Select a module to reregister for this student.");
        dialog.setContentText("Module:");
        java.util.Optional<ModuleOption> result = dialog.showAndWait();
        if (result.isPresent()) {
            int newModuleId = result.get().getModuleId();
            handleReregistration(oldModule, newModuleId);
        }
    }

    /**
     * Represents a module option for selection dialogs.
     */
    private static class ModuleOption {
        private final int moduleId;
        private final String moduleCode;
        private final String moduleName;
        public ModuleOption(int moduleId, String moduleCode, String moduleName) {
            this.moduleId = moduleId;
            this.moduleCode = moduleCode;
            this.moduleName = moduleName;
        }
        public int getModuleId() { return moduleId; }
        @Override
        public String toString() {
            return moduleCode + " - " + moduleName;
        }
    }

    @FXML private ListView<Note> noteList;
    @FXML private TextField noteInputField;
    @FXML private DatePicker followUpDueDateField;
    @FXML private TextField followUpDescField;

    @FXML private TextField studentNameField;
    @FXML private TextField studentNumberField;
    @FXML private TextField studentSlpField;
    @FXML private TextField studentStatusField;
    @FXML private TextField studentEmailField;
    @FXML private TextField studentIDNumberField;
    @FXML private TextField studentPhoneField;
    @FXML private TextField studentBranchField;

    private static final Logger logger = LoggerFactory.getLogger(VirtualRecordCardController.class);

    private Student selectedStudent;
    private final ObservableList<StudentModule> studentModules = FXCollections.observableArrayList();
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final ObservableList<FollowUp> followUps = FXCollections.observableArrayList();

    /**
     * Sets the selected student and loads their details and modules.
     * @param student The student to display.
     */
    public void setStudent(Student student) {
        this.selectedStudent = student;
        syncStudentModulesWithSLP();
        loadStudentDetails();
        loadStudentModules();
        loadNotes();
        loadFollowUps();
    }
    /**
     * Loads the follow-ups for the selected student from the database.
     * Populates the followUps ObservableList.
     */
    private void loadFollowUps() {
        followUps.clear();
        if (selectedStudent == null) return;
        String sql = "SELECT followup_id, due_date, description, completed FROM follow_ups WHERE student_id = ? ORDER BY due_date DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedStudent.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FollowUp fu = new FollowUp(
                    rs.getInt("followup_id"),
                    rs.getString("due_date"),
                    rs.getString("description"),
                    rs.getInt("completed") == 1
                );
                // Add listener to update DB when completed checkbox is clicked
                fu.completedProperty().addListener((obs, oldVal, newVal) -> updateFollowUpInDB(fu));
                followUps.add(fu);
            }
        } catch (SQLException e) {
            logger.error("Error loading follow-ups", e);
        }
    }


    /**
     * Sets the selected student and loads their details and modules.
     * @param student The student to display.
     */
    // (All duplicate methods removed. Only one implementation of each method remains.)

    // Load notes from DB
    private void loadNotes() {
        notes.clear();
        if (selectedStudent == null) return;
        String sql = "SELECT note_id, note_text, date_added FROM notes WHERE student_id = ? ORDER BY date_added DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedStudent.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(new Note(rs.getInt("note_id"), rs.getString("note_text"), rs.getString("date_added")));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading notes", e);
        }
    }

    /**
     * Adds a new note for the selected student from the UI input field.
     */
    @FXML
    private void handleAddNote() {
        if (selectedStudent == null || noteInputField == null) return;
        String noteText = noteInputField.getText();
        if (noteText == null || noteText.trim().isEmpty()) return;
        String sql = "INSERT INTO notes (student_id, note_text, date_added) VALUES (?, ?, date('now'))";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, selectedStudent.getId());
            stmt.setString(2, noteText.trim());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int noteId = keys.getInt(1);
                    notes.add(0, new Note(noteId, noteText.trim(), java.time.LocalDate.now().toString()));
                }
            }
        } catch (SQLException e) {
            logger.error("Error adding note", e);
            showError("Error adding note", e.getMessage());
        } finally {
            noteInputField.clear();
        }
    }

    // Add follow-up from UI using in-place fields
    @FXML
    private void handleAddFollowUp() {
        if (selectedStudent == null || followUpDueDateField == null || followUpDescField == null) return;
        LocalDate dueDateValue = followUpDueDateField.getValue();
        String desc = followUpDescField.getText();
        if (dueDateValue == null || desc == null || desc.trim().isEmpty()) return;
        String dueDate = dueDateValue.toString();
        String sql = "INSERT INTO follow_ups (student_id, due_date, description, completed) VALUES (?, ?, ?, 0)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, selectedStudent.getId());
            stmt.setString(2, dueDate);
            stmt.setString(3, desc);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    followUps.add(new FollowUp(keys.getInt(1), dueDate, desc, false));
                }
            }
        } catch (SQLException e) {
            logger.error("Error adding follow-up", e);
        } finally {
            followUpDueDateField.setValue(null);
            followUpDescField.clear();
        }
    }

    // Update note in DB
    private void updateNoteInDB(Note note) {
        if (note == null) return;
        String sql = "UPDATE notes SET note_text = ? WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, note.getText());
            stmt.setInt(2, note.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating note", e);
        }
    }

    // Update follow-up in DB
    private void updateFollowUpInDB(FollowUp fu) {
        if (fu == null) return;
        String sql = "UPDATE follow_ups SET due_date = ?, description = ?, completed = ? WHERE followup_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fu.getDueDate());
            stmt.setString(2, fu.getDescription());
            stmt.setInt(3, fu.isCompleted() ? 1 : 0);
            stmt.setInt(4, fu.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating follow-up", e);
        }
    }

    /**
     * Loads the selected student's details into the UI.
     */
    private void loadStudentDetails() {
        if (selectedStudent == null) {
            showError("No student selected", "Please select a student.");
            return;
        }
        if (studentNameField != null) studentNameField.setText(selectedStudent.getFirstName() + " " + selectedStudent.getSecondName() + " " + selectedStudent.getLastName());
        if (studentNumberField != null) studentNumberField.setText(selectedStudent.getStudentNumber());
        if (studentSlpField != null) studentSlpField.setText(selectedStudent.getSlp());
        if (studentStatusField != null) studentStatusField.setText(selectedStudent.getStatus());
        if (studentEmailField != null) studentEmailField.setText(selectedStudent.getEmail());
        if (studentIDNumberField != null) studentIDNumberField.setText(selectedStudent.getIdNumber());
        if (studentPhoneField != null) studentPhoneField.setText(selectedStudent.getPhoneNumber());
        if (studentBranchField != null) studentBranchField.setText(selectedStudent.getBranch());
    }

    /**
     * Loads the modules for the selected student from the database.
     * Populates the studentModules ObservableList.
     */
    private void loadStudentModules() {
        studentModules.clear();
        if (selectedStudent == null) return;
        logger.info("Loading modules for student_id: {}", selectedStudent.getId());
        String sql = "SELECT sm.*, m.pass_rate FROM student_modules sm JOIN modules m ON sm.module_id = m.module_id WHERE sm.student_id = ? AND (sm.status IS NULL OR sm.status != 'replaced')";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedStudent.getId());
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                StudentModule sm = new StudentModule(
                    rs.getInt("student_id"),
                    rs.getInt("module_id"),
                    rs.getString("module_code"),
                    rs.getString("module_name"),
                    rs.getObject("formative") != null ? rs.getDouble("formative") : 0.0,
                    rs.getObject("summative") != null ? rs.getDouble("summative") : 0.0,
                    rs.getObject("supplementary") != null ? rs.getDouble("supplementary") : 0.0,
                    rs.getBoolean("received_book"),
                    rs.getString("signature_path"),
                    rs.getString("date_issued")
                );
                // Set passRate if setter exists
                try {
                    int passRate = rs.getObject("pass_rate") != null ? rs.getInt("pass_rate") : 50;
                    java.lang.reflect.Method setPassRate = sm.getClass().getMethod("setPassRate", int.class);
                    setPassRate.invoke(sm, passRate);
                } catch (Exception ignore) {}
                studentModules.add(sm);
                count++;
            }
            logger.info("Loaded {} modules for student_id {}", count, selectedStudent.getId());
        } catch (SQLException e) {
            logger.error("Error loading student modules", e);
        }
        if (studentModules.isEmpty()) {
            showError("No Modules Found", "This student is not registered for any modules.");
        }

        // --- TableView Column Setup ---
        if (moduleCodeColumn != null) {
            moduleCodeColumn.setCellValueFactory(new PropertyValueFactory<>("moduleCode"));
        }
        if (moduleNameColumn != null) {
            moduleNameColumn.setCellValueFactory(new PropertyValueFactory<>("moduleName"));
        }
        // Removed setCellValueFactory for exam columns; now set in initialize()
        if (bookIssuedColumn != null) {
            bookIssuedColumn.setCellValueFactory(cellData -> {
                StudentModule sm = cellData.getValue();
                SimpleBooleanProperty property = new SimpleBooleanProperty(sm.isReceivedBook());
                property.addListener((observable, oldValue, newValue) -> {
                    sm.setReceivedBook(newValue);
                    updateBookIssuedInDB(sm.getStudentId(), sm.getModuleId(), newValue);
                });
                return property;
            });
            bookIssuedColumn.setCellFactory(tc -> {
                CheckBoxTableCell<StudentModule, Boolean> cell = new CheckBoxTableCell<>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            });
        }
        if (moduleTable != null) {
            moduleTable.setItems(studentModules);
            System.out.println("[VirtualRecordCardController] moduleTable items set, row count: " + studentModules.size());
            // Force refresh to ensure custom cell rendering (color coding) is applied
            moduleTable.refresh();
            System.out.println("[VirtualRecordCardController] moduleTable refreshed, row count: " + moduleTable.getItems().size());
        }

        // Notes ListView setup
        if (noteList != null) {
            noteList.setItems(notes);
            noteList.setEditable(true);
            noteList.setCellFactory(lv -> new javafx.scene.control.cell.TextFieldListCell<>(new javafx.util.StringConverter<Note>() {
                @Override
                public String toString(Note note) {
                    if (note == null) return "";
                    return note.getText() + " [" + note.getDateAdded() + "]";
                }
                @Override
                public Note fromString(String string) {
                    int index = noteList.getEditingIndex();
                    if (index >= 0 && index < notes.size()) {
                        Note note = notes.get(index);
                        // Only update the text, not the date
                        String newText = string.replaceAll("\\s*\\[.*$", "").trim();
                        note.setText(newText);
                        updateNoteInDB(note);
                        return note;
                    }
                    return null;
                }
            }));
        }

        // Follow-up Table setup
        if (followUpTable != null) {
            followUpTable.setItems(followUps);
            followUpTable.setEditable(true);
            if (dueDateColumn != null) {
                dueDateColumn.setCellValueFactory(cellData -> cellData.getValue().dueDateProperty());
                dueDateColumn.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
                dueDateColumn.setOnEditCommit(event -> {
                    FollowUp fu = event.getRowValue();
                    fu.setDueDate(event.getNewValue());
                    updateFollowUpInDB(fu);
                });
            }
            if (descriptionColumn != null) {
                descriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
                descriptionColumn.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
                descriptionColumn.setOnEditCommit(event -> {
                    FollowUp fu = event.getRowValue();
                    fu.setDescription(event.getNewValue());
                    updateFollowUpInDB(fu);
                });
            }
            if (completedColumn != null) {
                completedColumn.setCellValueFactory(cellData -> cellData.getValue().completedProperty());
                completedColumn.setCellFactory(tc -> new CheckBoxTableCell<FollowUp, Boolean>());
                completedColumn.setOnEditCommit(event -> {
                    FollowUp fu = event.getRowValue();
                    fu.setCompleted(event.getNewValue());
                    updateFollowUpInDB(fu);
                });
            }
        }
    }

    /**
     * Updates the received_book status in the database for a student-module pair.
     * @param studentId The student ID.
     * @param moduleId The module ID.
     * @param receivedBook Whether the book was received.
     */
    private void updateBookIssuedInDB(int studentId, int moduleId, boolean receivedBook) {
        String sql = "UPDATE student_modules SET received_book = ? WHERE student_id = ? AND module_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, receivedBook);
            pstmt.setInt(2, studentId);
            pstmt.setInt(3, moduleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating book issued status", e);
            showError("Error updating book issued status", e.getMessage());
        }
    }

    /**
     * Shows an error alert dialog.
     * @param title The title of the alert.
     * @param message The error message to display.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Saves the exam result for a given student module and exam type to the database.
     * @param sm The StudentModule object.
     * @param examType The type of exam ("formative", "summative", "supplementary").
     * @param value The new value to save.
     */
    public void saveExamResult(StudentModule sm, String examType, Double value) {
        if (sm == null || examType == null || value == null) return;

        String column;
        switch (examType) {
            case "formative":
            case "summative":
            case "supplementary":
                column = examType;
                break;
            default:
                return;
        }

        String sql = "UPDATE student_modules SET " + column + " = ? WHERE student_id = ? AND module_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, value);
            stmt.setInt(2, sm.getStudentId());
            stmt.setInt(3, sm.getModuleId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving exam result", e);
            showError("Error saving exam result", e.getMessage());
        }

        // Check graduation flags after updating marks
        GraduationService graduationService = new GraduationService();
        graduationService.checkAndUpdateGraduationFlags();

        // Reload student details to reflect updated status
        reloadStudentStatus();

        // Refresh parent view if callback is set
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }

    @FXML
    private void initialize() {
        System.out.println("[VirtualRecordCardController] initialize() called");
        // Make TableView and columns editable
        if (moduleTable != null) moduleTable.setEditable(true);
        if (formativeColumn != null) {
            formativeColumn.setCellValueFactory(new PropertyValueFactory<>("formative"));
            formativeColumn.setCellFactory(EditableDoubleCell.forExamType("formative", this));
            formativeColumn.setEditable(true);
        }
        if (summativeColumn != null) {
            summativeColumn.setCellValueFactory(new PropertyValueFactory<>("summative"));
            summativeColumn.setCellFactory(EditableDoubleCell.forExamType("summative", this));
            summativeColumn.setEditable(true);
        }
        if (supplementaryColumn != null) {
            supplementaryColumn.setCellValueFactory(new PropertyValueFactory<>("supplementary"));
            supplementaryColumn.setCellFactory(EditableDoubleCell.forExamType("supplementary", this));
            supplementaryColumn.setEditable(true);
        }
        // Signature column setup
        if (signatureColumn != null) {
            signatureColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSignaturePath()));
            signatureColumn.setCellFactory(col -> new TableCell<StudentModule, String>() {
                private final javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
                {
                    setOnDragOver(event -> {
                        if (event.getGestureSource() != this &&
                            event.getDragboard().hasFiles() &&
                            !event.getDragboard().getFiles().isEmpty() &&
                            isImageFile(event.getDragboard().getFiles().get(0))) {
                            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                        }
                        event.consume();
                    });
                    setOnDragDropped(event -> {
                        javafx.scene.input.Dragboard db = event.getDragboard();
                        boolean success = false;
                        if (db.hasFiles() && !db.getFiles().isEmpty() && isImageFile(db.getFiles().get(0))) {
                            java.io.File sourceFile = db.getFiles().get(0);
                            String originalName = sourceFile.getName();
                            if (isSignatureFilenameUsed(originalName)) {
                                event.setDropCompleted(false);
                                event.consume();
                                javafx.application.Platform.runLater(() ->
                                    showError("Duplicate Image", "This image has already been used for another module. Please use a different image.")
                                );
                                return;
                            } else if (!sourceFile.exists() || !sourceFile.canRead()) {
                                event.setDropCompleted(false);
                                event.consume();
                                javafx.application.Platform.runLater(() ->
                                    showError("Unsupported Source", "Cannot import directly from phone. Please copy the image to your PC first, then drag it here.")
                                );
                                return;
                            } else {
                                try {
                                    java.io.File destDir = getSignaturesDir();
                                    java.io.File destFile = new java.io.File(destDir, System.currentTimeMillis() + "_" + originalName);
                                    java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    StudentModule module = (StudentModule) getTableRow().getItem();
                                    if (module != null) {
                                        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                                        module.setSignaturePath(destFile.getAbsolutePath());
                                        module.setReceivedBook(true);
                                        module.setDateIssued(today);
                                        updateSignatureInDB(module, destFile.getAbsolutePath(), today);
                                        getTableView().refresh();
                                    }
                                    success = true;
                                } catch (Exception ex) {
                                    event.setDropCompleted(false);
                                    event.consume();
                                    javafx.application.Platform.runLater(() ->
                                        showError("Drag-and-Drop Error", "Could not copy image: " + ex.getMessage())
                                    );
                                    return;
                                }
                            }
                        } else {
                            event.setDropCompleted(false);
                            event.consume();
                            javafx.application.Platform.runLater(() ->
                                showError("Invalid File", "Please drag a PNG or JPG image from your PC.")
                            );
                            return;
                        }
                        event.setDropCompleted(success);
                        event.consume();
                    });
                }
                @Override
                protected void updateItem(String path, boolean empty) {
                    super.updateItem(path, empty);
                    if (empty || path == null || path.isEmpty()) {
                        setGraphic(null);
                    } else {
                        java.io.File file = new java.io.File(path);
                        if (file.exists()) {
                            imageView.setImage(new javafx.scene.image.Image(file.toURI().toString(), 100, 50, true, true));
                            setGraphic(imageView);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
                private boolean isImageFile(java.io.File file) {
                    String name = file.getName().toLowerCase();
                    return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                }
            });
        }
        // Date Issued column setup
        if (dateIssuedColumn != null) {
            dateIssuedColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDateIssued()));
            dateIssuedColumn.setCellFactory(col -> new TableCell<StudentModule, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            });
        }
    }

    private Runnable refreshCallback;

    private void reloadStudentStatus() {
        if (selectedStudent == null) return;

        String sql = "SELECT status FROM students WHERE student_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedStudent.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    selectedStudent.setStatus(rs.getString("status")); // Update the local object
                }
            }
        } catch (SQLException e) {
            logger.error("Error reloading student status", e);
            showError("Error reloading student status", e.getMessage());
        }

        // Update the status label in the UI
        if (studentStatusField != null) {
            studentStatusField.setText("Status: " + selectedStudent.getStatus());
        }
    }

    /**
 * Ensures the student's modules exactly match the modules currently linked to their SLP.
 * Removes modules not in the SLP and adds any new ones from the SLP.
 */
    private void syncStudentModulesWithSLP() {
        if (selectedStudent == null) return;
        int studentId = selectedStudent.getId();
        int slpId = -1;

        // Get the student's current SLP ID
        String getSlpIdSql = "SELECT current_slp_id FROM students WHERE student_id = ?";
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement getSlpStmt = conn.prepareStatement(getSlpIdSql)) {
            getSlpStmt.setInt(1, studentId);
            try (ResultSet rs = getSlpStmt.executeQuery()) {
                if (rs.next()) {
                    slpId = rs.getInt("current_slp_id");
                }
            }
            if (slpId == -1) return;

            // 1. Get all module IDs currently linked to the SLP
            List<Integer> slpModuleIds = new java.util.ArrayList<>();
            String slpModulesSql = "SELECT module_id FROM slp_modules WHERE slp_id = ?";
            try (PreparedStatement slpModulesStmt = conn.prepareStatement(slpModulesSql)) {
                slpModulesStmt.setInt(1, slpId);
                try (ResultSet rs = slpModulesStmt.executeQuery()) {
                    while (rs.next()) {
                        slpModuleIds.add(rs.getInt("module_id"));
                    }
                }
            }

            // 2. Get all module IDs currently assigned to the student (excluding replaced)
            List<Integer> studentModuleIds = new java.util.ArrayList<>();
            String studentModulesSql = "SELECT module_id FROM student_modules WHERE student_id = ? AND (status IS NULL OR status != 'replaced')";
            try (PreparedStatement studentModulesStmt = conn.prepareStatement(studentModulesSql)) {
                studentModulesStmt.setInt(1, studentId);
                try (ResultSet rs = studentModulesStmt.executeQuery()) {
                    while (rs.next()) {
                        studentModuleIds.add(rs.getInt("module_id"));
                    }
                }
            }

            // 3. Remove modules from student that are no longer in the SLP
            for (Integer moduleId : studentModuleIds) {
                if (!slpModuleIds.contains(moduleId)) {
                    String deleteSql = "DELETE FROM student_modules WHERE student_id = ? AND module_id = ?";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, studentId);
                        deleteStmt.setInt(2, moduleId);
                        deleteStmt.executeUpdate();
                    }
                }
            }

            // 4. Add modules to student that are in the SLP but not yet assigned
            String getModuleDetailsSql = "SELECT module_code, name FROM modules WHERE module_id = ?";
            for (Integer moduleId : slpModuleIds) {
                if (!studentModuleIds.contains(moduleId)) {
                    // Get module details
                    String moduleCode = null, moduleName = null;
                    try (PreparedStatement getModuleStmt = conn.prepareStatement(getModuleDetailsSql)) {
                        getModuleStmt.setInt(1, moduleId);
                        try (ResultSet rs = getModuleStmt.executeQuery()) {
                            if (rs.next()) {
                                moduleCode = rs.getString("module_code");
                                moduleName = rs.getString("name");
                            }
                        }
                    }
                    if (moduleCode != null && moduleName != null) {
                        String insertSql = "INSERT INTO student_modules (student_id, module_id, module_code, module_name, formative, summative, supplementary, received_book) VALUES (?, ?, ?, ?, 0, 0, 0, 0)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setInt(1, studentId);
                            insertStmt.setInt(2, moduleId);
                            insertStmt.setString(3, moduleCode);
                            insertStmt.setString(4, moduleName);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error syncing student modules with SLP", e);
            showError("Sync Error", "Could not sync modules with SLP: " + e.getMessage());
        }
    }

    private java.io.File getSignaturesDir() {
        java.io.File dir = new java.io.File(System.getProperty("user.home"), "studenttracker_signatures");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void updateSignatureInDB(StudentModule module, String path, String dateIssued) {
        try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(
            "UPDATE student_modules SET signature_path = ?, received_book = ?, date_issued = ? WHERE student_id = ? AND module_id = ?")) {
        ps.setString(1, path);
        ps.setBoolean(2, module.isReceivedBook());
        ps.setString(3, dateIssued);
        ps.setInt(4, module.getStudentId());
        ps.setInt(5, module.getModuleId());
        ps.executeUpdate();
    } catch (Exception e) {
        e.printStackTrace();
        showError("DB Error", "Failed to save signature: " + e.getMessage());
    }
    }

    private boolean isSignatureFilenameUsed(String filename) {
        for (StudentModule sm : studentModules) {
            String sigPath = sm.getSignaturePath();
            if (sigPath != null && !sigPath.isEmpty()) {
                java.io.File f = new java.io.File(sigPath);
                String[] parts = f.getName().split("_", 2);
                String nameToCheck = parts.length == 2 ? parts[1] : f.getName();
                if (nameToCheck.equals(filename)) {
                    return true;
                }
            }
        }
        return false;
    }
}
