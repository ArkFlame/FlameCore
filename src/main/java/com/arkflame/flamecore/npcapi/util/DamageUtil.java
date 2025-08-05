package com.arkflame.flamecore.npcapi.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.arkflame.flamecore.materialapi.MaterialAPI;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility to calculate 1.8-style PvP damage from an ItemStack.
 */
public final class DamageUtil {
    private static final Map<Material, Double> DAMAGE_MAP = new HashMap<>();

    static {
        // Fists
        DAMAGE_MAP.put(null, 1.0); // Represents an empty hand

        // Swords
        DAMAGE_MAP.put(MaterialAPI.getOrAir("WOOD_SWORD", "WOODEN_SWORD"), 4.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("GOLD_SWORD", "GOLDEN_SWORD"), 4.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("STONE_SWORD"), 5.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("IRON_SWORD"), 6.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("DIAMOND_SWORD"), 7.0);

        // Axes
        DAMAGE_MAP.put(MaterialAPI.getOrAir("WOOD_AXE", "WOODEN_AXE"), 3.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("GOLD_AXE", "GOLDEN_AXE"), 3.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("STONE_AXE"), 4.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("IRON_AXE"), 5.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("DIAMOND_AXE"), 6.0);

        // Pickaxes
        DAMAGE_MAP.put(MaterialAPI.getOrAir("WOOD_PICKAXE", "WOODEN_PICKAXE"), 2.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("GOLD_PICKAXE", "GOLDEN_PICKAXE"), 2.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("STONE_PICKAXE"), 3.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("IRON_PICKAXE"), 4.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("DIAMOND_PICKAXE"), 5.0);

        // Shovels
        DAMAGE_MAP.put(MaterialAPI.getOrAir("WOOD_SPADE", "WOODEN_SHOVEL"), 1.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("GOLD_SPADE", "GOLDEN_SHOVEL"), 1.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("STONE_SPADE", "STONE_SHOVEL"), 2.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("IRON_SPADE", "IRON_SHOVEL"), 3.0);
        DAMAGE_MAP.put(MaterialAPI.getOrAir("DIAMOND_SPADE", "DIAMOND_SHOVEL"), 4.0);
    }
    
    private DamageUtil() {}

    /**
     * Gets the 1.8-style damage value for a given item.
     * @param item The item to check.
     * @return The damage value. Defaults to 1.0 (fist damage).
     */
    public static double getDamage(ItemStack item) {
        if (item == null || item.getType() == MaterialAPI.getOrAir("AIR")) {
            return DAMAGE_MAP.get(null);
        }
        return DAMAGE_MAP.getOrDefault(item.getType(), 1.0);
    }
}