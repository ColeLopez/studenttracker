package com.cole.controller;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cole.model.Student;
import com.cole.util.DBUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.collections.transformation.FilteredList;

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
     * Initializes the controller and sets up the TableView columns.
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
                    // Search by student number, first name, last name, or SLP name
                    if (student.getStudentNumber().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if ((student.getFirstName() + " " + student.getLastName()).toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (student.getSlp().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                    return false;
                });
            });
        }
    }


    /**
     * Loads students from the database and populates the studentList.
     */
    private void loadStudents() {
        studentList.clear();

        String sql = "SELECT s.student_id, s.student_number, s.first_name, s.last_name, s.email, s.phone, sl.name AS slp_name, s.status " +
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
                    rs.getString("slp_name"), // Use SLP name instead of ID
                    rs.getString("status")
                ));
            }
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