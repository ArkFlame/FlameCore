package com.arkflame.core.configapi;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main factory for creating and managing Config objects.
 * This API handles the loading and creation of configuration files from the plugin's resources.
 */
public final class ConfigAPI {
    private static JavaPlugin plugin;
    private static final Map<String, Config> configCache = new ConcurrentHashMap<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            throw new IllegalStateException("ConfigAPI is already initialized.");
        }
        plugin = pluginInstance;
    }

    /**
     * Gets a Config object for the specified file path.
     * <p>
     * If the file does not exist in the plugin's data folder, it will be
     * automatically copied from the plugin's JAR resources.
     * This handles subdirectories correctly (e.g., "menus/main.yml").
     *
     * @param path The path to the configuration file relative to the data folder (e.g., "config.yml").
     * @return A cached Config object ready for use.
     */
    public static Config getConfig(String path) {
        if (plugin == null) {
            throw new IllegalStateException("ConfigAPI has not been initialized! Call ConfigAPI.init(plugin) in onEnable.");
        }
        // Use a normalized path for the cache key to handle both / and \ separators.
        String normalizedPath = path.replace(File.separatorChar, '/');
        return configCache.computeIfAbsent(normalizedPath, p -> new Config(p, plugin));
    }

    /**
     * Reloads a specific configuration file from disk.
     * @param path The path to the configuration file.
     */
    public static void reloadConfig(String path) {
        getConfig(path).reload();
    }

    /**
     * Reloads all cached configuration files.
     */
    public static void reloadAll() {
        configCache.values().forEach(Config::reload);
    }
}