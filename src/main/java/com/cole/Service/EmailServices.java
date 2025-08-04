package com.cole.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class EmailServices {
    
    /**
     * Loads email settings from a properties file.
     * @return Properties object containing email settings, or null if loading fails
     */
    public static Properties loadEmailSettings() {
        Properties config = new Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream("email_settings.properties")) {
            config.load(fis);
            return config;
        } catch (Exception e) {
            // Optionally log the error here if you have a logger
            return null;
        }
    }

    /**
     * Prompts the user for their email password using a dialog.
     * @return the entered password, or null if cancelled
     */
    public static String promptForPassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Email Password");
        dialog.setHeaderText("Please enter your email password:");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        TextField visibleField = new TextField();
        visibleField.setPromptText("Password");
        visibleField.setManaged(false);
        visibleField.setVisible(false);

        CheckBox showPasswordCheck = new CheckBox("Show password");
        passwordField.textProperty().bindBidirectional(visibleField.textProperty());

        showPasswordCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                visibleField.setText(passwordField.getText());
                dialog.getDialogPane().setContent(new VBox(visibleField, showPasswordCheck));
                visibleField.setManaged(true);
                visibleField.setVisible(true);
                passwordField.setManaged(false);
                passwordField.setVisible(false);
            } else {
                passwordField.setText(visibleField.getText());
                dialog.getDialogPane().setContent(new VBox(passwordField, showPasswordCheck));
                passwordField.setManaged(true);
                passwordField.setVisible(true);
                visibleField.setManaged(false);
                visibleField.setVisible(false);
            }
        });

        dialog.getDialogPane().setContent(new VBox(passwordField, showPasswordCheck));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return showPasswordCheck.isSelected() ? visibleField.getText() : passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Sends an email with the specified parameters.
     * @param from the sender's email address
     * @param password the sender's email password
     * @param to the recipient's email address
     * @param cc additional CC recipients
     * @param subject the subject of the email
     * @param body the body of the email
     * @param smtp SMTP server address
     * @param port SMTP server port
     * @throws Exception if sending fails
     */
    public static void sendEmail(String from, String password, String to, String cc, String subject, String body, String smtp, String port) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtp);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

        // Always add manager CC
        String managerCC = getManagerCC();
        String allCC = "";
        if (cc != null && !cc.isEmpty()) {
            allCC = cc;
        }
        if (managerCC != null && !managerCC.isEmpty()) {
            if (!allCC.isEmpty()) {
                allCC += "," + managerCC;
            } else {
                allCC = managerCC;
            }
        }
        if (!allCC.isEmpty()) {
            message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(allCC));
        }

        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    /**
     * Gets the SCAA recipient email address from properties file.
     * @return the SCAA recipient email address
     */
    public static String getScaaRecipient() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("transcript_email_settings.properties")) {
            props.load(fis);
            return props.getProperty("scaa.recipient", "");
        } catch (IOException e) {
            // Optionally log error
            return "";
        }
    }

    /**
     * Gets the CC email address for the manager.
     * @return the manager's CC email address
     */
    public static String getManagerCC() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("manager_email_settings.properties")) {
            props.load(fis);
            return props.getProperty("manager.cc", "");
        } catch (IOException e) {
            // Optionally log error
            return "";
        }
    }
}
