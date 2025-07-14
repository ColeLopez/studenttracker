package com.cole.Service;

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

public class SLPService {
    private static final Logger logger = LoggerFactory.getLogger(SLPService.class);

    public List<SLP> getAllSLPs() {
        List<SLP> slps = new ArrayList<>();
        String sql = "SELECT slp_id, slp_code, name FROM slps";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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

    public boolean addSLP(String code, String name) {
        String sql = "INSERT INTO slps (slp_code, name) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Failed to add SLP", e);
            return false;
        }
    }

    public boolean updateSLP(int id, String code, String name) {
        String sql = "UPDATE slps SET slp_code = ?, name = ? WHERE slp_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.setInt(3, id);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            logger.error("Failed to update SLP", e);
            return false;
        }
    }

    public boolean deleteSLP(int id) {
        String sql = "DELETE FROM slps WHERE slp_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Failed to delete SLP", e);
            return false;
        }
    }

    public boolean isDuplicateSLPCode(String slpCode, Integer excludeId) {
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
            logger.error("Error checking for duplicate SLP code", e);
        }
        return false;
    }
}
