package main.java.com.banking.dao;

import main.java.com.banking.models.User;
import main.java.com.banking.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Data Access Object for User operations
 * Handles all database interactions for users
 * Uses PreparedStatement to prevent SQL injection
 */
public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    /**
     * Create new user in database
     * @param user User object with populated fields
     * @return true if successful
     */
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, full_name, phone, address, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getAddress());
            pstmt.setBoolean(7, user.isActive());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("User created successfully: {}", user.getUsername());
                return true;
            }
            
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                logger.warn("Username or email already exists: {}", user.getUsername());
            } else {
                logger.error("Error creating user", e);
            }
        }
        
        return false;
    }

    /**
     * Find user by username
     * @param username Username to search for
     * @return Optional containing User if found
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT user_id, username, email, password_hash, full_name, phone, address, " +
                     "created_at, updated_at, is_active FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    return Optional.of(user);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding user by username", e);
        }
        
        return Optional.empty();
    }

    /**
     * Find user by ID
     * @param userId User ID to search for
     * @return Optional containing User if found
     */
    public Optional<User> findById(int userId) {
        String sql = "SELECT user_id, username, email, password_hash, full_name, phone, address, " +
                     "created_at, updated_at, is_active FROM users WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    return Optional.of(user);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding user by ID", e);
        }
        
        return Optional.empty();
    }

    /**
     * Find user by email
     * @param email Email to search for
     * @return Optional containing User if found
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT user_id, username, email, password_hash, full_name, phone, address, " +
                     "created_at, updated_at, is_active FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    return Optional.of(user);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding user by email", e);
        }
        
        return Optional.empty();
    }

    /**
     * Update user information
     * @param user User object with updated fields
     * @return true if successful
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone = ?, address = ?, is_active = ? " +
                     "WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getPhone());
            pstmt.setString(4, user.getAddress());
            pstmt.setBoolean(5, user.isActive());
            pstmt.setInt(6, user.getUserId());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("User updated successfully: ID {}", user.getUserId());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating user", e);
        }
        
        return false;
    }

    /**
     * Change user password
     * @param userId User ID
     * @param newPasswordHash New password hash
     * @return true if successful
     */
    public boolean changePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Password changed for user ID: {}", userId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error changing password", e);
        }
        
        return false;
    }

    /**
     * Map ResultSet row to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("full_name"),
            rs.getString("phone"),
            rs.getString("address"),
            rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toInstant()
                  .atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
            rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toInstant()
                  .atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
            rs.getBoolean("is_active")
        );
    }
}