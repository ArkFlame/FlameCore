package com.arkflame.flamecore.langapi.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * A highly efficient, version-agnostic utility to get a player's client language.
 * This class is for internal use by the LangAPI.
 */
public final class PlayerLocale {

    // --- Reflection Caches ---
    // Caches are populated on the first attempt to use them.
    private static Method getLocaleMethod;
    private static Method getHandleMethod;
    private static Method getLocaleNMSMethod; // For player.getHandle().getLocale()

    // A sentinel object to indicate that a reflection attempt has already failed.
    // This prevents repeated, expensive reflection lookups for methods that don't exist.
    private static final Object FAILED_LOOKUP = new Object();

    private PlayerLocale() {
        // Private constructor for utility class
    }

    /**
     * Gets the player's language code (e.g., "en", "es").
     * It tries the most modern and efficient methods first, falling back to reflection.
     *
     * @param player The player whose locale to get.
     * @return The 2-letter language code, or null if it cannot be determined.
     */
    public static String get(Player player) {
        if (player == null) return null;

        String locale = null;

        // --- Method 1: Modern Player#getLocale (1.12+) ---
        // This is the best and most direct method.
        if (getLocaleMethod == null) {
            try {
                getLocaleMethod = Player.class.getMethod("getLocale");
            } catch (NoSuchMethodException e) {
                // Method doesn't exist on this version, mark it as failed.
                getLocaleMethod = (Method) FAILED_LOOKUP;
            }
        }
        if (getLocaleMethod != FAILED_LOOKUP) {
            try {
                locale = (String) getLocaleMethod.invoke(player);
            } catch (Exception e) {
                // Invocation failed, should not happen but we'll fall back.
            }
        }

        // --- Method 2: Reflection on EntityPlayer (Common on older versions) ---
        if (locale == null || locale.isEmpty()) {
            if (getHandleMethod == null) {
                try {
                    getHandleMethod = player.getClass().getMethod("getHandle");
                    // Assuming the handle's class is consistent for all players.
                    getLocaleNMSMethod = getHandleMethod.getReturnType().getMethod("getLocale");
                } catch (NoSuchMethodException e) {
                    getHandleMethod = (Method) FAILED_LOOKUP;
                }
            }
            if (getHandleMethod != FAILED_LOOKUP) {
                try {
                    Object entityPlayer = getHandleMethod.invoke(player);
                    locale = (String) getLocaleNMSMethod.invoke(entityPlayer);
                } catch (Exception e) {
                    // Fallback if this method also fails.
                }
            }
        }
        
        // If a locale was found, clean it up and return the language part.
        if (locale != null && !locale.isEmpty()) {
            // "en_US" -> "en"
            String[] parts = locale.split("_");
            if (parts.length > 0) {
                return parts[0].toLowerCase();
            }
        }

        return null; // Could not determine locale.
    }
}