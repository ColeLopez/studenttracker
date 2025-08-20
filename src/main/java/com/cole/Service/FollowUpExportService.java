package com.cole.Service;

import com.cole.util.DBUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FollowUpExportService {

    /**
     * Exports follow-up history to an Excel file based on the filter type.
     * @param file The file to export to.
     * @param filter The filter type: "completed", "upcoming", or "overdue".
     * @throws Exception If an error occurs during export.
     */
    public void exportFollowUpHistoryToExcel(File file, String filter) throws Exception {
        String[] columns = {
            "Student Number", "First Name", "Last Name", "Branch",
            "Due Date", "Description", "Completed"
        };

        // Adjust SQL query based on filter
        String sql = "SELECT s.student_number, s.first_name, s.last_name, s.branch, " +
                     "f.due_date, f.description, f.completed " +
                     "FROM follow_ups f " +
                     "JOIN students s ON f.student_id = s.student_id ";
        switch (filter) {
            case "completed":
                sql += "WHERE f.completed = 1";
                break;
            case "upcoming":
                sql += "WHERE f.due_date > CURRENT_DATE AND f.completed = 0";
                break;
            case "overdue":
                sql += "WHERE f.due_date < CURRENT_DATE AND f.completed = 0";
                break;
            default:
                throw new IllegalArgumentException("Invalid filter type: " + filter);
        }

        try (
            Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            XSSFWorkbook workbook = new XSSFWorkbook()
        ) {
            Sheet sheet = workbook.createSheet("Follow-Up History");

            // --- Title Row ---
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Follow-Up History Report: " + capitalizeFilter(filter));

            // Title style: Bold, larger font
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Arial");
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            titleCell.setCellStyle(titleStyle);

            // Merge title across all columns
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                0, 0, 0, columns.length - 1
            ));

            // --- Header Row ---
            Row header = sheet.createRow(1);
            CellStyle headerStyle = createBorderedStyle(workbook);
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data Rows ---
            CellStyle dataStyle = createBorderedStyle(workbook);
            Font dataFont = workbook.createFont();
            dataFont.setFontName("Arial");
            dataFont.setFontHeightInPoints((short) 10);
            dataStyle.setFont(dataFont);

            int rowNum = 2; // Start after title and header rows
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rs.getString("student_number"));
                row.createCell(1).setCellValue(rs.getString("first_name"));
                row.createCell(2).setCellValue(rs.getString("last_name"));
                row.createCell(3).setCellValue(rs.getString("branch"));
                row.createCell(4).setCellValue(rs.getString("due_date"));
                row.createCell(5).setCellValue(rs.getString("description"));
                row.createCell(6).setCellValue(rs.getInt("completed") == 1 ? "Yes" : "No");

                // Apply border style to all cells in the row
                for (int i = 0; i < columns.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Set all columns to width ~20 characters
            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Creates a cell style with borders.
     * @param workbook The workbook to create the style for.
     * @return A CellStyle with borders applied.
     */
    private CellStyle createBorderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Capitalizes the filter type for display in the title.
     * @param filter The filter type.
     * @return The capitalized filter type.
     */
    private String capitalizeFilter(String filter) {
        return filter.substring(0, 1).toUpperCase() + filter.substring(1).toLowerCase();
    }
}
