package com.cole.controller;

import com.cole.model.Module;
import com.cole.model.SLP;
import com.cole.util.DBUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing SLPs and their linked modules.
 * Handles UI events and database operations for linking/unlinking modules.
 */
public class SLPModuleController {

    @FXML private ComboBox<SLP> slpComboBox;
    @FXML private TableView<Module> moduleTable;
    @FXML private TableColumn<Module, String> codeColumn;
    @FXML private TableColumn<Module, String> nameColumn;
    @FXML private TableColumn<Module, Number> passRateColumn;

    private final ObservableList<SLP> slps = FXCollections.observableArrayList();
    private final ObservableList<Module> linkedModules = FXCollections.observableArrayList();

    // SQL Queries as constants
    private static final String SELECT_ALL_SLPS = "SELECT * FROM slps";
    private static final String SELECT_MODULES_FOR_SLP =
            "SELECT m.module_id, m.module_code, m.name, m.pass_rate " +
            "FROM modules m JOIN slp_modules sm ON sm.module_id = m.module_id WHERE sm.slp_id = ?";
    private static final String SELECT_ALL_MODULES = "SELECT * FROM modules ORDER BY module_code";
    private static final String CHECK_MODULE_LINKED =
            "SELECT 1 FROM slp_modules WHERE slp_id = ? AND module_id = ?";
    private static final String INSERT_SLP_MODULE =
            "INSERT INTO slp_modules (slp_id, module_id) VALUES (?, ?)";
    private static final String DELETE_SLP_MODULE =
            "DELETE FROM slp_modules WHERE slp_id = ? AND module_id = ?";
    private static final String INSERT_MODULE =
            "INSERT INTO modules (module_code, name, pass_rate) VALUES (?, ?, ?)";

    /**
     * Initializes the controller and sets up UI bindings.
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
     */
    private void loadSLPs() {
        slps.clear();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SLPS)) {

            while (rs.next()) {
                slps.add(new SLP(
                        rs.getInt("slp_id"),
                        rs.getString("slp_code"),
                        rs.getString("name")
                ));
            }
            slpComboBox.setItems(slps);
        } catch (SQLException e) {
            showError("Failed to load SLPs", e.getMessage());
        }
    }

    /**
     * Loads all modules linked to the given SLP ID.
     * @param slpId SLP identifier
     */
    private void loadModulesForSLP(int slpId) {
        linkedModules.clear();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_MODULES_FOR_SLP)) {
            stmt.setInt(1, slpId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    linkedModules.add(new Module(
                            rs.getInt("module_id"),
                            rs.getString("module_code"),
                            rs.getString("name"),
                            rs.getInt("pass_rate")
                    ));
                }
            }
            moduleTable.setItems(linkedModules);
        } catch (SQLException e) {
            showError("Failed to load modules", e.getMessage());
        }
    }

    /**
     * Handles linking selected modules to the selected SLP.
     * Shows a dialog for multi-select and search.
     */
    @FXML
    private void handleAddModules() {
        SLP selectedSLP = slpComboBox.getSelectionModel().getSelectedItem();
        if (selectedSLP == null) {
            showError("No SLP Selected", "Please select an SLP first.");
            return;
        }

        ObservableList<Module> allModules = FXCollections.observableArrayList();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_MODULES)) {

            while (rs.next()) {
                allModules.add(new Module(
                        rs.getInt("module_id"),
                        rs.getString("module_code"),
                        rs.getString("name"),
                        rs.getInt("pass_rate")
                ));
            }
        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
            return;
        }

        // Show module selection dialog
        Optional<List<Module>> result = showModuleSelectionDialog(allModules);
        result.ifPresent(selectedModules -> {
            if (selectedModules.isEmpty()) {
                showError("No Selection", "No modules were selected.");
                return;
            }
            linkModulesToSLP(selectedSLP, selectedModules);
        });
    }

    /**
     * Links the given modules to the specified SLP, checking for duplicates.
     * @param slp SLP to link modules to
     * @param modules List of modules to link
     */
    private void linkModulesToSLP(SLP slp, List<Module> modules) {
        StringBuilder alreadyLinkedModules = new StringBuilder();
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement checkStmt = conn.prepareStatement(CHECK_MODULE_LINKED);
                 PreparedStatement insertStmt = conn.prepareStatement(INSERT_SLP_MODULE)) {

                for (Module module : modules) {
                    checkStmt.setInt(1, slp.getId());
                    checkStmt.setInt(2, module.getId());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            alreadyLinkedModules.append(module.getModuleCode()).append(", ");
                        } else {
                            insertStmt.setInt(1, slp.getId());
                            insertStmt.setInt(2, module.getId());
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
            conn.commit();

            if (alreadyLinkedModules.length() > 0) {
                String msgModules = alreadyLinkedModules.substring(0, alreadyLinkedModules.length() - 2);
                showError("Modules Already Linked",
                        "The following modules are already linked to this SLP and were not linked:\n" + msgModules);
            }
            loadModulesForSLP(slp.getId());
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    showError("Rollback Error", rollbackEx.getMessage());
                }
            }
            showError("Database Error", e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // Optionally log error: System.err.println("Error closing connection: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Shows a dialog for selecting multiple modules with a search filter.
     * @param allModules List of all modules
     * @return Optional list of selected modules
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
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(DELETE_SLP_MODULE)) {
                stmt.setInt(1, selectedSLP.getId());
                stmt.setInt(2, selectedModule.getId());
                stmt.executeUpdate();

                loadModulesForSLP(selectedSLP.getId());
            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleNewModule() {
        Optional<Module> result = showNewModuleDialog();
        result.ifPresent(module -> {
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(INSERT_MODULE)) {

                stmt.setString(1, module.getModuleCode());
                stmt.setString(2, module.getName());
                stmt.setInt(3, module.getPassRate());
                stmt.executeUpdate();

                loadSLPs(); // Refresh the SLP list after insertion

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Module Added");
                success.setHeaderText(null);
                success.setContentText("Module was added successfully.");
                success.showAndWait();

            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        });
    }

    /**
     * Shows a dialog for entering new module details.
     * @return Optional Module if input is valid
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
     * @param title Dialog title
     * @param message Error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        // Optionally log error: System.err.println(title + ": " + message);
        // Use a logging framework for production error tracking
        // org.slf4j.LoggerFactory.getLogger(SLPModuleController.class).error("{}: {}", title, message);
    }
}
