package com.arkflame.flamecore.langapi.util;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * A highly efficient, version-agnostic utility to get a player's client language.
 * This class is for internal use by the LangAPI and is designed to be error-free.
 */
public final class PlayerLocale {

    // --- Reflection Caches ---
    // These are lazily initialized. They will hold the method if found, or be null if not.
    private static Method getLocaleBukkitMethod;
    private static Method getLocaleSpigotMethod;
    private static Method getHandleMethod;
    private static Method getLocaleNMSMethod;

    // Flags to prevent repeated, expensive reflection lookups for methods that don't exist.
    private static boolean bukkitAttempted = false;
    private static boolean spigotAttempted = false;
    private static boolean nmsAttempted = false;

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

        // --- Method 1: Modern Player#getLocale (1.12+) ---
        // This is the best and most direct method.
        if (!bukkitAttempted) {
            try {
                getLocaleBukkitMethod = Player.class.getMethod("getLocale");
            } catch (NoSuchMethodException e) {
                // Method doesn't exist on this version.
            }
            bukkitAttempted = true;
        }
        if (getLocaleBukkitMethod != null) {
            try {
                String locale = (String) getLocaleBukkitMethod.invoke(player);
                if (isValid(locale)) return clean(locale);
            } catch (Exception ignored) {}
        }

        // --- Method 2: Spigot Player.Spigot#getLocale (Common on 1.8-1.11 Spigot forks) ---
        if (!spigotAttempted) {
            try {
                getLocaleSpigotMethod = Player.Spigot.class.getMethod("getLocale");
            } catch (NoSuchMethodException e) {
                // Method doesn't exist.
            }
            spigotAttempted = true;
        }
        if (getLocaleSpigotMethod != null) {
            try {
                String locale = (String) getLocaleSpigotMethod.invoke(player.spigot());
                if (isValid(locale)) return clean(locale);
            } catch (Exception ignored) {}
        }

        // --- Method 3: NMS Reflection (Ultimate fallback for CraftBukkit and other versions) ---
        if (!nmsAttempted) {
            try {
                getHandleMethod = player.getClass().getMethod("getHandle");
                getLocaleNMSMethod = getHandleMethod.getReturnType().getDeclaredField("locale").get(getHandleMethod.invoke(player)).getClass().getMethod("getLanguage");
            } catch (Exception e) {
                // NMS structure is different or doesn't exist.
            }
            nmsAttempted = true;
        }
        if (getHandleMethod != null && getLocaleNMSMethod != null) {
            try {
                Object entityPlayer = getHandleMethod.invoke(player);
                Object localeObject = entityPlayer.getClass().getDeclaredField("locale").get(entityPlayer);
                String locale = (String) getLocaleNMSMethod.invoke(localeObject);
                if (isValid(locale)) return clean(locale);
            } catch (Exception ignored) {}
        }

        return null; // Could not determine locale through any method.
    }

    /**
     * Cleans a locale string like "en_US" to just its language code "en".
     */
    private static String clean(String locale) {
        return locale.split("_")[0].toLowerCase();
    }

    /**
     * Checks if a retrieved locale string is valid.
     */
    private static boolean isValid(String locale) {
        return locale != null && !locale.isEmpty();
    }
}