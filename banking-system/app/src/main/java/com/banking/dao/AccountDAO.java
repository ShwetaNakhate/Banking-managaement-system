package main.java.com.banking.dao;

import main.java.com.banking.models.Account;
import main.java.com.banking.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Account operations
 */
public class AccountDAO {
    private static final Logger logger = LoggerFactory.getLogger(AccountDAO.class);

    /**
     * Create new account
     * @param account Account object
     * @return true if successful
     */
    public boolean createAccount(Account account) {
        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, account.getUserId());
            pstmt.setString(2, account.getAccountNumber());
            pstmt.setString(3, account.getAccountType().name());
            pstmt.setBigDecimal(4, account.getBalance());
            pstmt.setString(5, account.getStatus().name());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Account created: {}", account.getAccountNumber());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error creating account", e);
        }
        
        return false;
    }

    /**
     * Find account by account number
     */
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT account_id, user_id, account_number, account_type, balance, status, " +
                     "created_at, updated_at FROM accounts WHERE account_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding account by number", e);
        }
        
        return Optional.empty();
    }

    /**
     * Find account by ID
     */
    public Optional<Account> findById(int accountId) {
        String sql = "SELECT account_id, user_id, account_number, account_type, balance, status, " +
                     "created_at, updated_at FROM accounts WHERE account_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, accountId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding account by ID", e);
        }
        
        return Optional.empty();
    }

    /**
     * Get all accounts for a user
     */
    public List<Account> findByUserId(int userId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT account_id, user_id, account_number, account_type, balance, status, " +
                     "created_at, updated_at FROM accounts WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapResultSetToAccount(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching accounts for user", e);
        }
        
        return accounts;
    }

    /**
     * Get current balance
     */
    public Optional<BigDecimal> getBalance(int accountId) {
        String sql = "SELECT balance FROM accounts WHERE account_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, accountId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getBigDecimal("balance"));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error getting account balance", e);
        }
        
        return Optional.empty();
    }

    /**
     * Update account balance (used in transactions with proper locking)
     */
    public boolean updateBalance(int accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setInt(2, accountId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Account balance updated for account ID: {}", accountId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating account balance", e);
        }
        
        return false;
    }

    /**
     * Update account status (freeze/close accounts)
     */
    public boolean updateStatus(int accountId, Account.AccountStatus status) {
        String sql = "UPDATE accounts SET status = ? WHERE account_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.name());
            pstmt.setInt(2, accountId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Account status updated: {} -> {}", accountId, status);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error updating account status", e);
        }
        
        return false;
    }

    /**
     * Map ResultSet to Account object
     */
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        return new Account(
            rs.getInt("account_id"),
            rs.getInt("user_id"),
            rs.getString("account_number"),
            Account.AccountType.valueOf(rs.getString("account_type")),
            rs.getBigDecimal("balance"),
            Account.AccountStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toInstant()
                  .atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
            rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toInstant()
                  .atZone(ZoneId.systemDefault()).toLocalDateTime() : null
        );
    }
}