package com.cole.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import java.util.Properties;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(EmailSettingsController.class);
    @FXML private TextField emailField;
    @FXML private TextField smtpField;      
    @FXML private TextField portField;
    
    private static final String SETTINGS_FILE = "email_settings.properties";

    @FXML
    public void initialize() {
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            Properties props = new Properties();
            props.load(fis);
            emailField.setText(props.getProperty("email.sender", ""));
            smtpField.setText(props.getProperty("email.smtp", ""));
            portField.setText(props.getProperty("email.port", ""));
        } catch (IOException e) {
            logger.error("Error Retrieving Follow-Ups", e);
        }
    }

    @FXML
    private void handleSave() {
        Properties props = new Properties();
        props.setProperty("email.sender", emailField.getText());
        props.setProperty("email.smtp", smtpField.getText());
        props.setProperty("email.port", portField.getText());
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "Email Settings");
            showInfo("Saved", "Email settings saved successfully.");
        } catch (IOException e) {
            logger.error("Error Saving Email Settings", e);
            showError("Save Error", e.getMessage());
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
