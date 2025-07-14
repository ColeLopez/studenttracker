package com.cole.Service;

import com.cole.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ModuleService {
    private static final Logger logger = LoggerFactory.getLogger(ModuleService.class);

    /**
     * Adds a new module to the database.
     * @param code Module code (unique identifier).
     * @param name Module name.
     * @param passMark Pass mark for the module.
     * @return true if added successfully, false otherwise.
     */
    public boolean addModule(String code, String name, int passMark) {
        String sql = "INSERT INTO modules (module_code, name, pass_result) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.setInt(3, passMark);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.error("Failed to add module", e);
            return false;
        }
    }
}
