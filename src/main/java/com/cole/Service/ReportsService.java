package com.cole.Service;

import com.cole.model.Student;
import com.cole.model.StudentModule;
import com.cole.model.StudentReportData;
import com.cole.controller.VirtualRecordCardController.Note;
import com.cole.controller.VirtualRecordCardController.FollowUp;
import com.cole.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class ReportsService {

    public Student getStudentByNumber(String studentNumber) {
        String sql = "SELECT s.student_id, s.student_number, s.first_name, s.last_name, s.email, s.phone, sl.name AS slp_name, s.status, s.enrollment_date " +
                     "FROM students s " +
                     "LEFT JOIN slps sl ON s.current_slp_id = sl.slp_id " +
                     "WHERE s.student_number = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                        rs.getInt("student_id"),
                        rs.getString("student_number"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("slp_name"),
                        rs.getString("status"),
                        rs.getString("enrollment_date") // <-- Add this
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<StudentModule> getStudentModules(int studentId) {
        List<StudentModule> modules = new ArrayList<>();
        String sql = "SELECT sm.*, m.pass_rate FROM student_modules sm JOIN modules m ON sm.module_id = m.module_id WHERE sm.student_id = ? AND (sm.status IS NULL OR sm.status != 'replaced')";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StudentModule sm = new StudentModule(
                        rs.getInt("student_id"),
                        rs.getInt("module_id"),
                        rs.getString("module_code"),
                        rs.getString("module_name"),
                        rs.getBoolean("received_book"),
                        rs.getObject("formative") != null ? rs.getDouble("formative") : 0.0,
                        rs.getObject("summative") != null ? rs.getDouble("summative") : 0.0,
                        rs.getObject("supplementary") != null ? rs.getDouble("supplementary") : 0.0
                    );
                    // Set passRate if setter exists
                    try {
                        int passRate = rs.getObject("pass_rate") != null ? rs.getInt("pass_rate") : 50;
                        sm.setPassRate(passRate);
                    } catch (Exception ignore) {}
                    modules.add(sm);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modules;
    }

    public List<Note> getStudentNotes(int studentId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT note_id, note_text, date_added FROM notes WHERE student_id = ? ORDER BY date_added DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(new Note(
                        rs.getInt("note_id"),
                        rs.getString("note_text"),
                        rs.getString("date_added")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notes;
    }

    public List<FollowUp> getStudentFollowUps(int studentId) {
        List<FollowUp> followUps = new ArrayList<>();
        String sql = "SELECT followup_id, due_date, description, completed FROM follow_ups WHERE student_id = ? ORDER BY due_date ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    followUps.add(new FollowUp(
                        rs.getInt("followup_id"),
                        rs.getString("due_date"),
                        rs.getString("description"),
                        rs.getInt("completed") == 1
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return followUps;
    }

    public StudentReportData getStudentReportData(String studentNumber) {
        Student student = getStudentByNumber(studentNumber);
        if (student == null) return null;
        List<StudentModule> modules = getStudentModules(student.getId());
        List<Note> notes = getStudentNotes(student.getId());
        List<FollowUp> followUps = getStudentFollowUps(student.getId());
        return new StudentReportData(student, modules, notes, followUps);
    }
}
