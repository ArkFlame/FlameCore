package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.configapi.ConfigAPI;
import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Main entry point for the NpcAPI.
 * This API is a high-level wrapper around the Citizens plugin, featuring
 * a fluent builder, custom persistence, and simplified behavior control.
 */
public final class NpcAPI {
    private static JavaPlugin plugin;
    private static boolean citizensEnabled = false;
    private static final Map<UUID, Npc> managedNpcs = new ConcurrentHashMap<>();
    private static File npcsFolder;

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) return;
        plugin = pluginInstance;
        npcsFolder = new File(plugin.getDataFolder(), "npcs");

        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            plugin.getLogger().warning("Citizens plugin not found. NpcAPI will be disabled.");
            return;
        }
        
        citizensEnabled = true;
        plugin.getLogger().info("Successfully hooked into Citizens for NpcAPI.");
        Bukkit.getPluginManager().registerEvents(new NpcListener(), plugin);
        
        // Load all persistent NPCs from our custom config files on startup.
        loadNpcsFromFiles();
    }
    
    /**
     * Checks if the NpcAPI is enabled and functional.
     * @return True if Citizens is found on the server.
     */
    public static boolean isEnabled() {
        return citizensEnabled;
    }

    /**
     * Gets our managed Npc wrapper for a Bukkit Entity, if it is one of our NPCs.
     * @param entity The entity to check.
     * @return An Optional containing the Npc wrapper.
     */
    public static Optional<Npc> getNpc(Entity entity) {
        if (!isEnabled() || entity == null || !CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return Optional.empty();
        }
        return Optional.ofNullable(managedNpcs.get(entity.getUniqueId()));
    }

    public static Optional<Npc> getNpc(UUID uuid) {
        return Optional.ofNullable(managedNpcs.get(uuid));
    }

    public static Optional<Npc> getNpc(NPC npc) {
        return Optional.ofNullable(managedNpcs.get(npc.getUniqueId()));
    }

    /**
     * Gets a collection of all NPCs created and managed by THIS API.
     * @return An unmodifiable collection of Npc objects.
     */
    public static Collection<Npc> getAll() {
        return Collections.unmodifiableCollection(managedNpcs.values());
    }

    /**
     * Gets a list of all managed NPCs within a specific radius of a location.
     * @param location The center of the search.
     * @param radius The radius in blocks.
     * @return A list of nearby Npc objects.
     */
    public static List<Npc> getNearby(Location location, double radius) {
        if (!isEnabled() || location.getWorld() == null) {
            return Collections.emptyList();
        }
        double radiusSquared = radius * radius;
        return managedNpcs.values().stream()
            .filter(Npc::isSpawned)
            .filter(npc -> Objects.equals(npc.getLocation().getWorld(), location.getWorld()))
            .filter(npc -> npc.getLocation().distanceSquared(location) <= radiusSquared)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds the single closest managed NPC to a given location.
     * @param location The location to search from.
     * @return An Optional containing the nearest Npc.
     */
    public static Optional<Npc> getNearest(Location location) {
        if (!isEnabled() || location.getWorld() == null) {
            return Optional.empty();
        }
        return managedNpcs.values().stream()
            .filter(Npc::isSpawned)
            .filter(npc -> Objects.equals(npc.getLocation().getWorld(), location.getWorld()))
            .min(Comparator.comparingDouble(npc -> npc.getLocation().distanceSquared(location)));
    }

    /**
     * Despawns all managed NPCs without removing their data files.
     * Ideal for calling in onDisable.
     */
    public static void despawnAll() {
        getAll().forEach(Npc::despawn);
    }

    /**
     * Permanently destroys all NPCs created by this API, deleting their data files.
     */
    public static void destroyAll() {
        new ConcurrentHashMap<>(managedNpcs).values().forEach(Npc::destroy);
    }

    /**
     * Destroys only the non-persistent (temporary) NPCs created by this API.
     */
    public static void destroyAllTemporary() {
        if (!isEnabled()) return;
        CitizensCompat.getTemporaryNPCRegistry().forEach(npc -> {
            Optional.ofNullable(managedNpcs.get(npc.getUniqueId())).ifPresent(Npc::destroy);
        });
    }
    
    private static void loadNpcsFromFiles() {
        if (!npcsFolder.exists()) {
            npcsFolder.mkdirs();
        }
        if (!npcsFolder.isDirectory()) return;
        
        File[] files = npcsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        int count = 0;
        for (File npcFile : files) {
            try {
                // Use our ConfigAPI to load the file's content.
                Config npcConfig = ConfigAPI.getConfig("npcs/" + npcFile.getName());
                Npc.builderFromConfig(npcConfig).buildAndSpawn();
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load NPC from malformed file: " + npcFile.getName());
                e.printStackTrace();
            }
        }
        if (count > 0) {
            plugin.getLogger().info("Loaded " + count + " persistent NPCs from files.");
        }
    }

    // --- Internal Package-Private Methods ---
    static JavaPlugin getPlugin() { return plugin; }
    static File getNpcsFolder() { return npcsFolder; }
    
    static void registerNpc(Npc npc) {
        if (npc != null) {
            managedNpcs.put(npc.getUniqueId(), npc);
        }
    }

    static void unregisterNpc(UUID npcId) {
        if (npcId != null) {
            managedNpcs.remove(npcId);
        }
    }
}