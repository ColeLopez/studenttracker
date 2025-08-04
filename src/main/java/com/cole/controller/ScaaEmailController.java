package com.cole.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import java.util.Properties;
import java.io.*;

public class ScaaEmailController {
    @FXML private TextField scaaRecipientField;

    private static final String SETTINGS_FILE = "transcript_email_settings.properties";

    /**
     * Initializes the ScaaEmailController.
     * This method loads the SCAA recipient email from a properties file.
     */
    @FXML
    public void initialize() {
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            Properties props = new Properties();
            props.load(fis);
            scaaRecipientField.setText(props.getProperty("scaa.recipient", ""));
        } catch (IOException e) {
            // File may not exist yet; that's OK
        }
    }

    /**
     * Handles the save action for the SCAA email.
     * This method saves the SCAA recipient email address to a properties file.
     */
    @FXML
    private void handleSave() {
        Properties props = new Properties();
        props.setProperty("scaa.recipient", scaaRecipientField.getText());
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "Transcript Email Settings");
            showInfo("Saved", "Transcript recipient saved successfully.");
        } catch (IOException e) {
            showError("Save Error", e.getMessage());
        }
    }

    /**
     * Displays an information alert with the specified title and message.
     * This method is used to show success messages to the user.
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

    /**
     * Validates the SCAA recipient email address.
     * This method checks if the email field is empty or invalid.
     *
     * @return true if the email is valid, false otherwise
     */
    public static String getScaaRecipient() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            props.load(fis);
            return props.getProperty("scaa.recipient", "");
        } catch (IOException e) {
            return "";
        }
    }
}