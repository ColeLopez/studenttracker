package com.cole.Service;

import com.cole.util.DBUtil;
import java.sql.*;

public class GraduationService {

    // Call this to check and update all graduation flags and statuses
    public void checkAndUpdateGraduationFlags() {
        try (Connection conn = DBUtil.getConnection()) {
            String studentQuery = "SELECT s.student_id, s.student_number, s.first_name, s.last_name, s.email, s.phone, slp.name AS slp_course " +
                                  "FROM students s JOIN slps slp ON s.current_slp_id = slp.slp_id";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(studentQuery)) {
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String slpCourse = rs.getString("slp_course");
                    if (hasPassedAllModules(conn, studentId)) {
                        flagStudent(conn, rs, slpCourse);
                    } else {
                        unflagStudent(conn, studentId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a student has passed all their modules.
     * Formative must pass, summative must pass, if summative failed then supplementary must pass.
     */
    private boolean hasPassedAllModules(Connection conn, int studentId) throws SQLException {
        String query = "SELECT sm.module_id, m.name AS module_name, sm.formative, sm.summative, sm.supplementary, m.pass_rate " +
                       "FROM student_modules sm JOIN modules m ON sm.module_id = m.module_id " +
                       "WHERE sm.student_id = ?";
        boolean hasModules = false;
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasModules = true;
                    int passRate = rs.getInt("pass_rate");
                    Double formative = rs.getObject("formative") != null ? rs.getDouble("formative") : null;
                    Double summative = rs.getObject("summative") != null ? rs.getDouble("summative") : null;
                    Double supplementary = rs.getObject("supplementary") != null ? rs.getDouble("supplementary") : null;

                    // If formative or summative is missing, not passed
                    if (formative == null || summative == null) {
                        return false;
                    }
                    // Formative must pass
                    if (formative < passRate) {
                        return false;
                    }
                    // Summative must pass, unless supplementary is a pass
                    if (summative < passRate) {
                        if (supplementary == null || supplementary < passRate) {
                            return false;
                        }
                    }
                }
            }
        }
        return hasModules;
    }

    // Insert into students_to_graduate and update status
    private void flagStudent(Connection conn, ResultSet rs, String slpCourse) throws SQLException {
        String studentNumber = rs.getString("student_number");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        int studentId = rs.getInt("student_id");
        if (studentNumber == null || firstName == null || lastName == null || slpCourse == null) {
            System.out.println("flagStudent: Required field is null, skipping for studentId=" + studentId);
            return;
        }
        System.out.println("flagStudent: Adding " + studentNumber + " to students_to_graduate");
        String insertSql = "INSERT OR IGNORE INTO students_to_graduate (student_id, student_number, first_name, last_name, slp_course, email, phone, transcript_requested) VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, studentId);
            ps.setString(2, studentNumber);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, slpCourse);
            ps.setString(6, email);
            ps.setString(7, phone);
            int rows = ps.executeUpdate();
            System.out.println("flagStudent: Inserted rows = " + rows + " for student_number=" + studentNumber);
            if (rows == 0) {
                System.out.println("flagStudent: Insert was ignored or failed for student_number=" + studentNumber);
            }
        }
        // Update student status to 'Graduated'
        try (PreparedStatement ps = conn.prepareStatement("UPDATE students SET status = 'Graduated' WHERE student_id = ?")) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        }
    }

    // Remove from students_to_graduate and revert status
    private void unflagStudent(Connection conn, int studentId) throws SQLException {
        String studentNumber = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT student_number FROM students WHERE student_id = ?")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) studentNumber = rs.getString("student_number");
            }
        }
        if (studentNumber != null) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM students_to_graduate WHERE student_number = ?")) {
                ps.setString(1, studentNumber);
                ps.executeUpdate();
            }
        }
        // Optionally set status back to 'Active'
        try (PreparedStatement ps = conn.prepareStatement("UPDATE students SET status = 'Active' WHERE student_id = ?")) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        }
    }
}
