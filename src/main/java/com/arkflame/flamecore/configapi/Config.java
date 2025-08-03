package com.arkflame.flamecore.configapi;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.arkflame.flamecore.colorapi.ColorAPI;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper around a single YAML configuration file, providing easy and color-aware access to its values.
 * This class should be obtained via {@link ConfigAPI#getConfig(String)}.
 */
public class Config {
    private final File file;
    private FileConfiguration config;

    /**
     * Internal constructor. Creates the file if it doesn't exist by copying from resources.
     */
    Config(String path, JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), path);
        
        if (!file.exists()) {
            // This ensures parent directories are created.
            file.getParentFile().mkdirs();
            // This copies the file from the JAR's resources to the data folder.
            plugin.saveResource(path, false);
        }
        
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Gets a string from the config, automatically colorized.
     * @param path The path to the string.
     * @return The colorized string, or null if not found.
     */
    public String getString(String path) {
        String raw = config.getString(path);
        return raw != null ? ColorAPI.colorize(raw).toLegacyText() : null;
    }

    /**
     * Gets a string from the config with a default value, automatically colorized.
     * @param path The path to the string.
     * @param def The default value to return if the path is not found.
     * @return The colorized string.
     */
    public String getString(String path, String def) {
        String raw = config.getString(path, def);
        return raw != null ? ColorAPI.colorize(raw).toLegacyText() : def;
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public double getDouble(String path) {
        return config.getDouble(path);
    }

    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    /**
     * Gets a list of strings from the config, with each entry automatically colorized.
     * @param path The path to the string list.
     * @return A list of colorized strings.
     */
    public List<String> getStringList(String path) {
        return config.getStringList(path).stream()
                .map(line -> ColorAPI.colorize(line).toLegacyText())
                .collect(Collectors.toList());
    }

    public Location getLocation(String path) {
        return config.getLocation(path);
    }

    public ItemStack[] getItems(String path) {
        List<?> list = config.getList(path);
        if (list == null) {
            return new ItemStack[0];
        }
        return list.toArray(new ItemStack[0]);
    }
    
    /**
     * Sets a value at a specific path in the configuration.
     * Remember to call {@link #save()} to persist changes to the file.
     * @param path The path to set.
     * @param value The value to set.
     */
    public void set(String path, Object value) {
        config.set(path, value);
    }

    /**
     * Checks if the configuration contains a value at the specified path.
     * @param path The path to check.
     * @return True if the path exists, false otherwise.
     */
    public boolean contains(String path) {
        return config.contains(path);
    }

    /**
     * Gets a configuration section at the specified path.
     * @param path The path to the section.
     * @return The ConfigurationSection, or null if not found.
     */
    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    /**
     * Saves all changes made to the configuration back to the file on disk.
     */
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.err.println("Could not save config file: " + file.getName());
            e.printStackTrace();
        }
    }

    /**
     * Reloads the configuration from the file on disk, discarding any unsaved changes.
     */
    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Gets the raw FileConfiguration object for advanced operations.
     * @return The underlying FileConfiguration.
     */
    public FileConfiguration getRaw() {
        return config;
    }
}