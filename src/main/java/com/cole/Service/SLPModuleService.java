package com.cole.Service;

import com.cole.model.Module;
import com.cole.model.SLP;
import com.cole.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SLPModuleService {
    private static final Logger logger = LoggerFactory.getLogger(SLPModuleService.class);

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

    public List<SLP> getAllSLPs() {
        List<SLP> slps = new ArrayList<>();
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
        } catch (SQLException e) {
            logger.error("Failed to load SLPs", e);
        }
        return slps;
    }

    public List<Module> getModulesForSLP(int slpId) {
        List<Module> modules = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_MODULES_FOR_SLP)) {
            stmt.setInt(1, slpId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    modules.add(new Module(
                            rs.getInt("module_id"),
                            rs.getString("module_code"),
                            rs.getString("name"),
                            rs.getInt("pass_rate")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load modules for SLP {}", slpId, e);
        }
        return modules;
    }

    public List<Module> getAllModules() {
        List<Module> modules = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_MODULES)) {
            while (rs.next()) {
                modules.add(new Module(
                        rs.getInt("module_id"),
                        rs.getString("module_code"),
                        rs.getString("name"),
                        rs.getInt("pass_rate")
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to load all modules", e);
        }
        return modules;
    }

    public List<String> linkModulesToSLP(int slpId, List<Module> modules) {
        List<String> alreadyLinked = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement checkStmt = conn.prepareStatement(CHECK_MODULE_LINKED);
                 PreparedStatement insertStmt = conn.prepareStatement(INSERT_SLP_MODULE)) {
                for (Module module : modules) {
                    checkStmt.setInt(1, slpId);
                    checkStmt.setInt(2, module.getId());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            alreadyLinked.add(module.getModuleCode());
                        } else {
                            insertStmt.setInt(1, slpId);
                            insertStmt.setInt(2, module.getId());
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Failed to link modules to SLP {}", slpId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Rollback error", rollbackEx);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    logger.error("Error closing connection", closeEx);
                }
            }
        }
        return alreadyLinked;
    }

    public boolean removeModuleFromSLP(int slpId, int moduleId) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SLP_MODULE)) {
            stmt.setInt(1, slpId);
            stmt.setInt(2, moduleId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Failed to remove module {} from SLP {}", moduleId, slpId, e);
            return false;
        }
    }

    public boolean addNewModule(Module module) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_MODULE)) {
            stmt.setString(1, module.getModuleCode());
            stmt.setString(2, module.getName());
            stmt.setInt(3, module.getPassRate());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Failed to add new module", e);
            return false;
        }
    }
}
