package com.arkflame.core.langapi;

import com.arkflame.core.configapi.ConfigAPI;
import com.arkflame.core.langapi.util.PlayerLocale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * The main factory for creating and managing localized messages.
 * This API handles loading language files, integrating with PlaceholderAPI,
 * and automatically detecting a player's client language.
 */
public final class LangAPI {
    private static JavaPlugin plugin;
    private static String defaultLang = "en";
    private static boolean papiEnabled = false;

    /**
     * Initializes the LangAPI. Must be called once in your plugin's onEnable method.
     * @param pluginInstance The instance of your plugin.
     */
    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            throw new IllegalStateException("LangAPI is already initialized.");
        }
        plugin = pluginInstance;
        // Ensure ConfigAPI is initialized as we depend on it.
        ConfigAPI.init(pluginInstance);

        // Safely check for PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiEnabled = true;
            plugin.getLogger().info("Successfully hooked into PlaceholderAPI.");
        }
    }

    /**
     * Sets the default language to use for the console and as a fallback.
     * @param langCode The language code (e.g., "en", "es").
     */
    public static void setDefaultLanguage(String langCode) {
        defaultLang = langCode.toLowerCase();
    }

    /**
     * Gets a localized message builder for a specific key.
     * This is the entry point for creating a fluent message.
     *
     * @param key The key of the message in the language files (e.g., "errors.no-permission").
     * @return A LangMessage builder instance.
     */
    public static LangMessage getMessage(String key) {
        if (plugin == null) {
            throw new IllegalStateException("LangAPI has not been initialized! Call LangAPI.init(plugin) in onEnable.");
        }
        return new LangMessage(key);
    }
    
    // --- Internal Methods ---
    
    /**
     * Checks if the PlaceholderAPI plugin is enabled on the server.
     * @return True if PlaceholderAPI is hooked.
     */
    static boolean isPapiEnabled() {
        return papiEnabled;
    }
    
    /**
     * Gets the language for a specific player.
     * It first tries to get the player's client language. If that fails or a language file
     * for that locale doesn't exist, it uses the server's default language.
     * @param player The player to get the language for.
     * @return The language code (e.g., "en").
     */
    static String getPlayerLanguage(Player player) {
        // Use our utility to get the player's client language.
        String playerLocale = PlayerLocale.get(player);
        
        if (playerLocale != null) {
            // Check if a language file for this locale exists in the plugin's data folder.
            File langFile = new File(plugin.getDataFolder(), "lang/" + playerLocale + ".yml");
            if (langFile.exists()) {
                return playerLocale;
            }
        }

        // Fallback to the server's default language if the player's locale is unknown or unsupported.
        return defaultLang;
    }

    /**
     * Gets the server's configured default language code.
     * @return The default language code.
     */
    static String getDefaultLanguage() {
        return defaultLang;
    }
}