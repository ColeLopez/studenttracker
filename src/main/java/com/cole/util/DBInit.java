package com.cole.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for initializing the database schema.
 * <p>
 * This class provides a static method to create all required tables for the application.
 * It uses SQL statements to ensure tables exist and are up-to-date.
 * <p>
 * Usage:
 * <pre>
 *     DBInit.initializeDatabase();
 * </pre>
 * <p>
 * This class cannot be instantiated.
 */
public final class DBInit {
    /** SLF4J logger for DBInit operations. */
    private static final Logger logger = LoggerFactory.getLogger(DBInit.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private DBInit() {
        // Prevent instantiation
    }

    /**
     * Initializes the database schema by creating all required tables if they do not exist.
     * <p>
     * This method should be called once at application startup.
     * It logs success or failure using SLF4J.
     *
     * @throws RuntimeException if database initialization fails
     */
    public static void initializeDatabase() {
        String[] schemaStatements = {
            // Split the schema into Java-executable SQL commands
            "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, salt TEXT NOT NULL);",
            "CREATE TABLE IF NOT EXISTS students (student_id INTEGER PRIMARY KEY AUTOINCREMENT, student_number TEXT UNIQUE NOT NULL, first_name TEXT NOT NULL, last_name TEXT NOT NULL, email TEXT, phone TEXT, enrollment_date TEXT, current_slp_id INTEGER, status TEXT, FOREIGN KEY (current_slp_id) REFERENCES slps(slp_id));",
            "CREATE TABLE IF NOT EXISTS slps (slp_id INTEGER PRIMARY KEY AUTOINCREMENT, slp_code TEXT NOT NULL, name TEXT, description TEXT);",
            "CREATE TABLE IF NOT EXISTS modules (module_id INTEGER PRIMARY KEY AUTOINCREMENT, module_code TEXT NOT NULL UNIQUE, name TEXT, pass_rate INTEGER NOT NULL);",
            "CREATE TABLE IF NOT EXISTS slp_modules (id INTEGER PRIMARY KEY AUTOINCREMENT, slp_id INTEGER NOT NULL, module_id INTEGER NOT NULL, FOREIGN KEY (slp_id) REFERENCES slps(slp_id), FOREIGN KEY (module_id) REFERENCES modules(module_id));",
            // New junction table to track if a student has received a book for a module
            "CREATE TABLE IF NOT EXISTS student_modules (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, module_id INTEGER NOT NULL, module_code TEXT, module_name TEXT, formative TEXT, summative TEXT, supplementary TEXT, received_book INTEGER DEFAULT 0, FOREIGN KEY (student_id) REFERENCES students(student_id), FOREIGN KEY (module_id) REFERENCES modules(module_id));",
            "CREATE TABLE IF NOT EXISTS notes (note_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, note_text TEXT, date_added TEXT, FOREIGN KEY (student_id) REFERENCES students(student_id));",
            "CREATE TABLE IF NOT EXISTS follow_ups (followup_id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, due_date TEXT NOT NULL, description TEXT, completed INTEGER DEFAULT 0, FOREIGN KEY (student_id) REFERENCES students(student_id));",
        };

        try (Connection conn = DBUtil.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : schemaStatements) {
                stmt.execute(sql);
            }
            logger.info("Database initialized successfully.");
        } catch (SQLException e) {
            logger.error("Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}
