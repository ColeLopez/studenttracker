package com.cole.controller;

import java.sql.Statement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cole.Service.StudentReportsService;
import com.cole.model.Student;
import com.cole.model.StudentReportData;
import com.cole.util.DBUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.collections.transformation.FilteredList;

import javafx.scene.input.MouseButton;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for viewing students in a TableView.
 */
public class StudentViewController {
    private static final Logger logger = LoggerFactory.getLogger(StudentViewController.class);

    @FXML
    private TableView<Student> studentTable;

    @FXML
    private TableColumn<Student, String> numberColumn;

    @FXML
    private TableColumn<Student, String> nameColumn;

    @FXML
    private TableColumn<Student, String> emailColumn;

    @FXML
    private TableColumn<Student, String> phoneColumn;

    @FXML
    private TableColumn<Student, String> slpColumn;

    @FXML
    private TableColumn<Student, String> statusColumn;

    private final ObservableList<Student> studentList = FXCollections.observableArrayList();
    private FilteredList<Student> filteredStudents;

    @FXML
    private TextField searchField; // fx:id must match your FXML

    /**
     * Initializes the StudentViewController.
     * This method sets up the TableView columns and loads students from the database.
     */
    @FXML
    private void initialize() {
        numberColumn.setCellValueFactory(cellData -> cellData.getValue().studentNumberProperty());
        nameColumn.setCellValueFactory(cellData -> {
            Student s = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(s.getFirstName() + " " + s.getLastName());
        });
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        phoneColumn.setCellValueFactory(cellData -> cellData.getValue().phoneNumberProperty());
        slpColumn.setCellValueFactory(cellData -> cellData.getValue().slpProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        filteredStudents = new FilteredList<>(studentList, p -> true);
        studentTable.setItems(filteredStudents);
        loadStudents();

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredStudents.setPredicate(student -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    // Filter by student number or SLP (case-insensitive)
                    if (student.getStudentNumber().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (student.getSlp().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                    return false;
                });
            });
        }

        // Add double-click event to open virtual record card
        studentTable.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();

            // Double-click to open virtual record card
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Student clickedStudent = row.getItem();
                    openVirtualRecordCard(clickedStudent);
                }
            });

            // Context menu for right-click actions
            ContextMenu contextMenu = new ContextMenu();
            MenuItem generateReportItem = new MenuItem("Generate Report");
            generateReportItem.setOnAction(event -> {
                Student selectedStudent = row.getItem();
                if (selectedStudent != null) {
                    generateStudentReport(selectedStudent.getStudentNumber());
                }
            });
            contextMenu.getItems().add(generateReportItem);
            row.setContextMenu(contextMenu);

            return row;
        });
    }

    /**
     * Generates a report for the selected student.
     * @param studentNumber The student number of the selected student.
     */
    private void generateStudentReport(String studentNumber) {
        
        try{
            // Get the report data using your service
            StudentReportsService reportsService = new StudentReportsService();
            StudentReportData reportData = reportsService.getStudentReportData(studentNumber);
            if(reportData == null){
                showError("Report Generation Error", "No data found for student number: " + studentNumber);
                return;
            }

            //Show a file chooser to save the report
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save Student Report");    
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                );
            fileChooser.setInitialFileName("Student_Report_" + studentNumber + ".pdf");
            java.io.File file = fileChooser.showSaveDialog(studentTable.getScene().getWindow());
            if(file != null){
                // Call the service to generate the report
                reportsService.exportStudentSummaryPdf(reportData, file);
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Report Generated");
                    alert.setHeaderText(null);
                    alert.setContentText("Report generated successfully: " + file.getAbsolutePath());
                    alert.showAndWait();
                });
            }
        } catch (Exception e) {
            logger.error("Failed to generate report for student number: " + studentNumber, e);
            showError("Report Generation Error", "Failed to generate report for student number: " + studentNumber + "\n" + e.getMessage());
        }
    }

    /**
     * Opens the Virtual Record Card window for the selected student.
     * @param student The student to display in the record card.
     */
    private void openVirtualRecordCard(Student student) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/virtualRecordCard.fxml"));
            Parent root = loader.load();
            VirtualRecordCardController controller = loader.getController();
            controller.setStudent(student);
            // Set callback to refresh student list after changes (e.g., delete or status change)
            controller.setRefreshCallback(() -> loadStudents());
            Stage stage = new Stage();
            stage.setTitle("Virtual Record Card - " + student.getFirstName() + " " + student.getLastName());
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to open Virtual Record Card", e);
            showError("Failed to open Virtual Record Card", e.getMessage());
        }
    }


    /**
     * Loads students from the database and populates the studentList.
     */
    private void loadStudents() {
        studentList.clear();

        String sql = "SELECT s.student_id, s.student_number, s.first_name, s.last_name, s.email, s.phone, sl.name AS slp_name, s.status, s.enrollment_date " +
                     "FROM students s " +
                     "LEFT JOIN slps sl ON s.current_slp_id = sl.slp_id " +
                     "ORDER BY s.enrollment_date DESC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                studentList.add(new Student(
                    rs.getInt("student_id"),
                    rs.getString("student_number"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("slp_name"),
                    rs.getString("status"),
                    rs.getString("enrollment_date")
                ));
            }
            studentTable.setItems(studentList);
        } catch (SQLException e) {
            logger.error("Error loading students", e);
            showError("Error loading students", e.getMessage());
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

}