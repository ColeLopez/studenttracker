package com.cole.controller;

import com.cole.model.Module;
import com.cole.model.SLP;
import com.cole.Service.SLPModuleService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

// ...existing code...
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing Student Learning Pathways (SLPs) and their linked modules.
 * <p>
 * Handles UI events and database operations for linking/unlinking modules to SLPs.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Loading SLPs and modules from the database</li>
 *   <li>Linking and unlinking modules to SLPs</li>
 *   <li>Handling user interactions for module management</li>
 *   <li>Providing dialogs for module selection and creation</li>
 * </ul>
 * <p>
 * All database operations are performed asynchronously using JavaFX Tasks.
 */
public class SLPModuleController {
    private static final Logger logger = LoggerFactory.getLogger(SLPModuleController.class);
    private final SLPModuleService slpModuleService = new SLPModuleService();

    @FXML private ComboBox<SLP> slpComboBox;
    @FXML private TableView<Module> moduleTable;
    @FXML private TableColumn<Module, String> codeColumn;
    @FXML private TableColumn<Module, String> nameColumn;
    @FXML private TableColumn<Module, Number> passRateColumn;

    private final ObservableList<SLP> slps = FXCollections.observableArrayList();
    private final ObservableList<Module> linkedModules = FXCollections.observableArrayList();

    // ...existing code...

    /**
     * Initializes the controller and sets up UI bindings for the module table and SLP ComboBox.
     * This method is called automatically by the JavaFX framework after FXML loading.
     */
    @FXML
    public void initialize() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("moduleCode"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        passRateColumn.setCellValueFactory(new PropertyValueFactory<>("passRate"));

        loadSLPs();

        slpComboBox.setOnAction(event -> {
            SLP selected = slpComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                loadModulesForSLP(selected.getId());
            }
        });
    }

    /**
     * Loads all SLPs from the database into the ComboBox.
     * This method runs asynchronously and updates the ComboBox on success.
     */
    private void loadSLPs() {
        Task<ObservableList<SLP>> task = new Task<>() {
            @Override
            protected ObservableList<SLP> call() {
                return FXCollections.observableArrayList(slpModuleService.getAllSLPs());
            }
        };
        task.setOnSucceeded(e -> {
            slps.clear();
            slps.addAll(task.getValue());
            slpComboBox.setItems(slps);
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load SLPs", task.getException());
            showError("Failed to load SLPs", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    /**
     * Loads all modules linked to the given SLP ID.
     * @param slpId SLP identifier (database primary key)
     */
    private void loadModulesForSLP(int slpId) {
        Task<ObservableList<Module>> task = new Task<>() {
            @Override
            protected ObservableList<Module> call() {
                return FXCollections.observableArrayList(slpModuleService.getModulesForSLP(slpId));
            }
        };
        task.setOnSucceeded(e -> {
            linkedModules.clear();
            linkedModules.addAll(task.getValue());
            moduleTable.setItems(linkedModules);
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load modules for SLP {}", slpId, task.getException());
            showError("Failed to load modules", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    /**
     * Handles linking selected modules to the selected SLP.
     * Shows a dialog for multi-select and search.
     * Invoked by the UI when the user clicks the "Add Modules" button.
     */
    @FXML
    private void handleAddModules() {
        SLP selectedSLP = slpComboBox.getSelectionModel().getSelectedItem();
        if (selectedSLP == null) {
            showError("No SLP Selected", "Please select an SLP first.");
            return;
        }

        Task<ObservableList<Module>> task = new Task<>() {
            @Override
            protected ObservableList<Module> call() {
                return FXCollections.observableArrayList(slpModuleService.getAllModules());
            }
        };
        task.setOnSucceeded(e -> {
            Optional<List<Module>> result = showModuleSelectionDialog(task.getValue());
            result.ifPresent(selectedModules -> {
                if (selectedModules.isEmpty()) {
                    showError("No Selection", "No modules were selected.");
                    return;
                }
                linkModulesToSLP(selectedSLP, selectedModules);
            });
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load all modules", task.getException());
            showError("Database Error", task.getException().getMessage());
        });
        new Thread(task).start();

    }

    /**
     * Links the given modules to the specified SLP, checking for duplicates.
     * @param slp SLP to link modules to (must not be null)
     * @param modules List of modules to link (must not be null or empty)
     */
    private void linkModulesToSLP(SLP slp, List<Module> modules) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                return slpModuleService.linkModulesToSLP(slp.getId(), modules);
            }
        };
        task.setOnSucceeded(e -> {
            List<String> alreadyLinked = task.getValue();
            if (!alreadyLinked.isEmpty()) {
                String msgModules = String.join(", ", alreadyLinked);
                showError("Modules Already Linked",
                        "The following modules are already linked to this SLP and were not linked:\n" + msgModules);
            }
            loadModulesForSLP(slp.getId());
        });
        task.setOnFailed(e -> {
            logger.error("Failed to link modules to SLP {}", slp.getId(), task.getException());
            showError("Database Error", task.getException().getMessage());
        });
        new Thread(task).start();
    }

    /**
     * Shows a dialog for selecting multiple modules with a search filter.
     * @param allModules List of all modules (ObservableList for JavaFX bindings)
     * @return Optional list of selected modules, or empty if cancelled
     */
    private Optional<List<Module>> showModuleSelectionDialog(ObservableList<Module> allModules) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search by code or name...");
        ListView<Module> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        FilteredList<Module> filteredModules = new FilteredList<>(allModules, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            final String filter = newVal.toLowerCase();
            filteredModules.setPredicate(module ->
                    module.getModuleCode().toLowerCase().contains(filter) ||
                    module.getName().toLowerCase().contains(filter)
            );
        });

        listView.setItems(filteredModules);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Module item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getModuleCode() + " - " + item.getName());
            }
        });

        VBox dialogContent = new VBox(10, searchField, listView);
        dialogContent.setPadding(new Insets(10));

        Dialog<List<Module>> dialog = new Dialog<>();
        dialog.setTitle("Select Modules to Link");
        dialog.getDialogPane().setContent(dialogContent);

        ButtonType linkButtonType = new ButtonType("Link Selected", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(linkButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == linkButtonType) {
                return new ArrayList<>(listView.getSelectionModel().getSelectedItems());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Handles removing the selected module from the selected SLP.
     * Invoked by the UI when the user clicks the "Remove Module" button.
     * Prompts for confirmation before unlinking.
     */
    @FXML
    private void handleRemoveModule() {
        SLP selectedSLP = slpComboBox.getSelectionModel().getSelectedItem();
        Module selectedModule = moduleTable.getSelectionModel().getSelectedItem();

        if (selectedSLP == null || selectedModule == null) {
            showError("Missing Selection", "Please select both an SLP and a module to unlink.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unlink Module");
        confirm.setHeaderText("Are you sure you want to unlink this module from the selected SLP?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return slpModuleService.removeModuleFromSLP(selectedSLP.getId(), selectedModule.getId());
                }
            };
            task.setOnSucceeded(e2 -> {
                if (task.getValue()) {
                    loadModulesForSLP(selectedSLP.getId());
                } else {
                    showError("Database Error", "Failed to remove module from SLP.");
                }
            });
            task.setOnFailed(e2 -> {
                logger.error("Failed to remove module from SLP", task.getException());
                showError("Database Error", task.getException().getMessage());
            });
            new Thread(task).start();
        }
    }

    @FXML
    private void handleNewModule() {
        Optional<Module> result = showNewModuleDialog();
        result.ifPresent(module -> {
            SLP previouslySelectedSLP = slpComboBox.getSelectionModel().getSelectedItem();
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return slpModuleService.addNewModule(module);
                }
            };
            task.setOnSucceeded(e -> {
                if (task.getValue()) {
                    // Refresh the SLP list after insertion
                    Task<ObservableList<SLP>> reloadTask = new Task<>() {
                        @Override
                        protected ObservableList<SLP> call() {
                            return FXCollections.observableArrayList(slpModuleService.getAllSLPs());
                        }
                    };
                    reloadTask.setOnSucceeded(ev -> {
                        slps.clear();
                        slps.addAll(reloadTask.getValue());
                        slpComboBox.setItems(slps);
                        // Restore previous selection if possible
                        if (previouslySelectedSLP != null) {
                            for (SLP slp : slps) {
                                if (slp.getId() == previouslySelectedSLP.getId()) {
                                    slpComboBox.getSelectionModel().select(slp);
                                    break;
                                }
                            }
                        }
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Module Added");
                        successAlert.setHeaderText(null);
                        successAlert.setContentText("Module was added successfully.");
                        successAlert.showAndWait();
                    });
                    reloadTask.setOnFailed(ev -> {
                        logger.error("Failed to reload SLPs after module add", reloadTask.getException());
                        showError("Failed to reload SLPs", reloadTask.getException().getMessage());
                    });
                    new Thread(reloadTask).start();
                } else {
                    showError("Database Error", "Failed to add new module.");
                }
            });
            task.setOnFailed(e -> {
                logger.error("Failed to add new module", task.getException());
                showError("Database Error", task.getException().getMessage());
            });
            new Thread(task).start();
        });
    }

    /**
     * Shows a dialog for entering new module details.
     * @return Optional Module if input is valid, or empty if cancelled/invalid
     */
    private Optional<Module> showNewModuleDialog() {
        Dialog<Module> dialog = new Dialog<>();
        dialog.setTitle("Add New Module");
        dialog.setHeaderText("Enter module details:");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField codeField = new TextField();
        codeField.setPromptText("Module Code");

        TextField nameField = new TextField();
        nameField.setPromptText("Module Name");

        TextField passRateField = new TextField();
        passRateField.setPromptText("Pass Rate (e.g. 50)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Module Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Module Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Pass Rate:"), 0, 2);
        grid.add(passRateField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(new javafx.util.Callback<ButtonType, Module>() {
            @Override
            public Module call(ButtonType dialogButton) {
                if (dialogButton == addButtonType) {
                    String code = codeField.getText().trim();
                    String name = nameField.getText().trim();
                    String passRateStr = passRateField.getText().trim();
                    if (code.isEmpty() || name.isEmpty() || passRateStr.isEmpty()) {
                        showError("Invalid Input", "All fields are required.");
                        return null;
                    }
                    try {
                        int passRate = Integer.parseInt(passRateStr);
                        if (passRate < 0 || passRate > 100) {
                            showError("Invalid Input", "Pass rate must be between 0 and 100.");
                            return null;
                        }
                        return new Module(0, code, name, passRate);
                    } catch (NumberFormatException e) {
                        showError("Invalid Input", "Pass rate must be a number.");
                        return null;
                    }
                }
                return null;
            }
        });

        return dialog.showAndWait();
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
        logger.error("{}: {}", title, message);
    }
}
