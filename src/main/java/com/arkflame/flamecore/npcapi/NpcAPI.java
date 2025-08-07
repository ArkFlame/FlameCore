package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.configapi.ConfigAPI;
import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Main entry point for the NpcAPI.
 * This API is a high-level wrapper around the Citizens plugin, featuring
 * a fluent builder, custom file-based persistence, and simplified behavior control.
 */
public final class NpcAPI {
    private static JavaPlugin plugin;
    private static boolean citizensEnabled = false;

    // The main map, keyed by our custom flameId.
    private static final Map<UUID, Npc> managedNpcs = new ConcurrentHashMap<>();

    // *** FIX: ADDED LOOKUP MAP ***
    // This new map links the Citizens-generated UUID to our custom flameId.
    // Key: Citizens NPC UUID, Value: FlameCore NPC UUID (flameId)
    private static final Map<UUID, UUID> citizensToFlameMap = new ConcurrentHashMap<>();

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
        
        new BukkitRunnable() {
            @Override
            public void run() {
                loadNpcsFromFiles();
            }
        }.runTask(plugin);
    }
    
    public static boolean isEnabled() {
        return citizensEnabled;
    }

    /**
     * *** FIX: CORRECTED LOOKUP LOGIC ***
     * Gets our managed Npc wrapper for a Bukkit Entity, if it is one of our NPCs.
     * @param entity The entity to check.
     * @return An Optional containing the Npc wrapper.
     */
    public static Optional<Npc> getNpc(Entity entity) {
        if (!isEnabled() || entity == null || !CitizensCompat.getTemporaryNPCRegistry().isNPC(entity)) {
            return Optional.empty();
        }
        // An entity spawned by Citizens has the same UUID as the Citizens NPC object.
        // 1. Use the entity's UUID (which is the Citizens UUID) to find our flameId.
        UUID flameId = citizensToFlameMap.get(entity.getUniqueId());
        if (flameId == null) {
            return Optional.empty();
        }
        // 2. Use the flameId to get the Npc wrapper from the main map.
        return Optional.ofNullable(managedNpcs.get(flameId));
    }

    /**
     * *** FIX: CORRECTED LOOKUP LOGIC ***
     * Gets our managed Npc wrapper for a raw Citizens NPC object.
     */
    public static Optional<Npc> getNpc(NPC npc) {
        if (!isEnabled() || npc == null) return Optional.empty();
        // 1. Use the Citizens NPC's UUID to find our flameId.
        UUID flameId = citizensToFlameMap.get(npc.getUniqueId());
        if (flameId == null) {
            return Optional.empty();
        }
        // 2. Use the flameId to get the Npc wrapper from the main map.
        return Optional.ofNullable(managedNpcs.get(flameId));
    }

    /**
     * Retrieves a read-only collection of all Npc instances currently managed by the API.
     * This is useful for iterating over all custom NPCs.
     * @return A collection of all managed Npc objects.
     */
    public static Collection<Npc> getAllNpcs() {
        return Collections.unmodifiableCollection(managedNpcs.values());
    }

    public static Collection<Npc> getAll() {
        return Collections.unmodifiableCollection(managedNpcs.values());
    }

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
    
    public static Optional<Npc> getNearest(Location location) {
        if (!isEnabled() || location.getWorld() == null) {
            return Optional.empty();
        }
        return managedNpcs.values().stream()
            .filter(Npc::isSpawned)
            .filter(npc -> Objects.equals(npc.getLocation().getWorld(), location.getWorld()))
            .min(Comparator.comparingDouble(npc -> npc.getLocation().distanceSquared(location)));
    }

    public static void despawnAll() {
        getAll().forEach(Npc::despawn);
    }

    public static void destroyAll() {
        new ConcurrentHashMap<>(managedNpcs).values().forEach(Npc::destroy);
    }

    public static void destroyAllTemporary() {
        if (!isEnabled()) return;
        CitizensCompat.getTemporaryNPCRegistry().forEach(npc -> {
            Optional.ofNullable(managedNpcs.get(npc.getUniqueId())).ifPresent(Npc::destroy);
        });
    }
    
    private static void loadNpcsFromFiles() {
        if (!npcsFolder.exists()) npcsFolder.mkdirs();
        if (!npcsFolder.isDirectory()) return;
        
        File[] files = npcsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        int count = 0;
        for (File npcFile : files) {
            String fileName = npcFile.getName();
            String uuidString = fileName.substring(0, fileName.length() - 4);
            
            try {
                UUID flameId = UUID.fromString(uuidString);
                Config npcConfig = ConfigAPI.getConfig("npcs/" + fileName);
                
                if (npcConfig.getRaw().getKeys(false).isEmpty() || !npcConfig.contains("name")) {
                    plugin.getLogger().warning("Skipping malformed or empty NPC file: " + fileName);
                    continue;
                }
                
                Npc npc = NpcSerializer.deserialize(npcConfig, flameId);

                if (npc != null && npc.getInitialSpawnLocation() != null) {
                    npc.spawn();
                    count++;
                } else if (npc != null) {
                    Config.ConfigLocation wrapper = npcConfig.getLocation("location");
                    String worldName = (wrapper != null) ? wrapper.getWorldName() : "an unknown world";
                    plugin.getLogger().warning("Could not spawn persistent NPC " + npc.getName() + " because its world ('" + worldName + "') is not loaded.");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load NPC from file: " + fileName);
            }
        }
        if (count > 0) {
            plugin.getLogger().info("Loaded and spawned " + count + " persistent NPCs from files.");
        }
    }

    // --- Internal Package-Private Methods ---
    static JavaPlugin getPlugin() { return plugin; }
    static File getNpcsFolder() { return npcsFolder; }
    
    /**
     * *** FIX: UPDATED REGISTRATION LOGIC ***
     * Registers the Npc wrapper in both the main map and the lookup map.
     */
    static void registerNpc(Npc npc) {
        if (npc != null) {
            managedNpcs.put(npc.getUniqueId(), npc); // Keyed by flameId
            citizensToFlameMap.put(npc.getCitizensId(), npc.getUniqueId()); // Add entry to the lookup map
        }
    }

    /**
     * *** FIX: UPDATED UN-REGISTRATION LOGIC ***
     * Unregisters the NPC from both maps using the Citizens UUID as the entry point.
     * This is called by NpcListener, which gets the Citizens NPC from the event.
     */
    static void unregisterNpc(UUID citizensId) {
        if (citizensId != null) {
            // 1. Remove from the lookup map and get the corresponding flameId.
            UUID flameId = citizensToFlameMap.remove(citizensId);
            // 2. If a flameId was found, use it to remove from the main map.
            if (flameId != null) {
                managedNpcs.remove(flameId);
            }
        }
    }
}