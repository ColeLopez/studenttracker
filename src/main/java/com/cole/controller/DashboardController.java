package com.cole.controller;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cole.model.StudentReportData;
import com.cole.Service.StudentReportsService;
import com.cole.Service.GraduatesExportService;
import com.cole.Service.FollowUpExportService;
import com.cole.Service.DatabaseBackupService;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;

public class DashboardController {
    @FXML
    private Label userLabel;

    @FXML
    private VBox sidebar;

    @FXML
    private Button collapseButton;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Button studentRegBtn;

    @FXML
    private Button viewStudentsBtn;

    @FXML
    private Button manageFollowUpsBtn;

    @FXML
    private Button manageGraduatesBtn;

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    @FXML
    private AnchorPane contentArea;
    private final StudentReportsService reportsService = new StudentReportsService(); // Assuming ReportsService is used for report generation

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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
     * Handles the action to view the dashboard home when the user clicks the corresponding button.
     * Switches the center content to the Dashboard Home view.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleDashboard(ActionEvent event) {
        setCenterContent("/fxml/dashboardHome.fxml");
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

    /**
     * Handles the action to generate and export the student report when the user clicks the corresponding button.
     * Prompts for the student number and saves the report as a PDF file.
     * @param event ActionEvent from the UI
     */
    public void handleStudentReport(ActionEvent event) {
        // Prompt for student number
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Export Student Summary");
        dialog.setHeaderText("Enter Student Number");
        dialog.setContentText("Student Number:");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(studentNumber -> {
            StudentReportData reportData = reportsService.getStudentReportData(studentNumber);
            if (reportData == null) {
                showError("Not Found", "No student found with number: " + studentNumber);
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Student Summary PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try {
                    reportsService.exportStudentSummaryPdf(reportData, file);
                    showInfo("Export Successful", "Student summary exported to:\n" + file.getAbsolutePath());
                } catch (Exception e) {
                    showError("Export Error", "Could not export Student Summary as PDF.\n" + e.getMessage());
                }
            }
        });
    }

    /**
     * Handles the action to export the follow-up history report when the user clicks the corresponding button.
     * Prompts the user to select a filter (e.g., completed, upcoming, due) and saves the report as an Excel file.
     * @param event ActionEvent from the UI
     */
    @FXML
    private void handleFollowUpHistoryReport() {
        // Prompt user to select filter type
        ChoiceDialog<String> dialog = new ChoiceDialog<>("completed", "completed", "upcoming", "overdue");
        dialog.setTitle("Select Follow-Up Filter");
        dialog.setHeaderText("Export Follow-Up History");
        dialog.setContentText("Choose the type of follow-ups to export:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String filter = result.get();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Follow-Up History");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("follow_up_history_" + filter + ".xlsx");
        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        FollowUpExportService exportService = new FollowUpExportService();
        try {
            exportService.exportFollowUpHistoryToExcel(file, filter);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText(null);
            alert.setContentText("Follow-Up History exported successfully:\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText(null);
            alert.setContentText("Failed to export Follow-Up History:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    public void handleGraduatesReport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Graduates List");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("graduates.xlsx");
        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        GraduatesExportService exportService = new GraduatesExportService();
        try {
            exportService.exportGraduatesToExcel(file);
            showInfo("Export Successful", "Graduates list exported to:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Export Error", "Could not export graduates list.\n" + e.getMessage());
        }
    }

    private final DatabaseBackupService dbBackupService = new DatabaseBackupService();

    /**
     * Handles backing up the SQLite database.
     */
    @FXML
    private void handleBackup(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Backup Destination");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB Backup", "*.db", "*.sqlite", "*.bak", "*.*"));
        fileChooser.setInitialFileName("studenttracker_backup.db");
        File backupFile = fileChooser.showSaveDialog(null);
        
        if (backupFile == null) return;
        File dbFile = new File("database/data.db");
        if (!dbFile.exists()) {
            showError("Database Not Found", "Could not find the database file: " + dbFile.getAbsolutePath());
            return;
        }

        try {
            dbBackupService.backupDatabase(dbFile, backupFile);
            showInfo("Backup Successful", "Database backed up to:\n" + backupFile.getAbsolutePath());
        } catch (IOException e) {
            showError("Backup Failed", "Could not backup database:\n" + e.getMessage());
        }
    }

    /**
     * Handles restoring the SQLite database from a backup.
     */
    @FXML
    private void handleRestore(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Backup File to Restore");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB Backup", "*.db", "*.sqlite", "*.bak", "*.*"));
        File backupFile = fileChooser.showOpenDialog(null);
        if (backupFile == null) return;

        // Adjust this path to your actual SQLite DB location
        File dbFile = new File("database/data.db");
        if (!dbFile.exists()) {
            showError("Database Not Found", "Could not find the database file: " + dbFile.getAbsolutePath());
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Restore");
        confirm.setHeaderText("Restore database from backup?");
        confirm.setContentText("This will overwrite the current database.\nProceed?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) return;

        try {
            dbBackupService.restoreDatabase(backupFile, dbFile);
            showInfo("Restore Successful", "Database restored from:\n" + backupFile.getAbsolutePath());
        } catch (IOException e) {
            showError("Restore Failed", "Could not restore database:\n" + e.getMessage());
        }
    }

    /**
     * Handles toggling the sidebar visibility.
     */
    private boolean sidebarCollapsed = false;
    private final double SIDEBAR_EXPANDED_WIDTH = 200;
    private final double SIDEBAR_COLLAPSED_WIDTH = 50;
    @FXML private FontIcon collapseIcon;

    /**
     * Toggles the visibility of the sidebar.
     * @param event
     */
    @FXML
    private void toggleSidebar(ActionEvent event) {
        double start = sidebar.getPrefWidth();
        double end = sidebarCollapsed ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(sidebar.prefWidthProperty(), start)),
            new KeyFrame(Duration.millis(200), new KeyValue(sidebar.prefWidthProperty(), end))
        );

        for (javafx.scene.Node node : sidebar.getChildren()) {
            if (node instanceof Button btn && btn != collapseButton) {
                double btnStart = btn.getPrefWidth();
                double btnEnd = end;
                timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(btn.prefWidthProperty(), btnStart)),
                    new KeyFrame(Duration.millis(200), new KeyValue(btn.prefWidthProperty(), btnEnd)),
                    new KeyFrame(Duration.ZERO, new KeyValue(btn.minWidthProperty(), btnStart)),
                    new KeyFrame(Duration.millis(200), new KeyValue(btn.minWidthProperty(), btnEnd)),
                    new KeyFrame(Duration.ZERO, new KeyValue(btn.maxWidthProperty(), btnStart)),
                    new KeyFrame(Duration.millis(200), new KeyValue(btn.maxWidthProperty(), btnEnd))
                );
            }
        }

        timeline.play();

        sidebarCollapsed = !sidebarCollapsed;
        collapseIcon.setIconLiteral(sidebarCollapsed ? "fas-align-right" : "fas-bars");

        for (javafx.scene.Node node : sidebar.getChildren()) {
            if (node instanceof Button btn && btn != collapseButton) {
                btn.setContentDisplay(sidebarCollapsed ? javafx.scene.control.ContentDisplay.GRAPHIC_ONLY : javafx.scene.control.ContentDisplay.LEFT);
            }
        }
    }
    
}
