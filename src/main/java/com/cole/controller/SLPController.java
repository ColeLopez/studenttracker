package com.cole.controller;

import com.cole.Service.SLPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.concurrent.Task;

import com.cole.model.SLP;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

public class SLPController {
    private static final Logger logger = LoggerFactory.getLogger(SLPController.class);
    private final SLPService slpService = new SLPService();
    @FXML private TableView<SLP> slpTable;
    @FXML private TableColumn<SLP, String> codeColumn;
    @FXML private TableColumn<SLP, String> nameColumn;

    private final ObservableList<SLP> slpList = FXCollections.observableArrayList();

    /**
     * Initializes the controller and sets up UI bindings for the SLP table.
     * This method is called automatically by the JavaFX framework after FXML loading.
     */
    @FXML
    private void initialize() {
        codeColumn.setCellValueFactory(cellData -> cellData.getValue().slpCodeProperty());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        slpTable.setItems(slpList);
        loadSLPs();
    }

    /**
     * Loads all SLPs from the database into the TableView.
     * This method runs asynchronously and updates the TableView on success.
     */
    public void loadSLPs() {
        Task<ObservableList<SLP>> task = new Task<>() {
            @Override
            protected ObservableList<SLP> call() {
                return FXCollections.observableArrayList(slpService.getAllSLPs());
            }
        };
        task.setOnSucceeded(e -> {
            slpList.clear();
            slpList.addAll(task.getValue());
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load SLPs", task.getException());
            showError("Database Error", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    /**
     * Handles adding a new SLP via a custom dialog.
     * Invoked by the UI when the user clicks the "Add SLP" button.
     */
    @FXML
    private void handleAddSLP() {
        // Custom dialog for SLP code and name
        javafx.scene.control.Dialog<SLP> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Add New SLP");
        dialog.setHeaderText("Enter SLP details:");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        javafx.scene.control.TextField codeField = new javafx.scene.control.TextField();
        codeField.setPromptText("SLP Code");

        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        nameField.setPromptText("SLP Name");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new javafx.scene.control.Label("SLP Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new javafx.scene.control.Label("SLP Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String code = codeField.getText().trim();
                String name = nameField.getText().trim();
                if (code.isEmpty() || name.isEmpty()) {
                    showError("Invalid Input", "Both SLP Code and Name are required.");
                    return null;
                }
                return new SLP(0, code, name);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(slp -> {
            if (slp == null) return;
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    if (slpService.isDuplicateSLPCode(slp.getSlpCode(), null)) {
                        return false;
                    }
                    return slpService.addSLP(slp.getSlpCode(), slp.getName());
                }
            };
            task.setOnSucceeded(e -> {
                if (task.getValue()) {
                    showInfo("SLP Added", "The SLP was successfully added.");
                    loadSLPs();
                } else {
                    showError("Duplicate SLP Code", "An SLP with this code already exists or could not be added.");
                }
            });
            task.setOnFailed(e -> {
                logger.error("Failed to add SLP", task.getException());
                showError("Database Error", task.getException().getMessage());
            });
            new Thread(task).start();
        });
    }

    /**
     * Handles editing the selected SLP.
     * Invoked by the UI when the user clicks the "Edit SLP" button.
     * Opens a dialog to edit the SLP code and name.
     */
    @FXML
    private void handleEditSLP() {
        SLP selected = slpTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select an SLP to edit.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getSlpCode() + ", " + selected.getName());
        dialog.setTitle("Edit SLP");
        dialog.setHeaderText("Edit SLP Code and Name (comma separated):");
        dialog.setContentText("Format: MSW2021, Word Processing");

        dialog.showAndWait().ifPresent(input -> {
            String[] parts = input.split(",", 2);
            String newCode = parts.length > 0 ? parts[0].trim() : "";
            String newName = parts.length > 1 ? parts[1].trim() : "";

            if (newCode.isEmpty() || newName.isEmpty()) {
                showError("Invalid Format", "SLP Code and Name cannot be empty. Use: MSW2021, Word Processing");
                return;
            }
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    if (slpService.isDuplicateSLPCode(newCode, selected.getId())) {
                        return false;
                    }
                    return slpService.updateSLP(selected.getId(), newCode, newName);
                }
            };
            task.setOnSucceeded(e -> {
                if (task.getValue()) {
                    showInfo("SLP Updated", "The SLP was successfully updated.");
                    loadSLPs();
                } else {
                    showError("Duplicate SLP Code", "An SLP with this code already exists or could not be updated.");
                }
            });
            task.setOnFailed(e -> {
                logger.error("Failed to update SLP", task.getException());
                showError("Database Error", task.getException().getMessage());
            });
            new Thread(task).start();
        });
    }

    /**
     * Handles deleting the selected SLP after confirmation.
     * Invoked by the UI when the user clicks the "Delete SLP" button.
     * Prompts for confirmation before deleting from the database.
     */
    @FXML
    private void handleDeleteSLP() {
        SLP selected = slpTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection", "Please select an SLP to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Are you sure you want to delete this SLP?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return slpService.deleteSLP(selected.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    if (task.getValue()) {
                        showInfo("SLP Deleted", "The SLP was successfully deleted.");
                        loadSLPs();
                    } else {
                        showError("Delete Failed", "No SLP was deleted. Please try again.");
                    }
                });
                task.setOnFailed(e -> {
                    logger.error("Failed to delete SLP", task.getException());
                    showError("Database Error", task.getException().getMessage());
                });
                new Thread(task).start();
            }
        });
    }

    /**
     * Shows an information dialog with the given title and message.
     * @param title Dialog title (short description)
     * @param message Information message (detailed)
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
     * Syncs modules to students for the selected SLP.
     * Invoked by the UI when the user clicks the "Sync Modules" button.
     * This method directly calls the service layer to perform the sync operation.
     */
    @FXML
    private void handleSyncModules() {
        SLP selectedSLP = slpTable.getSelectionModel().getSelectedItem();
        if (selectedSLP == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select an SLP to sync.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        com.cole.Service.SLPModuleService.syncModulesToStudents(selectedSLP.getId());
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Modules synced to all students in this SLP.", ButtonType.OK);
        alert.showAndWait();
    }
}
