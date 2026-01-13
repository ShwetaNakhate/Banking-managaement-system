package main.java.com.banking;

import main.java.com.banking.dao.AccountDAO;
import main.java.com.banking.dao.UserDAO;
import main.java.com.banking.dao.TransactionDAO;
import main.java.com.banking.models.Account;
import main.java.com.banking.models.User;
import main.java.com.banking.service.AuthService;
import main.java.com.banking.service.BankingService;
import main.java.com.banking.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Scanner;

/**
 * Main application entry point
 * Demonstrates complete banking system workflow
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final Scanner scanner = new Scanner(System.in);

    private static UserDAO userDAO;
    private static AccountDAO accountDAO;
    private static TransactionDAO transactionDAO;
    private static AuthService authService;
    private static BankingService bankingService;

    private static User currentUser = null;

    public static void main(String[] args) {
        logger.info("=== Banking Management System Started ===");
        
        try {
            // Initialize DAOs and Services
            initializeServices();

            // Display menu
            displayMainMenu();

        } catch (Exception e) {
            logger.error("Application error", e);
        } finally {
            // Clean up resources
            DatabaseConnection.closeDataSource();
            scanner.close();
            logger.info("=== Banking System Stopped ===");
        }
    }

    private static void initializeServices() {
        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
        transactionDAO = new TransactionDAO();
        authService = new AuthService(userDAO);
        bankingService = new BankingService(accountDAO, transactionDAO);
        
        logger.info("Services initialized successfully");
    }

    private static void displayMainMenu() {
        while (true) {
            if (currentUser == null) {
                displayAuthMenu();
            } else {
                displayBankingMenu();
            }
        }
    }

    private static void displayAuthMenu() {
        System.out.println("\n===== BANKING SYSTEM =====");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                registerUser();
                break;
            case "2":
                loginUser();
                break;
            case "3":
                System.out.println("Goodbye!");
                System.exit(0);
            default:
                System.out.println("Invalid option");
        }
    }

    private static void registerUser() {
        System.out.println("\n===== REGISTER =====");
        
        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Full Name: ");
        String fullName = scanner.nextLine();

        System.out.print("Password (8+ chars, upper/lower/digit/special): ");
        String password = scanner.nextLine();

        if (authService.register(username, email, password, fullName)) {
            System.out.println("✓ Registration successful! Please login.");
        } else {
            System.out.println("✗ Registration failed. Please try again.");
        }
    }

    private static void loginUser() {
        System.out.println("\n===== LOGIN =====");
        
        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> userOpt = authService.login(username, password);
        
        if (userOpt.isPresent()) {
            currentUser = userOpt.get();
            System.out.println("✓ Login successful! Welcome, " + currentUser.getFullName());
        } else {
            System.out.println("✗ Login failed. Invalid credentials.");
        }
    }

    private static void displayBankingMenu() {
        System.out.println("\n===== BANKING MENU =====");
        System.out.println("Welcome, " + currentUser.getFullName());
        System.out.println("1. Create Account");
        System.out.println("2. View Accounts");
        System.out.println("3. Check Balance");
        System.out.println("4. Deposit");
        System.out.println("5. Withdraw");
        System.out.println("6. Transfer");
        System.out.println("7. Change Password");
        System.out.println("8. Logout");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                createAccount();
                break;
            case "2":
                viewAccounts();
                break;
            case "3":
                checkBalance();
                break;
            case "4":
                deposit();
                break;
            case "5":
                withdraw();
                break;
            case "6":
                transfer();
                break;
            case "7":
                changePassword();
                break;
            case "8":
                currentUser = null;
                System.out.println("✓ Logged out successfully");
                break;
            default:
                System.out.println("Invalid option");
        }
    }

    private static void createAccount() {
        System.out.println("\n===== CREATE ACCOUNT =====");
        System.out.println("1. SAVINGS");
        System.out.println("2. CHECKING");
        System.out.println("3. BUSINESS");
        System.out.print("Choose account type: ");

        String choice = scanner.nextLine();
        Account.AccountType accountType = null;

        switch (choice) {
            case "1":
                accountType = Account.AccountType.SAVINGS;
                break;
            case "2":
                accountType = Account.AccountType.CHECKING;
                break;
            case "3":
                accountType = Account.AccountType.BUSINESS;
                break;
            default:
                System.out.println("Invalid account type");
                return;
        }

        Optional<Account> accountOpt = bankingService.createAccount(currentUser.getUserId(), accountType);
        
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            System.out.println("✓ Account created successfully!");
            System.out.println("Account Number: " + account.getAccountNumber());
        } else {
            System.out.println("✗ Failed to create account");
        }
    }

    private static void viewAccounts() {
        System.out.println("\n===== YOUR ACCOUNTS =====");
        var accounts = accountDAO.findByUserId(currentUser.getUserId());
        
        if (accounts.isEmpty()) {
            System.out.println("No accounts found");
            return;
        }

        for (Account account : accounts) {
            System.out.println("Account: " + account.getAccountNumber() + 
                             " | Type: " + account.getAccountType() + 
                             " | Balance: ₹" + account.getBalance() + 
                             " | Status: " + account.getStatus());
        }
    }

    private static void checkBalance() {
        System.out.print("Enter account number: ");
        String accountNumber = scanner.nextLine();

        Optional<Account> accountOpt = accountDAO.findByAccountNumber(accountNumber);
        
        if (accountOpt.isPresent()) {
            System.out.println("Balance: ₹" + accountOpt.get().getBalance());
        } else {
            System.out.println("Account not found");
        }
    }

    private static void deposit() {
        System.out.print("Enter account number: ");
        String accountNumber = scanner.nextLine();

        Optional<Account> accountOpt = accountDAO.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            System.out.println("Account not found");
            return;
        }

        System.out.print("Enter amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine());

        System.out.print("Description: ");
        String description = scanner.nextLine();

        if (bankingService.deposit(accountOpt.get().getAccountId(), amount, description)) {
            System.out.println("✓ Deposit successful!");
        } else {
            System.out.println("✗ Deposit failed");
        }
    }

    private static void withdraw() {
        System.out.print("Enter account number: ");
        String accountNumber = scanner.nextLine();

        Optional<Account> accountOpt = accountDAO.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            System.out.println("Account not found");
            return;
        }

        System.out.print("Enter amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine());

        System.out.print("Description: ");
        String description = scanner.nextLine();

        if (bankingService.withdraw(accountOpt.get().getAccountId(), amount, description)) {
            System.out.println("✓ Withdrawal successful!");
        } else {
            System.out.println("✗ Withdrawal failed");
        }
    }

    private static void transfer() {
        System.out.print("Enter source account number: ");
        String fromAccountNumber = scanner.nextLine();

        System.out.print("Enter destination account number: ");
        String toAccountNumber = scanner.nextLine();

        Optional<Account> fromOpt = accountDAO.findByAccountNumber(fromAccountNumber);
        Optional<Account> toOpt = accountDAO.findByAccountNumber(toAccountNumber);

        if (fromOpt.isEmpty() || toOpt.isEmpty()) {
            System.out.println("One or both accounts not found");
            return;
        }

        System.out.print("Enter amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine());

        System.out.print("Description: ");
        String description = scanner.nextLine();

        if (bankingService.transfer(fromOpt.get().getAccountId(), toOpt.get().getAccountId(), amount, description)) {
            System.out.println("✓ Transfer successful!");
        } else {
            System.out.println("✗ Transfer failed");
        }
    }

    private static void changePassword() {
        System.out.print("Enter current password: ");
        String oldPassword = scanner.nextLine();

        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine();

        if (authService.changePassword(currentUser.getUserId(), oldPassword, newPassword)) {
            System.out.println("✓ Password changed successfully");
        } else {
            System.out.println("✗ Password change failed");
        }
    }
}