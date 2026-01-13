package main.java.com.banking.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Input validation utility for preventing invalid data entry
 */
public class ValidationUtil {
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtil.class);
    
    // Regex patterns
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^[\\d\\s\\-\\+\\(\\)]{10,15}$");
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{5,20}$");
    
    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate username (5-20 alphanumeric + underscore)
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate password strength (minimum 8 chars, mix of upper/lower/digits/special)
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+=\\-\\[\\]{};:'\",.<>?/\\\\|`~].*");
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Validate phone number
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate monetary amount
     */
    public static boolean isValidAmount(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            return amount > 0 && amount <= 10000000; // Max 10 million per transaction
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate account number format
     */
    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null) return false;
        return accountNumber.matches("^ACC\\d{10}$"); // Format: ACC0000000001
    }

    /**
     * Sanitize string input (prevent XSS-like attacks)
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        
        return input.replaceAll("[<>\"'%;()&+]", "")
                   .trim();
    }

    private ValidationUtil() {}
}