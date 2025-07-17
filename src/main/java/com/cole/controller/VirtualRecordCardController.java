// ...existing code...
package com.cole.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import com.cole.model.Student;
import com.cole.model.StudentModule;
import com.cole.util.DBUtil;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualRecordCardController {
    // --- Editable Notes and Follow-Up Section ---
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

    // FollowUp model
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

    // Editable cell for exam type columns
    public static class EditableDoubleCell extends javafx.scene.control.TableCell<StudentModule, Double> {
        private final javafx.scene.control.TextField textField = new javafx.scene.control.TextField();
        private final String examType;
        private final VirtualRecordCardController controller;

        public EditableDoubleCell(String examType, VirtualRecordCardController controller) {
            this.examType = examType;
            this.controller = controller;
            textField.setOnAction(e -> commitEdit(parseDouble(textField.getText())));
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) commitEdit(parseDouble(textField.getText()));
            });
        }
        @Override
        public void startEdit() {
            super.startEdit();
            textField.setText(getItem() == null ? "" : getItem().toString());
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            textField.requestFocus();
        }
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() == null ? "" : getItem().toString());
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
        @Override
        public void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                textField.setText(item == null ? "" : item.toString());
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(item == null ? "" : item.toString());
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
        @Override
        public void commitEdit(Double newValue) {
            super.commitEdit(newValue);
            StudentModule sm = getTableView().getItems().get(getIndex());
            controller.saveExamResult(sm, examType, newValue);
            if ("formative".equals(examType)) sm.setFormative(newValue);
            else if ("summative".equals(examType)) sm.setSummative(newValue);
            else if ("supplementary".equals(examType)) sm.setSupplementary(newValue);
        }
        private Double parseDouble(String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return null; }
        }
        public static javafx.util.Callback<TableColumn<StudentModule, Double>, TableCell<StudentModule, Double>> forExamType(String examType, VirtualRecordCardController controller) {
            return col -> new EditableDoubleCell(examType, controller);
        }
    }

    // TableCell for color coding pass/fail in exam columns
    public static class ColorCodedDoubleCell extends EditableDoubleCell {
        public ColorCodedDoubleCell(String examType, VirtualRecordCardController controller) {
            super(examType, controller);
        }

        @Override
        public void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setStyle("");
            } else {
                // Get pass rate from StudentModule (default to 50 if not found)
                int passRate = 50;
                try {
                    StudentModule sm = (StudentModule) getTableRow().getItem();
                    if (sm != null) {
                        java.lang.reflect.Field f = sm.getClass().getDeclaredField("passRate");
                        f.setAccessible(true);
                        passRate = f.getInt(sm);
                    }
                } catch (Exception ignore) {}
                if (item >= passRate) {
                    setStyle("-fx-background-color: #c8e6c9;"); // light green
                } else {
                    setStyle("-fx-background-color: #ffcdd2;"); // light red
                }
            }
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

    @FXML private TableView<FollowUp> followUpTable;
    @FXML private TableColumn<FollowUp, String> dueDateColumn;
    @FXML private TableColumn<FollowUp, String> descriptionColumn;
    @FXML private TableColumn<FollowUp, Boolean> completedColumn;

    // FXML fields for buttons and labels
    @FXML private Button handleAddModule;
    @FXML private Button handleAddFollowUp;
    @FXML private Button handleEditStudent;
    @FXML private Button handleDeleteStudent;
    @FXML private Button handleClose;

    @FXML private ListView<Note> noteList;
    @FXML private TextField noteInputField;
    @FXML private DatePicker followUpDueDateField;
    @FXML private TextField followUpDescField;

    @FXML private Label nameLabel;
    @FXML private Label studentNumberLabel;
    @FXML private Label slpLabel;
    @FXML private Label statusLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;

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
        loadStudentDetails();
        loadStudentModules();
        loadNotes();
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
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notes.add(new Note(rs.getInt("note_id"), rs.getString("note_text"), rs.getString("date_added")));
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
        }
        noteInputField.clear();
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
        }
        followUpDueDateField.setValue(null);
        followUpDescField.clear();
    }

    // Update note in DB
    private void updateNoteInDB(Note note) {
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
        if (nameLabel != null) nameLabel.setText(selectedStudent.getFirstName() + " " + selectedStudent.getLastName());
        if (studentNumberLabel != null) studentNumberLabel.setText(selectedStudent.getStudentNumber());
        if (slpLabel != null) slpLabel.setText(selectedStudent.getSlp());
        if (statusLabel != null) statusLabel.setText(selectedStudent.getStatus());
        if (emailLabel != null) emailLabel.setText(selectedStudent.getEmail());
        if (phoneLabel != null) phoneLabel.setText(selectedStudent.getPhoneNumber());
    }

    /**
     * Loads the modules for the selected student from the database.
     * Populates the studentModules ObservableList.
     */
    private void loadStudentModules() {
        // ...existing code...
        // (No duplicate ColorCodedDoubleCell here)
        if (supplementaryColumn != null) {
            supplementaryColumn.setCellValueFactory(new PropertyValueFactory<>("supplementary"));
            supplementaryColumn.setCellFactory(col -> new ColorCodedDoubleCell("supplementary", this));
        }
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
        if (moduleTable != null) moduleTable.setItems(studentModules);

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

    // Editable cell for exam type columns
    // (No duplicate EditableDoubleCell or ColorCodedDoubleCell here)
    // ...existing code...


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
                column = "formative";
                break;
            case "summative":
                column = "summative";
                break;
            case "supplementary":
                column = "supplementary";
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
    }

    @FXML
    private void initialize() {
        // Hint: initialize() will be called when the associated FXML has been completely loaded.
    }
}
