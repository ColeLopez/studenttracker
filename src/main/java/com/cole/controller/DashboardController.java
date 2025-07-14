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
    @FXML
    public void initialize() {
        setCenterContent("/fxml/dashboardHome.fxml");
    }

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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You have been logged out.");
        alert.setContentText("Implement scene switching to go back to login.");
        alert.showAndWait();
    }

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

    @FXML
    private void handleManageSLPs(ActionEvent event) {
        setCenterContent("/fxml/slp.fxml");
    }

    @FXML
    private void handleManageSLPModules(ActionEvent event) {
        setCenterContent("/fxml/slpModules.fxml");
    }
}
