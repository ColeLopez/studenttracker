package com.cole.controller;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    @FXML
    private AnchorPane contentArea;
    /**
     * Initializes the controller and loads the dashboard home view.
     * This method is called automatically by the JavaFX framework after FXML loading.
     */
    @FXML
    public void initialize() {
        setCenterContent("/fxml/dashboardHome.fxml");
    }

    /**
     * Loads and sets the center content of the dashboard to the specified FXML view.
     * @param fxmlPath Path to the FXML file to load (e.g., "/fxml/dashboardHome.fxml")
     */
    private void setCenterContent(String fxmlPath){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
        }
        catch (IOException e) {
            logger.error("Failed to load FXML: {}", fxmlPath, e);
            showError("Error loading view", "Could not load: " + fxmlPath);
        }
    }

    /**
     * Shows an error dialog with the given title and message.
     * @param title Dialog title (short description)
     * @param message Error message (detailed)
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handles the logout action when the user clicks the logout button.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You have been logged out.");
        alert.setContentText("Implement scene switching to go back to login.");
        alert.showAndWait();
    }

    /**
     * Handles the exit action when the user clicks the exit button.
     * Prompts for confirmation before closing the application.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleExit(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Exit Application");
        confirm.setHeaderText("Are you sure you want to exit?");
        confirm.setContentText("All unsaved changes will be lost.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.close();
            }
        });
    }

    /**
     * Handles the action to manage SLPs when the user clicks the corresponding button.
     * Switches the center content to the SLP management view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleManageSLPs(ActionEvent event) {
        setCenterContent("/fxml/slp.fxml");
    }

    /**
     * Handles the action to manage SLP modules when the user clicks the corresponding button.
     * Switches the center content to the SLP modules management view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleManageSLPModules(ActionEvent event) {
        setCenterContent("/fxml/slpModules.fxml");
    }

    /**
     * Handles the action to manage Student Registration when the user clicks the corresponding button.
     * Switches the center content to the Student Registration view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleStudentRegistration(ActionEvent event) {
        setCenterContent("/fxml/studentRegistration.fxml");
    }

    /**
     * Handles the action to view students when the user clicks the corresponding button.
     * Switches the center content to the Student View.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleViewStudents(ActionEvent event) {
        setCenterContent("/fxml/studentView.fxml");
    }

    /**
     * Handles the action to manage follow-ups when the user clicks the corresponding button.
     * Switches the center content to the Follow-Up management view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleManageFollowUps(ActionEvent event) {
        setCenterContent("/fxml/followUps.fxml");
    }

    /**
     * Handles the action to manage student follow-ups when the user clicks the corresponding button.
     * Switches the center content to the Student Follow-Up management view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleEmailSettings(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/emailSettings.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setTitle("Email Settings");
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(Stage.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .findFirst()
                .orElse(null));
            popupStage.centerOnScreen();
            popupStage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to load Email Settings FXML", e);
            showError("Error", "Could not open Email Settings.");
        }
    }

    /**
     * Handles the action to manage graduates when the user clicks the corresponding button.
     * Switches the center content to the Graduates management view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleManageGraduates(ActionEvent event) {
        setCenterContent("/fxml/graduates.fxml");
    }

    /**
     * Handles the action to manage SCAA emails when the user clicks the corresponding button.
     * Switches the center content to the SCAA Email management view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleSCAAEmail(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/scaaSettings.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setTitle("SCAA Email Settings");
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(Stage.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .findFirst()
                .orElse(null));
            popupStage.centerOnScreen();
            popupStage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to load Email Settings FXML", e);
            showError("Error", "Could not open Email Settings.");
        }
    }

    /**
     * Handles the action to manage SCAA emails when the user clicks the corresponding button.
     * Switches the center content to the SCAA Email management view.
     * @param event ActionEvent from the UI
     */ 
    @FXML
    private void handleManagerEmail(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/managerEmailSettings.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setTitle("Manager Email Settings");
            popupStage.setScene(new javafx.scene.Scene(root));
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(Stage.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .findFirst()
                .orElse(null));
            popupStage.centerOnScreen();
            popupStage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to load Email Settings FXML", e);
            showError("Error", "Could not open Email Settings.");
        }
    }
}
