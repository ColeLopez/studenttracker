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

public final class DBUtil {
    /** SLF4J logger for DBUtil operations. */
    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);

    /** Database folder name. */
    private static final String DB_FOLDER = "database";
    /** Database file path. */
    private static final String DB_PATH = DB_FOLDER + "/data.db";
    /** JDBC URL for SQLite database. */
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    static {
        createDatabaseFileIfMissing();
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private DBUtil() {
        // Prevent instantiation
    }

    /**
     * Creates the database file and folder if they do not exist.
     * Logs the creation or existence of the file/folder.
     *
     * @throws RuntimeException if file creation fails
     */
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
            throw new RuntimeException("Failed to create database file", e);
        }
    }

    /**
     * Gets a connection to the SQLite database.
     * <p>
     * Caller is responsible for closing the connection.
     *
     * @return Connection object to the database
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
