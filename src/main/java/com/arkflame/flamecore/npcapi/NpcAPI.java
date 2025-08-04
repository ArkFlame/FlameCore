package com.arkflame.flamecore.npcapi;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for the NpcAPI.
 * This API is a high-level wrapper around the Citizens plugin.
 */
public final class NpcAPI {
    private static JavaPlugin plugin;
    private static boolean citizensEnabled = false;
    private static final Map<UUID, Npc> npcMap = new ConcurrentHashMap<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) return;
        plugin = pluginInstance;

        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            citizensEnabled = true;
            plugin.getLogger().info("Successfully hooked into Citizens for NpcAPI.");
            // We can add a task here later if needed for managing complex behaviors.
        } else {
            plugin.getLogger().warning("Citizens plugin not found. NpcAPI will be disabled.");
        }
    }
    
    /**
     * Checks if the NpcAPI is enabled and functional.
     * @return True if Citizens is found on the server.
     */
    public static boolean isEnabled() {
        return citizensEnabled;
    }

    /**
     * Gets our custom Npc wrapper for a Citizens NPC.
     * @param citizensNpc The raw Citizens NPC.
     * @return The Npc wrapper, or null if the input is null.
     */
    public static Npc getNpc(NPC citizensNpc) {
        if (citizensNpc == null) return null;
        return npcMap.get(citizensNpc.getUniqueId());
    }

    // --- Internal Package-Private Methods ---

    static JavaPlugin getPlugin() {
        return plugin;
    }

    static void registerNpc(Npc npc) {
        if (npc != null) {
            npcMap.put(npc.getUniqueId(), npc);
        }
    }

    static void unregisterNpc(Npc npc) {
        if (npc != null) {
            npcMap.remove(npc.getUniqueId());
        }
    }
}