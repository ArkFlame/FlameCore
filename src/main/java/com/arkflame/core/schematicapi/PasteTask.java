package com.arkflame.core.schematicapi;

import com.arkflame.core.blocksapi.BlocksAPI;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

/**
 * A stateful object representing a single, ongoing paste operation.
 * It manages its own progress and ensures pasting is paced correctly.
 */
class PasteTask {
    private final Schematic schematic;
    private final Location pasteOrigin;
    private final Consumer<Boolean> callback;
    private int blockIndex = 0;

    PasteTask(Schematic schematic, Location pasteOrigin, Consumer<Boolean> callback) {
        this.schematic = schematic;
        this.pasteOrigin = pasteOrigin;
        this.callback = callback;
    }

    /**
     * Processes a batch of blocks from this paste task.
     * @param maxBlocks The maximum number of blocks to process in this call.
     * @return The number of blocks actually processed.
     */
    public int process(int maxBlocks) {
        int processedCount = 0;
        while (processedCount < maxBlocks && !isFinished()) {
            RelativeBlockData relativeBlock = schematic.getBlocks().get(blockIndex);
            
            Location blockLocation = pasteOrigin.clone().add(relativeBlock.getRelativeX(), relativeBlock.getRelativeY(), relativeBlock.getRelativeZ());
            String serializedData = relativeBlock.getSerializedBlockData();
            
            // Queue the block placement. The BlocksAPI will handle throttling and main-thread execution.
            BlocksAPI.setBlock(blockLocation, serializedData);
            
            blockIndex++;
            processedCount++;
        }
        return processedCount;
    }

    public boolean isFinished() {
        return blockIndex >= schematic.getBlocks().size();
    }

    /**
     * Safely executes the callback on the main server thread.
     * @param plugin The plugin instance to use for scheduling.
     */
    public void complete(JavaPlugin plugin) {
        if (callback != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.accept(true);
                }
            }.runTask(plugin);
        }
    }
}