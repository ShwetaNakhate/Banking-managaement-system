package main.java.com.banking.service;

import main.java.com.banking.dao.AccountDAO;
import main.java.com.banking.dao.TransactionDAO;
import main.java.com.banking.models.Account;
import main.java.com.banking.models.Transaction;
import main.java.com.banking.utils.DatabaseConnection;
import main.java.com.banking.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Banking service - handles core banking operations
 * Uses JDBC transactions for ACID compliance
 */
public class BankingService {
    private static final Logger logger = LoggerFactory.getLogger(BankingService.class);
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    // Transaction limits (for security)
    private static final BigDecimal MAX_WITHDRAWAL = new BigDecimal("50000");
    private static final BigDecimal MAX_TRANSFER = new BigDecimal("100000");
    private static final BigDecimal MAX_DEPOSIT = new BigDecimal("500000");

    public BankingService(AccountDAO accountDAO, TransactionDAO transactionDAO) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
    }

    /**
     * Create new account for user
     * @param userId User ID
     * @param accountType Type of account
     * @return Account if created successfully
     */
    public Optional<Account> createAccount(int userId, Account.AccountType accountType) {
        // Generate unique account number
        String accountNumber = "ACC" + System.currentTimeMillis() + 
                              (int)(Math.random() * 10000);

        Account newAccount = new Account(userId, accountNumber, accountType);

        if (accountDAO.createAccount(newAccount)) {
            logger.info("Account created for user {}: {}", userId, accountNumber);
            return accountDAO.findByAccountNumber(accountNumber);
        }

        return Optional.empty();
    }

    /**
     * Deposit money to account
     * @param accountId Target account ID
     * @param amount Amount to deposit
     * @param description Transaction description
     * @return true if deposit successful
     */
    public boolean deposit(int accountId, BigDecimal amount, String description) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(MAX_DEPOSIT) > 0) {
            logger.warn("Invalid deposit amount: {}", amount);
            return false;
        }

        Optional<Account> accountOpt = accountDAO.findById(accountId);
        if (accountOpt.isEmpty()) {
            logger.warn("Account not found: {}", accountId);
            return false;
        }

        Account account = accountOpt.get();

        // Check account status
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            logger.warn("Cannot deposit to inactive account: {}", accountId);
            return false;
        }

        // Use transaction for ACID compliance
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // Update balance
                BigDecimal newBalance = account.getBalance().add(amount);
                accountDAO.updateBalance(accountId, newBalance);

                // Record transaction
                Transaction transaction = new Transaction(
                    accountId, null, Transaction.TransactionType.DEPOSIT,
                    amount, description
                );
                transactionDAO.createTransaction(transaction);

                conn.commit(); // Commit if all operations succeed
                logger.info("Deposit successful: {} to account {}", amount, accountId);
                return true;

            } catch (Exception e) {
                conn.rollback(); // Rollback on error
                logger.error("Deposit failed, transaction rolled back", e);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Database error during deposit", e);
            return false;
        }
    }

    /**
     * Withdraw money from account
     * @param accountId Source account ID
     * @param amount Amount to withdraw
     * @param description Transaction description
     * @return true if withdrawal successful
     */
    public boolean withdraw(int accountId, BigDecimal amount, String description) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(MAX_WITHDRAWAL) > 0) {
            logger.warn("Invalid withdrawal amount: {}", amount);
            return false;
        }

        Optional<Account> accountOpt = accountDAO.findById(accountId);
        if (accountOpt.isEmpty()) {
            logger.warn("Account not found: {}", accountId);
            return false;
        }

        Account account = accountOpt.get();

        // Check account status
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            logger.warn("Cannot withdraw from inactive account: {}", accountId);
            return false;
        }

        // Check sufficient balance
        if (account.getBalance().compareTo(amount) < 0) {
            logger.warn("Insufficient balance for withdrawal: {}", accountId);
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Update balance
                BigDecimal newBalance = account.getBalance().subtract(amount);
                accountDAO.updateBalance(accountId, newBalance);

                // Record transaction
                Transaction transaction = new Transaction(
                    accountId, null, Transaction.TransactionType.WITHDRAWAL,
                    amount, description
                );
                transactionDAO.createTransaction(transaction);

                conn.commit();
                logger.info("Withdrawal successful: {} from account {}", amount, accountId);
                return true;

            } catch (Exception e) {
                conn.rollback();
                logger.error("Withdrawal failed, transaction rolled back", e);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Database error during withdrawal", e);
            return false;
        }
    }

    /**
     * Transfer money between accounts
     * @param fromAccountId Source account ID
     * @param toAccountId Destination account ID
     * @param amount Transfer amount
     * @param description Transfer description
     * @return true if transfer successful
     */
    public boolean transfer(int fromAccountId, int toAccountId, BigDecimal amount, String description) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(MAX_TRANSFER) > 0) {
            logger.warn("Invalid transfer amount: {}", amount);
            return false;
        }

        // Validate accounts exist
        Optional<Account> fromOpt = accountDAO.findById(fromAccountId);
        Optional<Account> toOpt = accountDAO.findById(toAccountId);

        if (fromOpt.isEmpty() || toOpt.isEmpty()) {
            logger.warn("One or both accounts not found");
            return false;
        }

        Account fromAccount = fromOpt.get();
        Account toAccount = toOpt.get();

        // Check account status
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE ||
            toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            logger.warn("Cannot transfer from/to inactive account");
            return false;
        }

        // Check sufficient balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            logger.warn("Insufficient balance for transfer");
            return false;
        }

        // IMPORTANT: Use JDBC transaction for atomic operation
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Withdraw from source
                BigDecimal fromNewBalance = fromAccount.getBalance().subtract(amount);
                accountDAO.updateBalance(fromAccountId, fromNewBalance);

                // Deposit to destination
                BigDecimal toNewBalance = toAccount.getBalance().add(amount);
                accountDAO.updateBalance(toAccountId, toNewBalance);

                // Record transaction
                Transaction transaction = new Transaction(
                    fromAccountId, toAccountId, Transaction.TransactionType.TRANSFER,
                    amount, description
                );
                transactionDAO.createTransaction(transaction);

                conn.commit(); // Both operations succeed together
                logger.info("Transfer successful: {} from account {} to {}", 
                           amount, fromAccountId, toAccountId);
                return true;

            } catch (Exception e) {
                conn.rollback(); // Both operations rollback together
                logger.error("Transfer failed, transaction rolled back", e);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Database error during transfer", e);
            return false;
        }
    }

    /**
     * Get account balance
     */
    public Optional<BigDecimal> getBalance(int accountId) {
        return accountDAO.getBalance(accountId);
    }

    /**
     * Get account details
     */
    public Optional<Account> getAccount(int accountId) {
        return accountDAO.findById(accountId);
    }
}
