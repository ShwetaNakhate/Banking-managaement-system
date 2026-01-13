package main.java.com.banking.utils;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Password hashing using ARGON2 (OWASP recommended)
 * ARGON2 is more secure against GPU/ASIC attacks than bcrypt/scrypt
 */
public class PasswordUtil {
    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);
    private static final Argon2 argon2 = Argon2Factory.create();
    
    // ARGON2 Parameters (OWASP recommendations for interactive use)
    private static final int ITERATIONS = 2;      // Number of iterations
    private static final int MEMORY = 19;         // Memory in KiB exponent (2^19)
    private static final int PARALLELISM = 1;     // Parallelism factor

    /**
     * Hash password securely using ARGON2
     * @param password Plain text password
     * @return Hashed password ready for database storage
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            String hash = argon2.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray());
            logger.debug("Password hashed successfully");
            return hash;
        } catch (Exception e) {
            logger.error("Password hashing failed", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verify password against hash
     * @param password Plain text password to verify
     * @param hash Stored hash from database
     * @return true if password matches hash
     */
    public static boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        
        try {
            boolean verified = argon2.verify(hash, password.toCharArray());
            if (!verified) {
                logger.warn("Password verification failed - incorrect password");
            }
            return verified;
        } catch (Exception e) {
            logger.error("Password verification failed", e);
            return false;
        }
    }

    /**
     * Generate random secure password (for password resets)
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        
        return password.toString();
    }

    private PasswordUtil() {}
}