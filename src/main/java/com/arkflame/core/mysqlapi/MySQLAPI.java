package com.arkflame.core.mysqlapi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main entry point for the MySQL Database API.
 * Handles connection pooling and provides high-level methods for data persistence with MySQL.
 */
public class MySQLAPI {
    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;
    private final EntityMapper entityMapper;

    public MySQLAPI(JavaPlugin plugin, MySQLConfig config) {
        this.plugin = plugin;
        this.entityMapper = new EntityMapper(this);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase());
        hikariConfig.setUsername(config.getUser());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.addDataSourceProperty("useUnicode", "true");
        hikariConfig.addDataSourceProperty("characterEncoding", "utf8");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Gets a connection from the pool. Use with try-with-resources.
     * This is an internal method for the EntityMapper.
     * @return A database connection.
     * @throws SQLException if a connection cannot be obtained.
     */
    Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Saves an object to the database asynchronously.
     * This will automatically create/update the table schema and then insert or update the data.
     */
    public CompletableFuture<Void> save(Object object) {
        return CompletableFuture.runAsync(() -> entityMapper.save(object),
                run -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, run));
    }

    /**
     * Loads a single object from the database by its primary key.
     */
    public <T> CompletableFuture<T> loadById(Class<T> clazz, Object primaryKeyValue) {
        return CompletableFuture.supplyAsync(() -> entityMapper.loadById(clazz, primaryKeyValue),
                run -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, run));
    }

    /**
     * Loads a list of objects from the database that match a specific key-value pair.
     */
    public <T> CompletableFuture<List<T>> loadAllBy(Class<T> clazz, String key, Object value) {
        return CompletableFuture.supplyAsync(() -> entityMapper.loadAllBy(clazz, key, value),
                run -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, run));
    }

    /**
     * Shuts down the database connection pool. Call this in your plugin's onDisable.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}