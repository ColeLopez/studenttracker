package com.cole.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DatabaseBackupService {

    /**
     * Backs up the database file to a backup location.
     * @param dbFile The database file to back up.
     * @param backupFile The backup file location.
     * @throws IOException If an I/O error occurs during backup.
     */
    public void backupDatabase(File dbFile, File backupFile) throws IOException {
        Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }


    /**
     * Restores the database file from a backup location.
     * @param backupFile The backup file to restore.
     * @param dbFile The database file location.
     * @throws IOException If an I/O error occurs during restore.
     */
    public void restoreDatabase(File backupFile, File dbFile) throws IOException {
        Files.copy(backupFile.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
