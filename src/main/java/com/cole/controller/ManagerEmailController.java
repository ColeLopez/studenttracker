package com.cole.controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class ManagerEmailController {
    @FXML

    
    private static final Logger logger = LoggerFactory.getLogger(ManagerEmailController.class);
    @FXML private TextField managerEmailField;

    private static final String SETTINGS_FILE = "manager_email_settings.properties";

    /**
     * Initializes the ManagerEmailController.
     * This method loads the manager email settings from a properties file.
     */
    @FXML
    public void initialize() {
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            Properties props = new Properties();
            props.load(fis);
            managerEmailField.setText(props.getProperty("manager.cc", ""));
        } catch (IOException e) {
            logger.error("Error loading manager email settings", e);
        }
    }

    /**
     * Handles the save action for the manager email.
     * This method saves the manager's email address to a properties file.
     */
    @FXML
    private void handleSave() {
        Properties props = new Properties();
        props.setProperty("manager.cc", managerEmailField.getText());
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "Manager Email Settings");
            showInfo("Saved", "Manager cc saved successfully.");
        } catch (IOException e) {
            logger.error("Error saving manager email settings", e);
            showError("Save Error", e.getMessage());
        }
    }

    /**
     * Displays an information alert with the specified title and message.
     * This method is used to show informational messages to the user.
     *
     * @param title the title of the information alert
     * @param message the content of the information message
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
