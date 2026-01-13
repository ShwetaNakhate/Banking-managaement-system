package main.java.com.banking.service;

import main.java.com.banking.dao.UserDAO;
import main.java.com.banking.models.User;
import main.java.com.banking.utils.PasswordUtil;
import main.java.com.banking.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authentication service - handles user login and registration
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Register new user
     * @param username Desired username
     * @param email User email
     * @param password Plain text password
     * @param fullName User's full name
     * @return true if registration successful
     */
    public boolean register(String username, String email, String password, String fullName) {
        // Validate inputs
        if (!ValidationUtil.isValidUsername(username)) {
            logger.warn("Invalid username format: {}", username);
            return false;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            logger.warn("Invalid email format: {}", email);
            return false;
        }

        if (!ValidationUtil.isStrongPassword(password)) {
            logger.warn("Password does not meet strength requirements");
            return false;
        }

        // Check if user already exists
        if (userDAO.findByUsername(username).isPresent()) {
            logger.warn("Username already exists: {}", username);
            return false;
        }

        if (userDAO.findByEmail(email).isPresent()) {
            logger.warn("Email already registered: {}", email);
            return false;
        }

        // Hash password and create user
        String passwordHash = PasswordUtil.hashPassword(password);
        User newUser = new User(username, email, passwordHash, fullName);

        boolean success = userDAO.createUser(newUser);
        
        if (success) {
            logger.info("User registered successfully: {}", username);
        }
        
        return success;
    }

    /**
     * Authenticate user login
     * @param username Username
     * @param password Plain text password
     * @return User object if authentication successful, empty Optional otherwise
     */
    public Optional<User> login(String username, String password) {
        // Find user by username
        Optional<User> userOpt = userDAO.findByUsername(username);

        if (userOpt.isEmpty()) {
            logger.warn("Login failed - user not found: {}", username);
            return Optional.empty();
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.isActive()) {
            logger.warn("Login failed - user account is inactive: {}", username);
            return Optional.empty();
        }

        // Verify password
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            logger.warn("Login failed - incorrect password: {}", username);
            return Optional.empty();
        }

        logger.info("User logged in successfully: {}", username);
        return Optional.of(user);
    }

    /**
     * Change user password
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        // Find user
        Optional<User> userOpt = userDAO.findById(userId);
        
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Verify old password
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            logger.warn("Password change failed - incorrect old password");
            return false;
        }

        // Validate new password
        if (!ValidationUtil.isStrongPassword(newPassword)) {
            logger.warn("New password does not meet strength requirements");
            return false;
        }

        // Hash and update
        String newPasswordHash = PasswordUtil.hashPassword(newPassword);
        boolean success = userDAO.changePassword(userId, newPasswordHash);

        if (success) {
            logger.info("Password changed for user ID: {}", userId);
        }

        return success;
    }
}