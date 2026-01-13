package main.java.com.banking.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Database Connection Manager using HikariCP Connection Pool
 * Ensures efficient connection reuse and resource management
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;

    // Static block - Initialize connection pool on class load
    static {
        try {
            initializeDataSource();
        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Initialize HikariCP connection pool with properties from application.properties
     */
    private static void initializeDataSource() throws Exception {
        Properties props = new Properties();
        
        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            
            if (input == null) {
                throw new RuntimeException("application.properties file not found");
            }
            props.load(input);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password"));
        config.setDriverClassName(props.getProperty("db.driver"));
        
        // Pool configuration
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("hikari.maximumPoolSize", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("hikari.minimumIdle", "5")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("hikari.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("hikari.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("hikari.maxLifetime", "1800000")));
        
        // Performance optimizations
        config.setAutoCommit(true);
        config.setLeakDetectionThreshold(60000); // Log connections held >60 seconds
        
        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized successfully");
    }

    /**
     * Get connection from pool
     * @return Database Connection from HikariCP pool
     * @throws SQLException if connection cannot be obtained
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool is not initialized or closed");
        }
        return dataSource.getConnection();
    }

    /**
     * Close connection pool (call during application shutdown)
     */
    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Connection pool closed");
        }
    }

    /**
     * Get pool statistics for monitoring
     */
public static String getPoolStats() {
    if (dataSource != null) {
        return String.format(
            "Active: %d, Idle: %d, Total: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getMaximumPoolSize()
        );
    }
    return "Connection pool not initialized";
}


    private DatabaseConnection() {}
}