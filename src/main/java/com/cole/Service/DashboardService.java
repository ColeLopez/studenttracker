package com.cole.Service;

import com.cole.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    public int getStudentCount() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("Failed to get student count", e);
        }
        return -1;
    }

    public int getGraduatedCount() {
        String sql = "SELECT COUNT(*) FROM students WHERE status = 'Graduated'";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("Failed to get graduated count", e);
        }
        return -1;
    }
}
