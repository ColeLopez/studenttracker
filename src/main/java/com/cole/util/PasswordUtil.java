package com.cole.util;

import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for password hashing and salt generation.
 * Note: For production, consider using a stronger algorithm like bcrypt, PBKDF2, or Argon2.
 */
public final class PasswordUtil {
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);

    private PasswordUtil() {
        // Prevent instantiation
    }

    /**
     * Generate a random salt for password hashing.
     * @return Base64-encoded salt string
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash the password with the provided salt using SHA-256.
     * @param password The password to hash
     * @param salt The Base64-encoded salt
     * @return Base64-encoded hashed password
     */
    public static String hashPassword(String password, String salt) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            md.update(saltBytes);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (java.security.NoSuchAlgorithmException e) {
            logger.error("Error hashing password: {}", e.getMessage(), e);
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
