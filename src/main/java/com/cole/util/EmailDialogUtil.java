package com.cole.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EmailDialogUtil {
    /** Session password for email operations. */
    private static String sessionPassword = null;

    /**
     * Gets the session password for email operations.
     * If the password is already set, it returns the existing value.
     * Otherwise, it prompts the user to enter their email password.
     *
     * @param owner the owner stage for the dialog
     * @return the email password entered by the user
     */
    public static String getSessionPassword(Stage owner) {
        if (sessionPassword != null && !sessionPassword.isEmpty()) {
            return sessionPassword;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Email Password");
        dialog.setHeaderText("Enter your email password");
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // UI controls
        PasswordField passwordField = new PasswordField();
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);

        CheckBox showPasswordCheckBox = new CheckBox("Show Password");

        // Bindings for show/hide password
        passwordField.textProperty().bindBidirectional(visiblePasswordField.textProperty());
        showPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                visiblePasswordField.setText(passwordField.getText());
                visiblePasswordField.setManaged(true);
                visiblePasswordField.setVisible(true);
                passwordField.setManaged(false);
                passwordField.setVisible(false);
            } else {
                passwordField.setText(visiblePasswordField.getText());
                passwordField.setManaged(true);
                passwordField.setVisible(true);
                visiblePasswordField.setManaged(false);
                visiblePasswordField.setVisible(false);
            }
        });

        // Layout for the dialog
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 20));
        grid.add(new Label("Password:"), 0, 0);
        grid.add(passwordField, 1, 0);
        grid.add(visiblePasswordField, 1, 0);
        grid.add(showPasswordCheckBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(passwordField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return showPasswordCheckBox.isSelected() ? visiblePasswordField.getText() : passwordField.getText();
            }
            return null;
        });

        String result = dialog.showAndWait().orElse(null);
        if (result != null && !result.isEmpty()) {
            sessionPassword = result;
        }
        return sessionPassword;
    }

    /**
     * Displays a progress dialog for email operations.
     * This dialog shows a progress bar and a message while the email task is running.
     *
     * @param owner the owner stage for the dialog
     * @param task the Task representing the email operation
     * @param title the title of the dialog
     * @param <T> the type of result produced by the task
     */
    public static <T> void showEmailProgressDialog(Stage owner, Task<T> task, String title) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(20);
        progressBar.setStyle("-fx-accent: #2196F3; -fx-background-radius: 10; -fx-border-radius: 10;");

        progressBar.progressProperty().bind(task.progressProperty());

        Label messageLabel = new Label();
        messageLabel.textProperty().bind(task.messageProperty());
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox vbox = new VBox(20, progressBar, messageLabel);
        vbox.setStyle("-fx-padding: 30; -fx-background-color: #f4f4f4; -fx-border-radius: 10; -fx-background-radius: 10;");
        vbox.setSpacing(20);
        vbox.setPrefWidth(350);
        vbox.setPrefHeight(120);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        dialog.getDialogPane().setContent(vbox);

        dialog.setOnCloseRequest(e -> task.cancel());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        task.setOnSucceeded(e -> dialog.close());
        task.setOnFailed(e -> dialog.close());
        task.setOnCancelled(e -> dialog.close());

        dialog.showAndWait();
    }

    /**
     * Clears the session password, allowing the user to re-enter it if needed.
     * This is useful for scenarios where the password may change or needs to be reset.
     */
    public static void clearSessionPassword() {
        sessionPassword = null;
    }
}
