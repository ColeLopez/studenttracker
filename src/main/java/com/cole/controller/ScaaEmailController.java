package com.cole.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import java.util.Properties;
import java.io.*;

public class ScaaEmailController {
    @FXML private TextField scaaRecipientField;

    private static final String SETTINGS_FILE = "transcript_email_settings.properties";

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

    // Static method to get the SCAA recipient email for use elsewhere
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