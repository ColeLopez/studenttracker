package com.cole.util;

import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for password hashing and salt generation.
 * <p>
 * Note: For production, consider using a stronger algorithm like bcrypt, PBKDF2, or Argon2.
 * <p>
 * Usage:
 * <pre>
 *     String salt = PasswordUtil.generateSalt();
 *     String hash = PasswordUtil.hashPassword(password, salt);
 * </pre>
 * <p>
 * This class cannot be instantiated.
 */
public final class PasswordUtil {
    /** SLF4J logger for PasswordUtil operations. */
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private PasswordUtil() {
        // Prevent instantiation
    }

    /**
     * Generates a random salt for password hashing.
     * <p>
     * Uses SecureRandom for cryptographic strength.
     *
     * @return Base64-encoded salt string
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes the password with the provided salt using SHA-256.
     * <p>
     * For production, use a stronger algorithm (bcrypt, PBKDF2, Argon2).
     *
     * @param password The password to hash (plain text)
     * @param salt The Base64-encoded salt
     * @return Base64-encoded hashed password
     * @throws RuntimeException if hashing fails
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
