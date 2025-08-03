package com.arkflame.flamecore.materialapi;

import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A modern, safe, and performant utility for retrieving Bukkit Materials across all server versions.
 * It handles legacy material names (pre-1.13) by automatically mapping them to their modern counterparts.
 *
 * Example:
 * <pre>{@code
 * // Safely get a material that might have a different name on older versions.
 * Material logMaterial = MaterialAPI.getOrAir("LOG", "OAK_LOG");
 *
 * // Use Optional for safer handling if a material might not exist.
 * Optional<Material> optionalSulphur = MaterialAPI.get("SULPHUR");
 * optionalSulphur.ifPresent(gunpowder -> player.getInventory().addItem(new ItemStack(gunpowder)));
 * }</pre>
 */
public final class MaterialAPI {
    // Cache for both successful lookups (Optional.of(mat)) and failures (Optional.empty()).
    private static final Map<String, Optional<Material>> MATERIAL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> LEGACY_MAPPINGS = new HashMap<>();

    static {
        // --- Populate the legacy material mappings ---

        // General & Common
        LEGACY_MAPPINGS.put("SULPHUR", "GUNPOWDER");
        LEGACY_MAPPINGS.put("GRASS", "GRASS_BLOCK");
        LEGACY_MAPPINGS.put("WORKBENCH", "CRAFTING_TABLE");
        LEGACY_MAPPINGS.put("BURNING_FURNACE", "FURNACE");
        LEGACY_MAPPINGS.put("SIGN_POST", "OAK_SIGN");
        LEGACY_MAPPINGS.put("WALL_SIGN", "OAK_WALL_SIGN");
        LEGACY_MAPPINGS.put("WATER_LILY", "LILY_PAD");
        LEGACY_MAPPINGS.put("ENCHANTMENT_TABLE", "ENCHANTING_TABLE");
        LEGACY_MAPPINGS.put("BREWING_STAND_ITEM", "BREWING_STAND");
        LEGACY_MAPPINGS.put("CAULDRON_ITEM", "CAULDRON");
        LEGACY_MAPPINGS.put("SKULL_ITEM", "PLAYER_HEAD");
        LEGACY_MAPPINGS.put("SKULL", "PLAYER_HEAD");
        LEGACY_MAPPINGS.put("MONSTER_EGG", "INFESTED_STONE");
        LEGACY_MAPPINGS.put("MYCEL", "MYCELIUM");
        
        // Wood & Logs
        LEGACY_MAPPINGS.put("WOOD", "OAK_WOOD");
        LEGACY_MAPPINGS.put("LOG", "OAK_LOG");
        LEGACY_MAPPINGS.put("LOG_2", "ACACIA_LOG");
        LEGACY_MAPPINGS.put("LEAVES", "OAK_LEAVES");
        LEGACY_MAPPINGS.put("LEAVES_2", "ACACIA_LEAVES");
        LEGACY_MAPPINGS.put("FENCE", "OAK_FENCE");
        LEGACY_MAPPINGS.put("WOOD_STAIRS", "OAK_STAIRS");
        LEGACY_MAPPINGS.put("SPRUCE_WOOD_STAIRS", "SPRUCE_STAIRS");
        LEGACY_MAPPINGS.put("BIRCH_WOOD_STAIRS", "BIRCH_STAIRS");
        LEGACY_MAPPINGS.put("JUNGLE_WOOD_STAIRS", "JUNGLE_STAIRS");
        LEGACY_MAPPINGS.put("WOOD_STEP", "OAK_SLAB");
        LEGACY_MAPPINGS.put("WOOD_DOUBLE_STEP", "OAK_SLAB");
        LEGACY_MAPPINGS.put("TRAP_DOOR", "OAK_TRAPDOOR");
        LEGACY_MAPPINGS.put("WOOD_DOOR", "OAK_DOOR");
        LEGACY_MAPPINGS.put("WOODEN_DOOR", "OAK_DOOR");
        LEGACY_MAPPINGS.put("BOAT", "OAK_BOAT");
        LEGACY_MAPPINGS.put("SIGN", "OAK_SIGN");
        
        // Nether
        LEGACY_MAPPINGS.put("NETHER_STALK", "NETHER_WART");
        LEGACY_MAPPINGS.put("NETHER_WARTS", "NETHER_WART");
        LEGACY_MAPPINGS.put("NETHER_FENCE", "NETHER_BRICK_FENCE");
        LEGACY_MAPPINGS.put("QUARTZ_ORE", "NETHER_QUARTZ_ORE");
        LEGACY_MAPPINGS.put("PORTAL", "NETHER_PORTAL");
        LEGACY_MAPPINGS.put("ENDER_STONE", "END_STONE");
        
        // Redstone & Rails
        LEGACY_MAPPINGS.put("DIODE", "REPEATER");
        LEGACY_MAPPINGS.put("REDSTONE_COMPARATOR", "COMPARATOR");
        LEGACY_MAPPINGS.put("REDSTONE_TORCH_OFF", "REDSTONE_TORCH");
        LEGACY_MAPPINGS.put("REDSTONE_TORCH_ON", "REDSTONE_TORCH");
        LEGACY_MAPPINGS.put("REDSTONE_LAMP_OFF", "REDSTONE_LAMP");
        LEGACY_MAPPINGS.put("REDSTONE_LAMP_ON", "REDSTONE_LAMP");
        LEGACY_MAPPINGS.put("GOLD_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE");
        LEGACY_MAPPINGS.put("IRON_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE");
        
        // Foods & Crops
        LEGACY_MAPPINGS.put("SPECKLED_MELON", "GLISTERING_MELON_SLICE");
        LEGACY_MAPPINGS.put("CARROT_ITEM", "CARROT");
        LEGACY_MAPPINGS.put("POTATO_ITEM", "POTATO");
        LEGACY_MAPPINGS.put("CROPS", "WHEAT");
        LEGACY_MAPPINGS.put("SOIL", "FARMLAND");
        LEGACY_MAPPINGS.put("SEEDS", "WHEAT_SEEDS");
        
        // Dyes, Wool, Glass, Terracotta
        LEGACY_MAPPINGS.put("INK_SACK", "INK_SAC");
        LEGACY_MAPPINGS.put("WOOL", "WHITE_WOOL");
        LEGACY_MAPPINGS.put("CARPET", "WHITE_CARPET");
        LEGACY_MAPPINGS.put("STAINED_GLASS", "WHITE_STAINED_GLASS");
        LEGACY_MAPPINGS.put("STAINED_GLASS_PANE", "WHITE_STAINED_GLASS_PANE");
        LEGACY_MAPPINGS.put("STAINED_CLAY", "WHITE_TERRACOTTA");
        LEGACY_MAPPINGS.put("HARD_CLAY", "TERRACOTTA");
        
        // Tools & Armor
        LEGACY_MAPPINGS.put("WOOD_SWORD", "WOODEN_SWORD");
        LEGACY_MAPPINGS.put("WOOD_SPADE", "WOODEN_SHOVEL");
        LEGACY_MAPPINGS.put("WOOD_PICKAXE", "WOODEN_PICKAXE");
        LEGACY_MAPPINGS.put("WOOD_AXE", "WOODEN_AXE");
        LEGACY_MAPPINGS.put("WOOD_HOE", "WOODEN_HOE");
    }

    private MaterialAPI() {
        // Private constructor to prevent instantiation of this utility class.
    }

    /**
     * Attempts to find the first valid Material from a list of names.
     * This is the safest method, as it makes no assumptions about the material existing.
     *
     * @param names One or more potential material names (e.g., "LOG", "OAK_LOG").
     * @return An Optional containing the found Material, or an empty Optional if none are found.
     */
    public static Optional<Material> get(String... names) {
        for (String name : names) {
            if (name == null || name.isEmpty()) continue;
            Optional<Material> material = findMaterial(name);
            if (material.isPresent()) {
                return material;
            }
        }
        return Optional.empty();
    }
    
    /**
     * A convenience method that attempts to get a Material, returning Material.AIR if none are found.
     *
     * @param names One or more potential material names.
     * @return The found Material, or Material.AIR if all names are invalid.
     */
    public static Material getOrAir(String... names) {
        return get(names).orElse(Material.AIR);
    }
    
    /**
     * A convenience method that attempts to get a Material, returning null if none are found.
     *
     * @param names One or more potential material names.
     * @return The found Material, or null if all names are invalid.
     */
    public static Material getOrNull(String... names) {
        return get(names).orElse(null);
    }

    /**
     * The core lookup logic. Checks cache, then modern names, then legacy names.
     * Caches the result (including failures) to prevent repeated lookups.
     */
    private static Optional<Material> findMaterial(String name) {
        String upperName = name.toUpperCase(Locale.ROOT);

        // 1. Check our high-performance cache first.
        if (MATERIAL_CACHE.containsKey(upperName)) {
            return MATERIAL_CACHE.get(upperName);
        }

        // 2. Try to match the name directly.
        Material directMatch = Material.getMaterial(upperName);
        if (directMatch != null) {
            MATERIAL_CACHE.put(upperName, Optional.of(directMatch));
            return Optional.of(directMatch);
        }

        // 3. If direct match fails, check our legacy mappings.
        String modernName = LEGACY_MAPPINGS.get(upperName);
        if (modernName != null) {
            Material legacyMatch = Material.getMaterial(modernName);
            if (legacyMatch != null) {
                Optional<Material> result = Optional.of(legacyMatch);
                // Cache the result for both the legacy and modern names to speed up future lookups.
                MATERIAL_CACHE.put(upperName, result);
                MATERIAL_CACHE.put(modernName, result);
                return result;
            }
        }

        // 4. If all lookups fail, cache the failure and return empty.
        MATERIAL_CACHE.put(upperName, Optional.empty());
        return Optional.empty();
    }

    /**
     * Checks if a material is a type of armor.
     * @param material The material to check.
     * @return True if the material is a helmet, chestplate, leggings, or boots.
     */
    public static boolean isArmor(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    /**
     * Checks if a material is a type of tool or weapon.
     * @param material The material to check.
     * @return True if the material is a sword, axe, pickaxe, shovel, hoe, bow, etc.
     */
    public static boolean isTool(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE") || name.endsWith("_HOE") || name.endsWith("_SHOVEL")
               || material == Material.FISHING_ROD || material == Material.FLINT_AND_STEEL || material == Material.SHEARS || material == Material.BOW;
    }
}