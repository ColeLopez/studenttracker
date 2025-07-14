package com.cole.controller;

import com.cole.Service.ModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// ...existing code...

/**
 * Controller for the module form dialog.
 * <p>
 * Handles UI events and database operations for adding new modules.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Validating module input fields</li>
 *   <li>Saving new modules to the database</li>
 *   <li>Closing the dialog window</li>
 *   <li>Showing error dialogs for validation and database errors</li>
 * </ul>
 * <p>
 * All database operations are performed asynchronously using JavaFX Tasks.
 */
public class ModuleFormController {
    private static final Logger logger = LoggerFactory.getLogger(ModuleFormController.class);
    private final ModuleService moduleService = new ModuleService();

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField passMarkField;

    /**
     * Handles saving a new module to the database.
     * Invoked by the UI when the user clicks the "Save" button.
     * Validates input fields and shows error dialogs if validation fails.
     * On success, closes the dialog window.
     */
    @FXML
    private void handleSave() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String passMarkText = passMarkField.getText().trim();

        if (code.isEmpty() || name.isEmpty() || passMarkText.isEmpty()) {
            showAlert("Validation Error", "All fields must be filled.");
            return;
        }

        int passMark;
        try {
            passMark = Integer.parseInt(passMarkText);
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Pass mark must be a valid number.");
            return;
        }
        if (passMark < 0 || passMark > 100) {
            showAlert("Validation Error", "Pass mark must be between 0 and 100.");
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return moduleService.addModule(code, name, passMark);
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                closeWindow();
            } else {
                showAlert("Database Error", "Failed to save module.");
            }
        });
        task.setOnFailed(e -> {
            logger.error("Failed to save module", task.getException());
            showAlert("Database Error", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    /**
     * Handles cancelling the module form dialog.
     * Invoked by the UI when the user clicks the "Cancel" button.
     * Closes the dialog window without saving.
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Closes the module form dialog window.
     * Called internally after save or cancel actions.
     */
    private void closeWindow() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        stage.close();
    }

    /**
     * Shows an error dialog with the given title and message.
     * @param title Dialog title (short description)
     * @param msg Error message (detailed)
     */
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
