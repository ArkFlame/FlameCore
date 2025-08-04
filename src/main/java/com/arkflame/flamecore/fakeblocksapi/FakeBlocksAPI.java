package com.arkflame.flamecore.fakeblocksapi;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A powerful, per-player API for sending and managing fake blocks.
 * These blocks are resilient to interaction and can be set to expire after a duration.
 */
public final class FakeBlocksAPI {
    private static final int NEARBY_UPDATE_RADIUS_SQUARED = 10 * 10; // 10 block radius, squared for performance
    private static final int NEARBY_UPDATE_INTERVAL_TICKS = 5; // Update nearby blocks every 2 ticks

    private static JavaPlugin plugin;
    private static final Map<UUID, Map<Location, FakeBlockData>> fakeBlocks = new ConcurrentHashMap<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            return; // Already initialized
        }
        plugin = pluginInstance;
        
        // --- THE FIX ---
        // Pass the plugin instance to the listener's constructor.
        plugin.getServer().getPluginManager().registerEvents(new FakeBlockListener(plugin), plugin);
        
        startTimerTask();
        startNearbyUpdaterTask(); // Start our new proactive task
    }

    /**
     * Restores a single fake block for a player to its original state.
     * @param player The player to restore the block for.
     * @param location The location of the fake block.
     */
    public static void restore(Player player, Location location) {
        Map<Location, FakeBlockData> playerBlocks = fakeBlocks.get(player.getUniqueId());
        if (playerBlocks == null) return;

        FakeBlockData data = playerBlocks.remove(location);
        if (data != null) {
            data.restore(player);
        }
    }

    /**
     * Restores all fake blocks for a specific player.
     * @param player The player whose blocks should be restored.
     */
    public static void restoreAll(Player player) {
        Map<Location, FakeBlockData> playerBlocks = fakeBlocks.remove(player.getUniqueId());
        if (playerBlocks != null) {
            for (FakeBlockData data : playerBlocks.values()) {
                data.restore(player);
            }
        }
    }

    // --- Internal Methods ---

    /**
     * Internal method to add or update a fake block for a player.
     * @param player The player receiving the fake block.
     * @param builder The builder containing all the fake block's data.
     */
    static void send(Player player, FakeBlock.Builder builder) {
        Location location = builder.location;
        Map<Location, FakeBlockData> playerBlocks = fakeBlocks.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());

        if (!playerBlocks.containsKey(location)) {
            playerBlocks.put(location, new FakeBlockData(location.getBlock()));
        }

        FakeBlockData originalData = playerBlocks.get(location);
        
        originalData.updateFakeState(builder);
        
        originalData.sendFake(player);
    }
    
    /**
     * Gets the fake block data for a player at a specific location, if it exists.
     */
    static FakeBlockData getFakeBlock(Player player, Location location) {
        Map<Location, FakeBlockData> playerBlocks = fakeBlocks.get(player.getUniqueId());
        return playerBlocks != null ? playerBlocks.get(location) : null;
    }

    /**
     * Starts the asynchronous task that checks for and restores expired fake blocks.
     */
    private static void startTimerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fakeBlocks.isEmpty()) return;
                
                long currentTime = System.currentTimeMillis();
                
                Iterator<Map.Entry<UUID, Map<Location, FakeBlockData>>> playerIterator = fakeBlocks.entrySet().iterator();
                while (playerIterator.hasNext()) {
                    Map.Entry<UUID, Map<Location, FakeBlockData>> playerEntry = playerIterator.next();
                    Player player = plugin.getServer().getPlayer(playerEntry.getKey());
                    
                    if (player == null || !player.isOnline()) continue;

                    Iterator<Map.Entry<Location, FakeBlockData>> blockIterator = playerEntry.getValue().entrySet().iterator();
                    while(blockIterator.hasNext()) {
                        FakeBlockData data = blockIterator.next().getValue();
                        if (data.isExpired(currentTime)) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    data.restore(player);
                                }
                            }.runTask(plugin);
                            blockIterator.remove();
                        }
                    }

                    if (playerEntry.getValue().isEmpty()) {
                        playerIterator.remove();
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 10L, 10L);
    }

    /**
     * NEW: Package-private helper to allow the listener to access the managed blocks map.
     * This is crucial for checking environmental changes.
     * @return The map of all managed fake blocks.
     */
    static Map<UUID, Map<Location, FakeBlockData>> getManagedBlocks() {
        return fakeBlocks;
    }

    /**
     * NEW: Starts the proactive task that constantly reinforces the visual state
     * of nearby fake blocks to prevent them from disappearing upon interaction.
     */
    private static void startNearbyUpdaterTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fakeBlocks.isEmpty()) return;

                // This list collects all the updates we need to perform on the main thread.
                List<Runnable> syncUpdates = new ArrayList<>();

                // Iterate through all players who have fake blocks.
                for (Map.Entry<UUID, Map<Location, FakeBlockData>> entry : fakeBlocks.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) continue;

                    Location playerLocation = player.getEyeLocation();
                    Map<Location, FakeBlockData> playerBlocks = entry.getValue();

                    // For each fake block this player has, check if it's nearby.
                    for (Map.Entry<Location, FakeBlockData> blockEntry : playerBlocks.entrySet()) {
                        Location blockLocation = blockEntry.getKey();
                        
                        // Check for world difference first for efficiency
                        if (!playerLocation.getWorld().equals(blockLocation.getWorld())) continue;

                        // Use distanceSquared for a massive performance boost over distance()
                        if (playerLocation.distanceSquared(blockLocation) <= NEARBY_UPDATE_RADIUS_SQUARED) {
                            // If it's nearby, add a task to re-send the block packet.
                            syncUpdates.add(() -> blockEntry.getValue().sendFake(player));
                        }
                    }
                }

                // If we have updates to perform, run them all on the main thread.
                if (!syncUpdates.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            syncUpdates.forEach(Runnable::run);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, NEARBY_UPDATE_INTERVAL_TICKS);
    }
}