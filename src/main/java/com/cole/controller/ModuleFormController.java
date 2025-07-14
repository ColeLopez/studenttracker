package com.cole.controller;

import com.cole.util.DBUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ModuleFormController {

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField passMarkField;

    @FXML
    private void handleSave() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String passMarkText = passMarkField.getText().trim();

        if (code.isEmpty() || name.isEmpty() || passMarkText.isEmpty()) {
            showAlert("Validation Error", "All fields must be filled.");
            return;
        }

        try {
            int passMark = Integer.parseInt(passMarkText);
            if (passMark < 0 || passMark > 100) {
                showAlert("Validation Error", "Pass mark must be between 0 and 100.");
                return;
            }

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO modules (module_code, name, pass_result) VALUES (?, ?, ?)")) {

                stmt.setString(1, code);
                stmt.setString(2, name);
                stmt.setInt(3, passMark);
                stmt.executeUpdate();
                closeWindow();

            } catch (Exception e) {
                showAlert("Database Error", e.getMessage());
            }

        } catch (NumberFormatException e) {
            showAlert("Input Error", "Pass mark must be a valid number.");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
