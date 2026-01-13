package main.java.com.banking.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity representing a banking transaction
 */
public class Transaction {
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, INTEREST
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED
    }

    private int transactionId;
    private int fromAccountId;
    private Integer toAccountId;  // Nullable for deposits
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private TransactionStatus status;

    // Constructor for transaction
    public Transaction(int fromAccountId, Integer toAccountId, TransactionType transactionType,
                      BigDecimal amount, String description) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.status = TransactionStatus.PENDING;
    }

    // Full constructor
    public Transaction(int transactionId, int fromAccountId, Integer toAccountId,
                      TransactionType transactionType, BigDecimal amount, String description,
                      LocalDateTime transactionDate, TransactionStatus status) {
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.status = status;
    }

    // Getters and Setters
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(int fromAccountId) { this.fromAccountId = fromAccountId; }

    public Integer getToAccountId() { return toAccountId; }
    public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", fromAccountId=" + fromAccountId +
                ", toAccountId=" + toAccountId +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}
