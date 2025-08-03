package com.arkflame.flamecore.fakeblocksapi;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A powerful, per-player API for sending and managing fake blocks.
 * These blocks are resilient to interaction and can be set to expire after a duration.
 */
public final class FakeBlocksAPI {
    private static JavaPlugin plugin;
    private static final Map<UUID, Map<Location, FakeBlockData>> fakeBlocks = new ConcurrentHashMap<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            throw new IllegalStateException("FakeBlocksAPI is already initialized.");
        }
        plugin = pluginInstance;
        plugin.getServer().getPluginManager().registerEvents(new FakeBlockListener(), plugin);
        startTimerTask();
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

        // If a fake block doesn't already exist here, save the original state.
        if (!playerBlocks.containsKey(location)) {
            playerBlocks.put(location, new FakeBlockData(location.getBlock()));
        }

        // Get the stored original state.
        FakeBlockData originalData = playerBlocks.get(location);
        
        // Update the FakeBlockData with the new fake state and duration.
        originalData.updateFakeState(builder);
        
        // Send the change to the player.
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
                
                // Use iterators for safe removal while iterating
                Iterator<Map.Entry<UUID, Map<Location, FakeBlockData>>> playerIterator = fakeBlocks.entrySet().iterator();
                while (playerIterator.hasNext()) {
                    Map.Entry<UUID, Map<Location, FakeBlockData>> playerEntry = playerIterator.next();
                    Player player = plugin.getServer().getPlayer(playerEntry.getKey());
                    
                    // If player is offline, no need to process, but keep data for their return.
                    if (player == null || !player.isOnline()) continue;

                    Iterator<Map.Entry<Location, FakeBlockData>> blockIterator = playerEntry.getValue().entrySet().iterator();
                    while(blockIterator.hasNext()) {
                        FakeBlockData data = blockIterator.next().getValue();
                        if (data.isExpired(currentTime)) {
                            // Schedule the restoration on the main thread
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    data.restore(player);
                                }
                            }.runTask(plugin);
                            blockIterator.remove();
                        }
                    }

                    // Clean up empty player maps
                    if (playerEntry.getValue().isEmpty()) {
                        playerIterator.remove();
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 10L, 10L); // Runs every 0.5 seconds
    }
}