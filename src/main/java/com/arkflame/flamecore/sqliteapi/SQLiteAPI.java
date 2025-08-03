package com.arkflame.flamecore.sqliteapi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main entry point for the SQLite Database API.
 * Handles connection pooling and provides high-level methods for data persistence with a local SQLite file.
 */
public class SQLiteAPI {
    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;
    private final EntityMapper entityMapper;

    public SQLiteAPI(JavaPlugin plugin, SQLiteConfig config) {
        this.plugin = plugin;
        this.entityMapper = new EntityMapper(this);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + config.getDatabaseFile().getAbsolutePath());
        
        // Pool size of 1 is recommended for SQLite to prevent database locking issues
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setConnectionTimeout(30000);
        
        // SQLite specific properties for performance
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Gets a connection from the pool. Internal use only.
     */
    Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Saves an object to the database asynchronously.
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