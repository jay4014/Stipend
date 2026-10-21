package com.stipend.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtil: PBKDF2WithHmacSHA256 hashing with a per-user salt.
 *
 * ENCAPSULATION: the stored format is "PBKDF2$<iterations>$<salt>$<hash>".
 * The raw hash is never exposed — only verified via verifyPassword().
 */
public final class PasswordUtil {

    // 310 000 iterations recommended by OWASP 2023 for PBKDF2-SHA256
    private static final int ITERATIONS = 310_000;
    private static final int KEY_LENGTH = 256; // bits

    private PasswordUtil() {} // no instances

    /**
     * Hashes a password with a fresh random salt.
     *
     * @param password plain-text password (cleared after use by the caller)
     * @return stored hash string in the format "PBKDF2$iters$salt$hash"
     */
    public static String hashPassword(char[] password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return "PBKDF2$" + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt)  + "$"
                    + Base64.getEncoder().encodeToString(hash);

        } catch (Exception ex) {
            throw new IllegalStateException("Password hashing failed.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Verifies a plain-text password against a stored hash string.
     * Uses a constant-time comparison to prevent timing attacks.
     *
     * @param password   plain-text password
     * @param storedHash hash string produced by hashPassword()
     * @return true if password matches
     */
    public static boolean verifyPassword(char[] password, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) return false;

        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !"PBKDF2".equals(parts[0])) return false;

        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (iterations < 100_000 || iterations > 1_000_000) return false;

        byte[] salt         = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

        PBEKeySpec spec =
                new PBEKeySpec(password, salt, iterations, expectedHash.length * 8);
        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] actualHash = factory.generateSecret(spec).getEncoded();
            // MessageDigest.isEqual is constant-time — prevents timing attacks
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception ex) {
            return false;
        } finally {
            spec.clearPassword();
        }
    }
}
