package com.cole.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {

    private static final String DB_FOLDER = "database";
    private static final String DB_PATH = DB_FOLDER + "/data.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    static {
        createDatabaseFileIfMissing();
    }

    private static void createDatabaseFileIfMissing() {
        try {
            File folder = new File(DB_FOLDER);
            if (!folder.exists()) {
                folder.mkdir();
            }

            File dbFile = new File(DB_PATH);
            if (!dbFile.exists()) {
                dbFile.createNewFile();
                System.out.println("Database file created at: " + dbFile.getAbsolutePath());
            } else {
                System.out.println("Database file already exists.");
            }
        } catch (Exception e) {
            System.err.println("Failed to create database file.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
