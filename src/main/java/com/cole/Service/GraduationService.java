package com.cole.Service;

import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.cole.util.DBUtil;

public class GraduationService {

    private static final Logger logger = Logger.getLogger(GraduationService.class.getName());

    /**
     * Checks if students have passed all modules and updates their graduation flags accordingly.
     * This method connects to the database, retrieves student data, checks module pass status,
     * and updates the graduation flags in the 'students_to_graduate' table.
     */
    public void checkAndUpdateGraduationFlags() {
        try (Connection conn = DBUtil.getConnection()) {
            String studentQuery = "SELECT s.student_id, s.student_number, s.first_name, s.last_name, s.email, s.phone, slp.name AS slp_course " +
                                  "FROM students s JOIN slps slp ON s.current_slp_id = slp.slp_id";
            // NOTE: Ensure 'slps' is the correct table name in your database schema. If not, replace 'slps' with the correct table name.
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(studentQuery)) {
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String slpCourse = rs.getString("slp_course");
                    if (hasPassedAllModules(conn, studentId)) {
                        logger.info("Student " + studentId + " passed all modules. Flagging for graduation.");
                        flagStudent(conn, rs, slpCourse);
                    } else {
                        logger.info("Student " + studentId + " has not passed all modules. Removing graduation flag if exists.");
                        unflagStudent(conn, studentId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking/updating graduation flags", e);
        }
    }

    /**
     * Checks if a student has passed all their modules.
     * @param conn the database connection
     * @param studentId the ID of the student to check
     * @return true if the student has passed all modules, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private boolean hasPassedAllModules(Connection conn, int studentId) throws SQLException {
       String query = "SELECT sm.module_id, m.name AS module_name, sm.formative, sm.summative, sm.supplementary, m.pass_rate " +
                       "FROM student_modules sm JOIN modules m ON sm.module_id = m.module_id " +
                       "WHERE sm.student_id = ?";
        boolean hasModules = false;
        boolean allPassed = true;
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasModules = true;
                    String moduleName = rs.getString("module_name");
                    int passRate = rs.getInt("pass_rate");
                    int formative = rs.getInt("formative");
                    int summative = rs.getInt("summative");
                    int supplementary = rs.getInt("supplementary");

                    boolean formativePassed = formative >= passRate;
                    boolean summativePassed = summative >= passRate;
                    boolean supplementaryPassed = supplementary >= passRate;

                    if (!formativePassed) {
                        logger.info("Student " + studentId + " did NOT pass formative for module: " + moduleName +
                                    " (Score: " + formative + ", Pass Rate: " + passRate + ")");
                        allPassed = false;
                    } else if (!summativePassed && !supplementaryPassed) {
                        logger.info("Student " + studentId + " did NOT pass summative or supplementary for module: " + moduleName +
                                    " (Summative: " + summative + ", Supplementary: " + supplementary + ", Pass Rate: " + passRate + ")");
                        allPassed = false;
                    } else {
                        logger.info("Student " + studentId + " PASSED module: " + moduleName +
                                    " (Formative: " + formative + ", Summative: " + summative + ", Supplementary: " + supplementary + ", Pass Rate: " + passRate + ")");
                    }
                }
            }
        }
        if (!hasModules) {
            logger.info("Student " + studentId + " is not enrolled in any modules.");
            return false;
        }
        return allPassed;
    }

    /**
     * Flags a student for graduation by inserting their details into the 'students_to_graduate' table.
     * @param conn the database connection
     * @param rs the ResultSet containing student data
     * @param slpCourse the SLP course of the student
     * @throws SQLException if a database access error occurs
     */
    private void flagStudent(Connection conn, ResultSet rs, String slpCourse) throws SQLException {
        int studentId = rs.getInt("student_id");
        String checkQuery = "SELECT 1 FROM students_to_graduate WHERE student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
            ps.setInt(1, studentId);
            try (ResultSet checkRs = ps.executeQuery()) {
                if (checkRs.next()) {
                    logger.info("Student " + studentId + " already flagged for graduation.");
                    return;
                }
            }
        }
        String insertQuery = "INSERT INTO students_to_graduate (student_id, student_number, first_name, last_name, slp_course, email, phone) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
            ps.setInt(1, studentId);
            ps.setString(2, rs.getString("student_number"));
            ps.setString(3, rs.getString("first_name"));
            ps.setString(4, rs.getString("last_name"));
            ps.setString(5, slpCourse);
            ps.setString(6, rs.getString("email"));
            ps.setString(7, rs.getString("phone"));
            ps.executeUpdate();
            logger.info("Student " + studentId + " flagged for graduation.");
        }
    }

    /**
     * Unflags a student for graduation by deleting their entry from the 'students_to_graduate' table.
     * @param conn the database connection
     * @param studentId the ID of the student to unflag
     * @throws SQLException if a database access error occurs
     */
    private void unflagStudent(Connection conn, int studentId) throws SQLException {
        String deleteQuery = "DELETE FROM students_to_graduate WHERE student_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
            ps.setInt(1, studentId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                logger.info("Student " + studentId + " unflagged for graduation.");
            }
        }
    }
}
