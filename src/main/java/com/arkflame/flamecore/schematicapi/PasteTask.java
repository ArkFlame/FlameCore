package com.arkflame.flamecore.schematicapi;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.arkflame.flamecore.blocksapi.BlocksAPI;

import java.util.function.Consumer;

/**
 * A stateful object representing a single, ongoing paste operation.
 * It manages its own progress and ensures pasting is paced correctly.
 */
class PasteTask {
    private final Schematic schematic;
    private final Location pasteOrigin;
    private final boolean ignoreAir;
    private final Consumer<Boolean> callback;
    private int blockIndex = 0;

    PasteTask(Schematic schematic, Location pasteOrigin, boolean ignoreAir, Consumer<Boolean> callback) {
        this.schematic = schematic;
        this.pasteOrigin = pasteOrigin;
        this.ignoreAir = ignoreAir;
        this.callback = callback;
    }

    /**
     * Processes a batch of blocks from this paste task.
     * @param maxBlocksToConsider The maximum number of blocks to iterate through in this call.
     * @return The number of blocks actually processed from the schematic's list.
     */
    public int process(int maxBlocksToConsider) {
        int blocksConsidered = 0;
        while (blocksConsidered < maxBlocksToConsider && !isFinished()) {
            RelativeBlockData relativeBlock = schematic.getBlocks().get(blockIndex);
            String serializedData = relativeBlock.getSerializedBlockData();
            
            // A simple but effective check for all types of air.
            // Covers "minecraft:air", "minecraft:cave_air", "minecraft:void_air" and their blockstate variants.
            boolean isAir = serializedData.startsWith("minecraft:air") || serializedData.startsWith("minecraft:cave_air") || serializedData.startsWith("minecraft:void_air");

            if (!ignoreAir || !isAir) {
                // If we are not ignoring air, or if the block is not air, place it.
                Location blockLocation = pasteOrigin.clone().add(relativeBlock.getRelativeX(), relativeBlock.getRelativeY(), relativeBlock.getRelativeZ());
                
                // Queue the block placement. The BlocksAPI will handle throttling and main-thread execution.
                BlocksAPI.setBlock(blockLocation, serializedData);
            }
            
            blockIndex++;
            blocksConsidered++;
        }
        return blocksConsidered;
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