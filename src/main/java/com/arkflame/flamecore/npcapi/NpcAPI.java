package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class NpcAPI {
    private static JavaPlugin plugin;
    private static boolean citizensEnabled = false;
    
    // THE CORE FIX: A central, stateful map of ONLY our managed NPCs.
    private static final Map<UUID, Npc> managedNpcs = new ConcurrentHashMap<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) return;
        plugin = pluginInstance;

        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            plugin.getLogger().warning("Citizens plugin not found. NpcAPI will be disabled.");
            return;
        }
        
        citizensEnabled = true;
        plugin.getLogger().info("Successfully hooked into Citizens for NpcAPI.");
        Bukkit.getPluginManager().registerEvents(new NpcListener(), plugin);
    }

    /**
     * NEW: Gets a list of all managed NPCs within a specific radius of a location.
     * This is highly optimized as it only iterates through our known NPCs.
     *
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
            .filter(npc -> npc.getLocation().getWorld().equals(location.getWorld()))
            .filter(npc -> npc.getLocation().distanceSquared(location) <= radiusSquared)
            .collect(Collectors.toList());
    }
    
    /**
     * NEW: Finds the single closest managed NPC to a given location.
     *
     * @param location The location to search from.
     * @return An Optional containing the nearest Npc, or an empty Optional if no NPCs are found.
     */
    public static Optional<Npc> getNearest(Location location) {
        if (!isEnabled() || location.getWorld() == null) {
            return Optional.empty();
        }
        return managedNpcs.values().stream()
            .filter(Npc::isSpawned)
            .filter(npc -> npc.getLocation().getWorld().equals(location.getWorld()))
            .min(Comparator.comparingDouble(npc -> npc.getLocation().distanceSquared(location)));
    }
    
    public static boolean isEnabled() { return citizensEnabled; }

    /**
     * Gets our managed Npc wrapper for a Bukkit Entity.
     * @param entity The entity to check.
     * @return An Optional containing the Npc wrapper if the entity is a managed NPC.
     */
    public static Optional<Npc> getNpc(Entity entity) {
        if (!isEnabled() || !CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return Optional.empty();
        }
        return Optional.ofNullable(managedNpcs.get(entity.getUniqueId()));
    }

    /**
     * Gets a collection of all NPCs created and managed by THIS API.
     */
    public static Collection<Npc> getAll() {
        return Collections.unmodifiableCollection(managedNpcs.values());
    }

    /**
     * Destroys all NPCs created by this API, both persistent and temporary.
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

    // --- Internal Package-Private Methods ---
    static JavaPlugin getPlugin() { return plugin; }
    
    static void registerNpc(Npc npc) {
        if (npc != null) managedNpcs.put(npc.getUniqueId(), npc);
    }

    static void unregisterNpc(UUID npcId) {
        if (npcId != null) managedNpcs.remove(npcId);
    }
}