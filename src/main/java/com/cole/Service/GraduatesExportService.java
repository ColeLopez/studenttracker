package com.cole.Service;

import com.cole.util.DBUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GraduatesExportService {

    public void exportGraduatesToExcel(File file) throws Exception {
        String[] columns = {
            "First Name", "Second Name", "Surname", "ID Number", "Student number",
            "OSAP Case Number", "Programme Name", "Branch", "Email Addy"
        };

        String sql = "SELECT first_name, second_name, last_name, id_number, student_number, slp_course, branch, email " +
                     "FROM students_to_graduate";

        try (
            Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            XSSFWorkbook workbook = new XSSFWorkbook()
        ) {
            Sheet sheet = workbook.createSheet("Graduates");

            // --- Title Row ---
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SLP List for Graduation");

            // Title style: 16pt Aptos (or Calibri fallback) + border
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Aptos");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            // Borders
            titleStyle.setBorderTop(BorderStyle.THIN);
            titleStyle.setBorderBottom(BorderStyle.THIN);
            titleStyle.setBorderLeft(BorderStyle.THIN);
            titleStyle.setBorderRight(BorderStyle.THIN);
            titleCell.setCellStyle(titleStyle);

            // Merge title across all columns
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                0, 0, 0, columns.length - 1
            ));
            // Apply border to merged cells
            for (int i = 1; i < columns.length; i++) {
                Cell mergedCell = titleRow.createCell(i);
                mergedCell.setCellStyle(titleStyle);
            }

            // --- Header Row ---
            Row header = sheet.createRow(1);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // Borders
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data Rows ---
            CellStyle dataStyle = workbook.createCellStyle();
            Font dataFont = workbook.createFont();
            dataFont.setFontName("Arial");
            dataFont.setFontHeightInPoints((short) 10);
            dataStyle.setFont(dataFont);
            // Borders
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            int rowNum = 2;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = row.createCell(i);
                    switch (i) {
                        case 0: cell.setCellValue(rs.getString("first_name")); break;
                        case 1: cell.setCellValue(rs.getString("second_name")); break;
                        case 2: cell.setCellValue(rs.getString("last_name")); break;
                        case 3: cell.setCellValue(rs.getString("id_number")); break;
                        case 4: cell.setCellValue(rs.getString("student_number")); break;
                        case 5: cell.setCellValue(""); break; // OSAP Case Number blank
                        case 6: cell.setCellValue(rs.getString("slp_course")); break; // Programme Name
                        case 7: cell.setCellValue(rs.getString("branch")); break;
                        case 8: cell.setCellValue(rs.getString("email")); break;
                    }
                    cell.setCellStyle(dataStyle);
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
}
