package com.cole.controller;

import com.cole.model.StudentToGraduate;
import com.cole.util.DBUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

    @FXML
    public void initialize() {
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
    }

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
}
