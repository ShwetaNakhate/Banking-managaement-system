package main.java.com.banking.dao;

import main.java.com.banking.models.Transaction;
import main.java.com.banking.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Transaction operations
 */
public class TransactionDAO {
    private static final Logger logger = LoggerFactory.getLogger(TransactionDAO.class);

    /**
     * Create transaction record
     */
    public boolean createTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (from_account_id, to_account_id, transaction_type, amount, description, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, transaction.getFromAccountId());
            pstmt.setObject(2, transaction.getToAccountId());
            pstmt.setString(3, transaction.getTransactionType().name());
            pstmt.setBigDecimal(4, transaction.getAmount());
            pstmt.setString(5, transaction.getDescription());
            pstmt.setString(6, transaction.getStatus().name());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("Transaction recorded: {} - {}", 
                           transaction.getTransactionType(), transaction.getAmount());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Error creating transaction", e);
        }
        
        return false;
    }

    /**
     * Get transaction history for account
     */
    public List<Transaction> getTransactionHistory(int accountId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT transaction_id, from_account_id, to_account_id, transaction_type, amount, description, " +
                     "transaction_date, status FROM transactions WHERE from_account_id = ? OR to_account_id = ? " +
                     "ORDER BY transaction_date DESC LIMIT 50";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, accountId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error fetching transaction history", e);
        }
        
        return transactions;
    }

    /**
     * Map ResultSet to Transaction object
     */
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
            rs.getInt("transaction_id"),
            rs.getInt("from_account_id"),
            rs.getObject("to_account_id") != null ? rs.getInt("to_account_id") : null,
            Transaction.TransactionType.valueOf(rs.getString("transaction_type")),
            rs.getBigDecimal("amount"),
            rs.getString("description"),
            rs.getTimestamp("transaction_date") != null ? 
                rs.getTimestamp("transaction_date").toInstant()
                  .atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
            Transaction.TransactionStatus.valueOf(rs.getString("status"))
        );
    }
}