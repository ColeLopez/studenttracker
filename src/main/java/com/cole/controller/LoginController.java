package com.cole.controller;

import java.io.IOException;

import com.cole.Service.ActivityService;
import com.cole.Service.AuthService;
import com.cole.model.User;
import com.cole.util.UserSession; // <-- Use util, not session package
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.concurrent.Task;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;


public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    @FXML
    private Button loginButton;

    // This is the controller class for handling user login functionality
    // This class will handle the login logic
    // It will interact with the database to verify user credentials
    // and manage the login process

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    private final AuthService authService = new AuthService();

    /**
     * Handles key press events in the login form.
     * Invoked by the UI when a key is pressed in the login fields.
     * If ENTER is pressed, triggers the login process.
     * @param event KeyEvent from the UI
     * @throws IOException if scene loading fails
     */
    @FXML
    private void handleKeyPressed(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    /**
     * Handles the login process when the user clicks the login button or presses ENTER.
     * Validates input fields and shows error dialogs if validation fails.
     * On success, switches to the dashboard scene.
     */
    @FXML
    private void handleLogin() {
        if (usernameField == null || passwordField == null) {
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Username or password field is not initialized.");
            return;
        }

        String username = usernameField.getText();
        String password = String.valueOf(passwordField.getText());

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Username and password must not be empty.");
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.login(username, password);
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                User user = authService.getUserByUsername(username);
                UserSession.getInstance().setUser(user); // <-- Set session here
                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome " + username + "!");
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Dashboard");
                    stage.centerOnScreen();

                    ActivityService.logActivity(
                        user.getId(),
                        "USER_LOGIN",
                        "User " + username + " logged in."
                    );
                } catch (IOException ex) {
                    logger.error("Failed to load dashboard.fxml", ex);
                    showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load dashboard.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            }
        });
        task.setOnFailed(e -> {
            logger.error("Login process failed", task.getException());
            showAlert(Alert.AlertType.ERROR, "Login Error", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    /**
     * Shows an alert dialog with the given type, title, and message.
     * @param type Alert type (e.g., ERROR, INFORMATION)
     * @param title Dialog title (short description)
     * @param message Message to display (detailed)
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
