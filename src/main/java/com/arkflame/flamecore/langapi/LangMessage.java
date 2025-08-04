package com.arkflame.flamecore.langapi;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.configapi.ConfigAPI;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fluent builder for creating, formatting, and sending localized messages.
 * This object is obtained via {@link LangAPI#getMessage(String)}.
 */
public class LangMessage {
    private final String key;
    private final Map<String, String> placeholders = new HashMap<>();

    LangMessage(String key) {
        this.key = key;
    }

    /**
     * Adds a placeholder to be replaced in the message.
     * @param key The placeholder key (e.g., "{player}").
     * @param value The value to replace it with.
     * @return The current LangMessage instance for chaining.
     */
    public LangMessage with(String key, Object value) {
        // Ensure the key is wrapped with {} for consistent replacement
        this.placeholders.put("{" + key.replace("{", "").replace("}", "") + "}", String.valueOf(value));
        return this;
    }

    /**
     * Retrieves the formatted message string for a specific player.
     * This will use the player's language and parse PlaceholderAPI placeholders.
     * @param player The player to get the message for.
     * @return The fully formatted and colorized message string.
     */
    public String get(Player player) {
        String lang = LangAPI.getPlayerLanguage(player);
        String message = getRawMessage(lang);

        message = applyPlaceholders(message);
        
        // Apply PAPI placeholders if available
        if (LangAPI.isPapiEnabled() && player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        
        return message;
    }

    /**
     * Retrieves the formatted message string for the console or as a default.
     * This will use the default language and will not parse PAPI placeholders.
     * @return The formatted and colorized message string.
     */
    public String get() {
        String message = getRawMessage(LangAPI.getDefaultLanguage());
        return applyPlaceholders(message);
    }
    
    /**
     * Sends the formatted message to a CommandSender.
     * Automatically handles player-specific formatting and console formatting.
     * @param sender The player or console to send the message to.
     */
    public void send(CommandSender sender) {
        if (sender instanceof Player) {
            sender.sendMessage(get((Player) sender));
        } else {
            sender.sendMessage(get());
        }
    }

    /**
     * Fetches the message from the config, intelligently handling both String and List<String>.
     * @param lang The language code to use.
     * @return A single, newline-joined string.
     */
    private String getRawMessage(String lang) {
        Config langConfig = ConfigAPI.getConfig("lang/" + lang + ".yml");
        
        // Check if the path exists at all.
        if (!langConfig.contains(key)) {
            // Fallback to default language if the key isn't in the primary language file.
            Config defaultConfig = ConfigAPI.getConfig("lang/" + LangAPI.getDefaultLanguage() + ".yml");
            if (!defaultConfig.contains(key)) {
                return "&cMissing message key: " + key;
            }
            langConfig = defaultConfig;
        }

        // Now, check if the value at the path is a list or a single string.
        if (langConfig.getRaw().isList(key)) {
            List<String> lines = langConfig.getStringList(key); // Our ConfigAPI automatically colors this list.
            return String.join("\n", lines);
        } else {
            return langConfig.getString(key); // Already colored by ConfigAPI.
        }
    }

    private String applyPlaceholders(String message) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }
}