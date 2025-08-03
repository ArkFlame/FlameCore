package com.arkflame.flamecore.sqliteapi;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * A final class to hold SQLite database file configuration.
 */
public final class SQLiteConfig {
    private final File databaseFile;

    /**
     * Creates a configuration for an SQLite database.
     * @param plugin The plugin instance, used to get the data folder.
     * @param filename The name of the database file (e.g., "playerdata.db").
     */
    public SQLiteConfig(JavaPlugin plugin, String filename) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.databaseFile = new File(dataFolder, filename);
        if (!this.databaseFile.exists()) {
            try {
                this.databaseFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create SQLite database file!", e);
            }
        }
    }

    public File getDatabaseFile() {
        return databaseFile;
    }
}