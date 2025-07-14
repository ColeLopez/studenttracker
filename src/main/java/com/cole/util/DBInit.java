package com.cole.util;

import java.sql.Connection;
import java.sql.Statement;

public class DBInit {
    public static void initializeDatabase() {
        String[] schemaStatements = {
            // Split the schema into Java-executable SQL commands
            "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, salt TEXT NOT NULL);",
            "CREATE TABLE IF NOT EXISTS students (student_id INTEGER PRIMARY KEY AUTOINCREMENT, student_number TEXT UNIQUE NOT NULL, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT, phone TEXT, enrollment_date TEXT, current_slp_id INTEGER, status TEXT, FOREIGN KEY (current_slp_id) REFERENCES slps(slp_id));",
            "CREATE TABLE IF NOT EXISTS slps (slp_id INTEGER PRIMARY KEY AUTOINCREMENT, slp_code TEXT NOT NULL, name TEXT, description TEXT);",
            "CREATE TABLE IF NOT EXISTS modules (module_id INTEGER PRIMARY KEY AUTOINCREMENT, module_code TEXT NOT NULL UNIQUE, name TEXT, pass_rate INTEGER NOT NULL);",
            "CREATE TABLE IF NOT EXISTS slp_modules (id INTEGER PRIMARY KEY AUTOINCREMENT, slp_id INTEGER NOT NULL, module_id INTEGER NOT NULL, FOREIGN KEY (slp_id) REFERENCES slps(slp_id), FOREIGN KEY (module_id) REFERENCES modules(module_id));",
            "CREATE TABLE IF NOT EXISTS exams (exam_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, module_id INTEGER NOT NULL, exam_type TEXT NOT NULL CHECK (exam_type IN ('formative', 'summative', 'supplementary')), date_taken TEXT, score REAL, result TEXT, FOREIGN KEY (student_id) REFERENCES students(student_id), FOREIGN KEY (module_id) REFERENCES modules(module_id));",
            "CREATE TABLE IF NOT EXISTS notes (note_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, note_text TEXT, date_added TEXT, FOREIGN KEY (student_id) REFERENCES students(student_id));",
            "CREATE TABLE IF NOT EXISTS follow_ups (followup_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, due_date TEXT NOT NULL, description TEXT, completed INTEGER DEFAULT 0, FOREIGN KEY (student_id) REFERENCES students(student_id));"
        };

        try (Connection conn = DBUtil.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : schemaStatements) {
                stmt.execute(sql);
            }
            System.out.println("Database initialized successfully.");
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
