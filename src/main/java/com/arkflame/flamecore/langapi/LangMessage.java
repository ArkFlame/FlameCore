package com.arkflame.flamecore.langapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.configapi.ConfigAPI;

import java.util.HashMap;
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
        this.placeholders.put("{" + key + "}", String.valueOf(value));
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
        if (LangAPI.isPapiEnabled()) {
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

    private String getRawMessage(String lang) {
        // Use our ConfigAPI to get the correct language file.
        Config langConfig = ConfigAPI.getConfig("lang/" + lang + ".yml");
        String message = langConfig.getString(key);
        
        // Fallback to default language if message is not found in the player's language
        if (message == null) {
            Config defaultConfig = ConfigAPI.getConfig("lang/" + LangAPI.getDefaultLanguage() + ".yml");
            message = defaultConfig.getString(key, "&cMissing message key: " + key + " in " + lang + ".yml");
        }
        
        return message;
    }

    private String applyPlaceholders(String message) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }
}