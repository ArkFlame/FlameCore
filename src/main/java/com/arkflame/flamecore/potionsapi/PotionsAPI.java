package com.arkflame.flamecore.potionsapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A modern, safe, and performant utility for managing Potion Effects on players.
 * It provides cached, case-insensitive lookups for potion effect types and thread-safe methods
 * for applying and removing effects.
 *
 * <pre>{@code
 * // Safely get a PotionEffectType
 * Optional<PotionEffectType> speedType = PotionsAPI.get("SPEED");
 *
 * // Apply an effect if the type exists
 * speedType.ifPresent(type -> {
 *     PotionsAPI.addEffect(player, type, 20 * 10, 1); // Apply Speed II for 10 seconds
 * });
 *
 * // Check if a player has an effect
 * if (PotionsAPI.hasEffect(player, "REGENERATION")) {
 *     // ...
 * }
 *
 * // Remove all effects from a player
 * PotionsAPI.removeAllEffects(player);
 * }</pre>
 */
public final class PotionsAPI {
    // Cache for both successful lookups (Optional.of(type)) and failures (Optional.empty()).
    private static final Map<String, Optional<PotionEffectType>> TYPE_CACHE = new ConcurrentHashMap<>();
    private static final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(PotionsAPI.class);

    private PotionsAPI() {
        // Private constructor to prevent instantiation of this utility class.
    }

    /**
     * Attempts to find the first valid PotionEffectType from a list of names.
     * This is the safest method for retrieving a potion type. The lookup is case-insensitive.
     *
     * @param names One or more potential potion effect type names (e.g., "SPEED", "regeneration").
     * @return An Optional containing the found PotionEffectType, or an empty Optional if none are found.
     */
    public static Optional<PotionEffectType> get(String... names) {
        for (String name : names) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            // Use a dedicated find method that handles caching
            Optional<PotionEffectType> type = findType(name);
            if (type.isPresent()) {
                return type;
            }
        }
        return Optional.empty();
    }

    /**
     * A convenience method that attempts to get a PotionEffectType, returning null if none are found.
     *
     * @param names One or more potential potion effect type names.
     * @return The found PotionEffectType, or null if all names are invalid.
     */
    public static PotionEffectType getOrNull(String... names) {
        return get(names).orElse(null);
    }

    /**
     * The core lookup logic. Checks the cache first, then Bukkit's registry.
     * Caches the result (including failures) to prevent repeated lookups.
     */
    private static Optional<PotionEffectType> findType(String name) {
        String upperName = name.toUpperCase(Locale.ROOT);

        // 1. Check our high-performance cache first.
        // The computeIfAbsent method handles the check and put atomically.
        return TYPE_CACHE.computeIfAbsent(upperName, key -> Optional.ofNullable(PotionEffectType.getByName(key)));
    }

    /**
     * Checks if a player has a specific potion effect.
     *
     * @param player The player to check.
     * @param type   The PotionEffectType to look for.
     * @return True if the player has the effect, false otherwise.
     */
    public static boolean hasEffect(Player player, PotionEffectType type) {
        return player.hasPotionEffect(type);
    }
    
    /**
     * Checks if a player has a specific potion effect by its name.
     *
     * @param player The player to check.
     * @param name   The name of the PotionEffectType.
     * @return True if a valid type is found and the player has the effect.
     */
    public static boolean hasEffect(Player player, String name) {
        return get(name).map(player::hasPotionEffect).orElse(false);
    }
    
    /**
     * Retrieves the active PotionEffect of a certain type from a player.
     *
     * @param player The player to get the effect from.
     * @param type The PotionEffectType to retrieve.
     * @return An Optional containing the PotionEffect, or an empty Optional if the player doesn't have it.
     */
    public static Optional<PotionEffect> getEffect(Player player, PotionEffectType type) {
        return Optional.ofNullable(player.getPotionEffect(type));
    }

    /**
     * Applies a potion effect to a player safely on the main server thread.
     *
     * @param player The player to apply the effect to.
     * @param effect The PotionEffect to add.
     */
    public static void addEffect(Player player, PotionEffect effect) {
        runSync(() -> player.addPotionEffect(effect));
    }

    /**
     * Creates and applies a potion effect to a player safely on the main server thread.
     *
     * @param player    The player to apply the effect to.
     * @param type      The PotionEffectType.
     * @param duration  The duration in ticks.
     * @param amplifier The amplifier (e.g., 0 for level I, 1 for level II).
     */
    public static void addEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        runSync(() -> player.addPotionEffect(new PotionEffect(type, duration, amplifier)));
    }
    
    /**
     * Removes a specific potion effect from a player safely on the main server thread.
     *
     * @param player The player to remove the effect from.
     * @param type   The PotionEffectType to remove.
     */
    public static void removeEffect(Player player, PotionEffectType type) {
        runSync(() -> player.removePotionEffect(type));
    }

    /**
     * Removes a specific potion effect from a player by its name safely on the main server thread.
     *
     * @param player The player to remove the effect from.
     * @param name   The name of the PotionEffectType to remove.
     */
    public static void removeEffect(Player player, String name) {
        get(name).ifPresent(type -> removeEffect(player, type));
    }

    /**
     * Removes all active potion effects from a player safely on the main server thread.
     *
     * @param player The player to clear all effects from.
     */
    public static void removeAllEffects(Player player) {
        runSync(() -> {
            // A copy is implicitly made by getActivePotionEffects, so this is safe.
            for (PotionEffect activeEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(activeEffect.getType());
            }
        });
    }

    /**
     * Executes a task on the main server thread.
     *
     * @param task The task to run.
     */
    private static void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            // Assumes this API is part of a plugin, which is standard.
            // JavaPlugin.getProvidingPlugin(PotionsAPI.class) is a robust way to get the instance.
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}