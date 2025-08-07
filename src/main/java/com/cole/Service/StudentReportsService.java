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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.Color;

/***
 * Service for managing student reports.
 * Provides methods to retrieve student data, generate reports, and export PDFs.
 */
public class StudentReportsService {
    private static final Logger logger = LoggerFactory.getLogger(StudentReportsService.class);

    /**
     * Retrieves a student by their student number.
     *
     * @param studentNumber The student's unique number
     * @return Student object or null if not found
     */
    public Student getStudentByNumber(String studentNumber) {
        String sql = "SELECT s.student_id, s.student_number, s.first_name, s.last_name, s.email, s.phone, sl.name AS slp_name, s.status, s.enrollment_date " +
                     "FROM students s " +
                     "LEFT JOIN slps sl ON s.current_slp_id = sl.slp_id " +
                     "WHERE LOWER(s.student_number) = LOWER(?)";
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
                        rs.getString("enrollment_date")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all modules for a student, excluding those marked as 'replaced'.
     *
     * @param studentId The student's ID
     * @return List of StudentModule objects
     */
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

    /**
     * Retrieves notes for a student.
     *
     * @param studentId The student's ID
     * @return List of Note objects
     */
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

    /**
     * Retrieves follow-ups for a student.
     *
     * @param studentId The student's ID
     * @return List of FollowUp objects
     */
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

    /**
     * Retrieves all data needed for a student's report.
     *
     * @param studentNumber The student's unique number
     * @return StudentReportData object containing student info, modules, notes, and follow-ups
     */
    public StudentReportData getStudentReportData(String studentNumber) {
        Student student = getStudentByNumber(studentNumber);
        if (student == null) return null;
        List<StudentModule> modules = getStudentModules(student.getId());
        List<Note> notes = getStudentNotes(student.getId());
        List<FollowUp> followUps = getStudentFollowUps(student.getId());
        return new StudentReportData(student, modules, notes, followUps);
    }

    /**
     * Exports a student's summary report to a PDF file.
     *
     * @param reportData The StudentReportData object containing all necessary data
     * @param file The file to save the PDF to
     */
    public void exportStudentSummaryPdf(StudentReportData reportData, File file) {
        if (reportData == null || reportData.getStudent() == null) {
            logger.error("No student data provided for PDF export.");
            throw new IllegalArgumentException("No student data provided.");
        }
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            int leftMargin = 50;
            int rightMargin = 550;
            int pageWidth = (int) PDRectangle.A4.getWidth();

            // Draw a border around the page (matching logo color)
            Color borderColor = new Color(166, 0, 38); // Use your logo's main color
            contentStream.setStrokingColor(borderColor);
            contentStream.setLineWidth(3); // Border thickness, adjust as needed
            float borderPadding = 10; // Distance from the edge, adjust as needed
            contentStream.addRect(
                borderPadding,
                borderPadding,
                page.getMediaBox().getWidth() - 2 * borderPadding,
                page.getMediaBox().getHeight() - 2 * borderPadding
            );
            contentStream.stroke();
            contentStream.setStrokingColor(Color.BLACK); // Reset to default

            // Draw logo at top center
            PDImageXObject logo = PDImageXObject.createFromFile("src/main/resources/logo/Boston-Logo-removebg-preview.png", document);
            float logoWidth = 120;
            float logoHeight = 60;
            float logoX = (page.getMediaBox().getWidth() - logoWidth) / 2;
            float logoY = page.getMediaBox().getHeight() - logoHeight - 20;
            contentStream.drawImage(logo, logoX, logoY, logoWidth, logoHeight);

            // Set y below the logo
            int y = (int)(logoY - 20);

            // Title
            String title = "Student Summary Report";
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 22);
            float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 22;
            float titleX = (pageWidth - titleWidth) / 2;
            contentStream.beginText();
            contentStream.newLineAtOffset(titleX, y);
            contentStream.showText(title);
            contentStream.endText();

            y -= 30;

            // Section: Student Info
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, y);
            contentStream.showText("Student Information");
            contentStream.endText();
            y -= 8;
            // Draw line under section header
            contentStream.setStrokingColor(new Color(120, 120, 120));
            contentStream.moveTo(leftMargin, y);
            contentStream.lineTo(rightMargin, y);
            contentStream.stroke();
            y -= 18;

            Student s = reportData.getStudent();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            String[][] info = {
                {"Name", s.getFirstName() + " " + s.getLastName()},
                {"Student Number", s.getStudentNumber()},
                {"Email", s.getEmail()},
                {"Phone", s.getPhoneNumber()},
                {"SLP", s.getSlp()},
                {"Status", s.getStatus()},
                {"Enrollment Date", s.getEnrollmentDate()}
            };
            for (String[] pair : info) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, y);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.showText(pair[0] + ": ");
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.showText(pair[1]);
                contentStream.endText();
                y -= 16;
            }
            y -= 10;

            // Section: Modules Table
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, y);
            contentStream.showText("Modules and Marks");
            contentStream.endText();
            y -= 8;
            contentStream.setStrokingColor(new Color(120, 120, 120));
            contentStream.moveTo(leftMargin, y);
            contentStream.lineTo(rightMargin, y);
            contentStream.stroke();
            y -= 18;

            // Table column positions (Name column is now shorter)
            float[] colX = {leftMargin, leftMargin + 60, leftMargin + 180, leftMargin + 260, leftMargin + 340, leftMargin + 430};
            String[] headers = {"Code", "Name", "Formative", "Summative", "Supplementary", "Pass Req"};

            // Header background
            contentStream.setNonStrokingColor(new Color(230, 230, 250));
            contentStream.addRect(leftMargin - 2, y - 4, rightMargin - leftMargin + 4, 18);
            contentStream.fill();
            contentStream.setNonStrokingColor(Color.BLACK);

            // Header text
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            for (int i = 0; i < headers.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(colX[i], y + 2);
                contentStream.showText(headers[i]);
                contentStream.endText();
            }
            y -= 18;

            // Table rows
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            for (StudentModule m : reportData.getModules()) {
                if (y < 60) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = 770;
                }
                String[] row = {
                    m.getModuleCode(),
                    m.getModuleName(),
                    String.format("%.2f%%", m.getFormative()),
                    String.format("%.2f%%", m.getSummative()),
                    String.format("%.2f%%", m.getSupplementary()),
                    m.getPassRate() + "%"
                };

                float nameColWidth = colX[2] - colX[1] - 5;
                List<String> wrappedName = wrapText(row[1], PDType1Font.HELVETICA, 11, nameColWidth);
                int maxLines = Math.max(1, wrappedName.size());

                // Draw all lines for this module row
                for (int lineIdx = 0; lineIdx < maxLines; lineIdx++) {
                    // Code column
                    contentStream.beginText();
                    contentStream.newLineAtOffset(colX[0], y - (lineIdx * 12));
                    contentStream.showText(lineIdx == 0 ? row[0] : "");
                    contentStream.endText();

                    // Name column (wrapped)
                    contentStream.beginText();
                    contentStream.newLineAtOffset(colX[1], y - (lineIdx * 12));
                    contentStream.showText(wrappedName.get(lineIdx));
                    contentStream.endText();

                    // Formative, Summative, Supplementary, Pass Req columns (only on first line)
                    if (lineIdx == 0) {
                        for (int i = 2; i < row.length; i++) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(colX[i], y);
                            if (i >= 2 && i <= 4) {
                                double[] marks = {m.getFormative(), m.getSummative(), m.getSupplementary()};
                                int passRate = m.getPassRate();
                                if (marks[i - 2] < passRate) {
                                    contentStream.setNonStrokingColor(new Color(220, 0, 0)); // red for fail
                                } else {
                                    contentStream.setNonStrokingColor(Color.BLACK); // black for pass
                                }
                            } else {
                                contentStream.setNonStrokingColor(Color.BLACK);
                            }
                            contentStream.showText(row[i]);
                            contentStream.endText();
                        }
                    }
                    contentStream.setNonStrokingColor(Color.BLACK); // reset color
                }

                // Now update y for the next row
                y -= maxLines * 12;
            }
            y -= 10;

            // Section: Notes
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, y);
            contentStream.showText("Notes");
            contentStream.endText();
            y -= 8;
            contentStream.setStrokingColor(new Color(120, 120, 120));
            contentStream.moveTo(leftMargin, y);
            contentStream.lineTo(rightMargin, y);
            contentStream.stroke();
            y -= 18;

            contentStream.setFont(PDType1Font.HELVETICA, 11);
            float notesMaxWidth = rightMargin - leftMargin - 20;
            for (Note n : reportData.getNotes()) {
                if (y < 60) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = 770;
                }
                String noteText = "\u2022 " + n.getDateAdded() + ": " + n.getText();
                List<String> wrappedNote = wrapText(noteText, PDType1Font.HELVETICA, 11, notesMaxWidth);
                for (String line : wrappedNote) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 10, y);
                    contentStream.showText(line);
                    contentStream.endText();
                    y -= 14;
                }
            }
            y -= 10;

            // Section: Follow-Ups
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, y);
            contentStream.showText("Follow-Ups");
            contentStream.endText();
            y -= 8;
            contentStream.setStrokingColor(new Color(120, 120, 120));
            contentStream.moveTo(leftMargin, y);
            contentStream.lineTo(rightMargin, y);
            contentStream.stroke();
            y -= 18;

            contentStream.setFont(PDType1Font.HELVETICA, 11);
            float followUpMaxWidth = rightMargin - leftMargin - 20;
            for (FollowUp f : reportData.getFollowUps()) {
                if (y < 60) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = 770;
                }
                String followText = "\u2022 " + f.getDueDate() + ": " + f.getDescription() +
                    " [" + (f.isCompleted() ? "Done" : "Pending") + "]";
                List<String> wrappedFollow = wrapText(followText, PDType1Font.HELVETICA, 11, followUpMaxWidth);
                for (String line : wrappedFollow) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(leftMargin + 10, y);
                    contentStream.showText(line);
                    contentStream.endText();
                    y -= 14;
                }
            }

            // Footer
            y = 40;
            contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
            contentStream.setNonStrokingColor(new Color(120, 120, 120));
            contentStream.beginText();
            contentStream.newLineAtOffset(leftMargin, y);
            contentStream.showText("Generated on: " + java.time.LocalDate.now());
            contentStream.endText();
            contentStream.setNonStrokingColor(Color.BLACK);

            contentStream.close();

            // Add page numbers as footers
            int totalPages = document.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage currentPage = document.getPage(pageIndex);
                try (PDPageContentStream footerStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    String pageNumText = "Page " + (pageIndex + 1) + " of " + totalPages;
                    footerStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                    footerStream.setNonStrokingColor(new Color(120, 120, 120));
                    float stringWidth = PDType1Font.HELVETICA_OBLIQUE.getStringWidth(pageNumText) / 1000 * 9;
                    float x = (currentPage.getMediaBox().getWidth() - stringWidth) / 2;
                    float yFooter = 25;
                    footerStream.beginText();
                    footerStream.newLineAtOffset(x, yFooter);
                    footerStream.showText(pageNumText);
                    footerStream.endText();
                }
            }

            document.save(file);
            logger.info("Student summary PDF exported successfully to {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to export Student Summary PDF", e);
            throw new RuntimeException("Failed to export Student Summary PDF: " + e.getMessage(), e);
        }

    }

    /**
     * Wraps text to fit within a specified width using the given font and font size.
     *
     * @param text The text to wrap
     * @param font The PDType1Font to use for measuring text width
     * @param fontSize The size of the font
     * @param maxWidth The maximum width allowed for the text
     * @return List of lines that fit within the specified width
     * @throws Exception if an error occurs during text wrapping
     */
    private List<String> wrapText(String text, PDType1Font font, int fontSize, float maxWidth) throws Exception {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            float size = font.getStringWidth(testLine) / 1000 * fontSize;
            if (size > maxWidth) {
                if (line.length() > 0) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    lines.add(word);
                    line = new StringBuilder();
                }
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
}
