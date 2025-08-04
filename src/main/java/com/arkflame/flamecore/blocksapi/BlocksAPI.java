package com.arkflame.flamecore.blocksapi;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.arkflame.flamecore.blocksapi.util.BlockDataHandler;
import com.arkflame.flamecore.blocksapi.util.LegacyBlockDataHandler;
import com.arkflame.flamecore.blocksapi.util.ModernBlockDataHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A powerful, version-agnostic API for capturing and restoring block states.
 * All block setting operations are processed through an internal, throttled queue
 * to prevent server lag during large-scale changes.
 */
public final class BlocksAPI {
    // --- Throttling Configuration ---
    private static final int MAX_CHECKS_PER_TICK = 200;
    private static final int MAX_SETS_PER_TICK = 50;
    
    private static JavaPlugin plugin;
    public static BlockDataHandler dataHandler;
    private static final ConcurrentLinkedQueue<BlockSetOperation> blockQueue = new ConcurrentLinkedQueue<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            return;
        }
        plugin = pluginInstance;

        try {
            Class.forName("org.bukkit.block.data.BlockData");
            dataHandler = new ModernBlockDataHandler();
        } catch (ClassNotFoundException e) {
            dataHandler = new LegacyBlockDataHandler();
        }
        
        // Start the central block processor task.
        startProcessorTask();
    }

    /**
     * Asynchronously captures the full state of a block at a given location.
     * @param location The location of the block to capture.
     * @return A CompletableFuture that will complete with a BlockWrapper object.
     */
    public static CompletableFuture<BlockWrapper> getBlockAsync(Location location) {
        CompletableFuture<BlockWrapper> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                future.complete(dataHandler.capture(location.getBlock()));
            }
        }.runTask(plugin);
        return future;
    }

    /**
     * Queues a block to be set in the world to match the state defined in a BlockWrapper.
     * The operation is handled by a central, throttled processor to ensure server stability.
     *
     * @param location The location where the block should be placed.
     * @param wrapper The BlockWrapper defining the desired state.
     */
    public static void setBlock(Location location, BlockWrapper wrapper) {
        if (location == null || wrapper == null) return;
        blockQueue.add(new BlockSetOperation(location, wrapper));
    }
    
    /**
     * A convenience method to queue a block set from its serialized string representation.
     * @param location The location where the block should be placed.
     * @param serializedData The string produced by BlockWrapper.serialize().
     */
    public static void setBlock(Location location, String serializedData) {
        BlockWrapper wrapper = BlockWrapper.deserialize(serializedData);
        if (wrapper != null) {
            setBlock(location, wrapper);
        }
    }
    
    /**
     * Starts the single, repeating task that processes the block queue.
     */
    private static void startProcessorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int checksThisTick = 0;
                int setsThisTick = 0;

                // Process the queue until it's empty or a limit is reached.
                while (!blockQueue.isEmpty() && checksThisTick < MAX_CHECKS_PER_TICK && setsThisTick < MAX_SETS_PER_TICK) {
                    BlockSetOperation operation = blockQueue.peek(); // Peek, don't remove yet.
                    if (operation == null) break;

                    // This check MUST be on the main thread.
                    if (dataHandler.needsUpdate(operation.getLocation().getBlock(), operation.getWrapper())) {
                        dataHandler.apply(operation.getLocation().getBlock(), operation.getWrapper());
                        setsThisTick++;
                    }
                    checksThisTick++;
                    
                    // Now that the operation is fully processed (checked and possibly set), remove it.
                    blockQueue.poll();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Run every tick.
    }
}