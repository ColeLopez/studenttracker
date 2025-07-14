package com.cole.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.cole.model.SLP;
import com.cole.util.DBUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

public class SLPController {
    @FXML private TableView<SLP> slpTable;
    @FXML private TableColumn<SLP, String> codeColumn;
    @FXML private TableColumn<SLP, String> nameColumn;

    private final ObservableList<SLP> slpList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        codeColumn.setCellValueFactory(cellData -> cellData.getValue().slpCodeProperty());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        slpTable.setItems(slpList);
        loadSLPs();
    }

    public void loadSLPs() {
        slpList.clear();
        String sql = "SELECT slp_id, slp_code, name FROM slps";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                slpList.add(new SLP(
                    rs.getInt("slp_id"),
                    rs.getString("slp_code"),
                    rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    @FXML
    private void handleAddSLP() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add SLP");
        dialog.setHeaderText("Enter SLP Code and Name (comma separated):");
        dialog.setContentText("Format: MSW2021, Word Processing");

        dialog.showAndWait().ifPresent(input -> {
            String[] parts = input.split(",", 2);
            String code = parts.length > 0 ? parts[0].trim() : "";
            String name = parts.length > 1 ? parts[1].trim() : "";

            if (code.isEmpty() || name.isEmpty()) {
                showError("Invalid Input", "SLP Code and Name cannot be empty. Use: MSW2021, Word Processing");
                return;
            }
            if (isDuplicateSLPCode(code, null)) {
                showError("Duplicate SLP Code", "An SLP with this code already exists.");
                return;
            }
            String sql = "INSERT INTO slps (slp_code, name) VALUES (?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, code);
                stmt.setString(2, name);
                stmt.executeUpdate();
                showInfo("SLP Added", "The SLP was successfully added.");
                loadSLPs();
            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        });
    }

    private boolean isDuplicateSLPCode(String slpCode, Integer excludeId) {
        String sql = "SELECT 1 FROM slps WHERE slp_code = ?";
        if (excludeId != null) {
            sql += " AND slp_id <> ?";
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slpCode);
            if (excludeId != null) {
                stmt.setInt(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            showError("Database Error", "Error checking for duplicate SLP code: " + e.getMessage());
        }
        return false;
    }

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

            if (isDuplicateSLPCode(newCode, selected.getId())) {
                showError("Duplicate SLP Code", "An SLP with this code already exists.");
                return;
            }

            String sql = "UPDATE slps SET slp_code = ?, name = ? WHERE slp_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newCode);
                stmt.setString(2, newName);
                stmt.setInt(3, selected.getId());
                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    showInfo("SLP Updated", "The SLP was successfully updated.");
                    loadSLPs();
                } else {
                    showError("Update Failed", "No SLP was updated. Please try again.");
                }
            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        });
    }

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
                String sql = "DELETE FROM slps WHERE slp_id = ?";
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, selected.getId());
                    stmt.executeUpdate();
                    showInfo("SLP Deleted", "The SLP was successfully deleted.");
                    loadSLPs();
                } catch (SQLException e) {
                    showError("Database Error", e.getMessage());
                }
            }
        });
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
}
