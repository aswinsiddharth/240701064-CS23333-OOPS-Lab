package com.gymmanagementsystem.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.net.URI;

public class DatabaseConnection {
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();

            // Check for DATABASE_URL environment variable (for cloud deployment)
            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl != null && databaseUrl.startsWith("mysql://")) {
                URI dbUri = new URI(databaseUrl);
                String username = null;
                String password = null;

                if (dbUri.getUserInfo() != null) {
                    String[] credentials = dbUri.getUserInfo().split(":");
                    username = credentials[0];
                    if (credentials.length > 1) {
                        password = credentials[1];
                    }
                }

                String jdbcUrl = "jdbc:mysql://" + dbUri.getHost() +
                        (dbUri.getPort() != -1 ? ":" + dbUri.getPort() : ":3306") +
                        dbUri.getPath() +
                        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

                config.setJdbcUrl(jdbcUrl);
                if (username != null) config.setUsername(username);
                if (password != null) config.setPassword(password);
            } else {
                // Use individual environment variables or defaults
                String url = System.getenv("DB_URL") != null
                        ? System.getenv("DB_URL")
                        : "jdbc:mysql://localhost:3306/gym_management";

                // Ensure proper JDBC URL format
                if (!url.startsWith("jdbc:")) {
                    url = "jdbc:" + url;
                }

                // Add MySQL connection parameters if not present
                if (!url.contains("?")) {
                    url += "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                }

                config.setJdbcUrl(url);

                // Get username (check multiple environment variables)
                String username = System.getenv("DB_USERNAME") != null
                        ? System.getenv("DB_USERNAME")
                        : "root";

                // Get password (check multiple environment variables)
                String password = System.getenv("DB_PASSWORD") != null
                        ? System.getenv("DB_PASSWORD")
                        : "Ajay1234";  // CHANGE THIS!

                config.setUsername(username);
                config.setPassword(password);
            }

            // MySQL driver
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            // MySQL-specific optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            dataSource = new HikariDataSource(config);
            System.out.println("MySQL database connection pool initialized successfully!");
        } catch (Exception e) {
            System.err.println("Failed to initialize database connection pool: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Database connection pool closed.");
        }
    }

    public static boolean isPoolInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }
}