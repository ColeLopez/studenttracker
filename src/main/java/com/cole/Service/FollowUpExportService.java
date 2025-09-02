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

    private static final String[] COLUMNS = {
        "Student Number", "First Name", "Last Name", "Branch",
        "Due Date", "Description", "Completed"
    };

    /**
     * Exports follow-up history to an Excel file based on the filter type.
     * @param file The file to export to.
     * @param filter The filter type: "completed", "upcoming", or "overdue".
     * @throws Exception If an error occurs during export.
     */
    public void exportFollowUpHistoryToExcel(File file, String filter) throws Exception {
        String sql = buildSql(filter);

        try (
            Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            XSSFWorkbook workbook = new XSSFWorkbook()
        ) {
            Sheet sheet = workbook.createSheet("Follow-Up History");

            // Title Row
            createTitleRow(sheet, workbook, filter);

            // Header Row
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row header = sheet.createRow(1);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(COLUMNS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Style
            CellStyle dataStyle = createDataStyle(workbook);

            // Data Rows
            int rowNum = 2;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rs.getString("student_number"));
                row.createCell(1).setCellValue(rs.getString("first_name"));
                row.createCell(2).setCellValue(rs.getString("last_name"));
                row.createCell(3).setCellValue(rs.getString("branch"));
                row.createCell(4).setCellValue(rs.getString("due_date"));
                row.createCell(5).setCellValue(rs.getString("description"));
                row.createCell(6).setCellValue(rs.getInt("completed") == 1 ? "Yes" : "No");
                for (int i = 0; i < COLUMNS.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Auto-size columns for better performance and appearance
            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    private String buildSql(String filter) {
        StringBuilder sql = new StringBuilder(
            "SELECT s.student_number, s.first_name, s.last_name, s.branch, " +
            "f.due_date, f.description, f.completed " +
            "FROM follow_ups f " +
            "JOIN students s ON f.student_id = s.student_id "
        );
        switch (filter) {
            case "completed":
                sql.append("WHERE f.completed = 1");
                break;
            case "upcoming":
                sql.append("WHERE f.due_date > CURRENT_DATE AND f.completed = 0");
                break;
            case "overdue":
                sql.append("WHERE f.due_date < CURRENT_DATE AND f.completed = 0");
                break;
            default:
                throw new IllegalArgumentException("Invalid filter type: " + filter);
        }
        return sql.toString();
    }

    private void createTitleRow(Sheet sheet, Workbook workbook, String filter) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Follow-Up History Report: " + capitalizeFilter(filter));

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setFontName("Arial");
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setBold(true);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        titleCell.setCellStyle(titleStyle);

        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
            0, 0, 0, COLUMNS.length - 1
        ));
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = createBorderedStyle(workbook);
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private CellStyle createBorderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String capitalizeFilter(String filter) {
        return filter.substring(0, 1).toUpperCase() + filter.substring(1).toLowerCase();
    }
}
