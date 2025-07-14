package com.cole.util;

// ...existing code...
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for database connection and file initialization.
 */
public final class DBUtil {
    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);

    private static final String DB_FOLDER = "database";
    private static final String DB_PATH = DB_FOLDER + "/data.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    static {
        createDatabaseFileIfMissing();
    }

    private DBUtil() {
        // Prevent instantiation
    }

    private static void createDatabaseFileIfMissing() {
        try {
            Path folderPath = Paths.get(DB_FOLDER);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                logger.info("Database folder created at: {}", folderPath.toAbsolutePath());
            }

            Path dbFilePath = Paths.get(DB_PATH);
            if (!Files.exists(dbFilePath)) {
                Files.createFile(dbFilePath);
                logger.info("Database file created at: {}", dbFilePath.toAbsolutePath());
            } else {
                logger.info("Database file already exists at: {}", dbFilePath.toAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Failed to create database file: {}", e.getMessage(), e);
        }
    }

    /**
     * Get a connection to the SQLite database.
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
