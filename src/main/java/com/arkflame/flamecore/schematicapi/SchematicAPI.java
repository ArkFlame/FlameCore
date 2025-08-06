package com.arkflame.flamecore.schematicapi;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.arkflame.flamecore.blocksapi.BlockWrapper;
import com.arkflame.flamecore.blocksapi.BlocksAPI;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Main entry point for the Schematic API.
 * This version is fully integrated with a queued BlocksAPI and features a robust, paced pasting system.
 */
public final class SchematicAPI {
    // How many blocks to process per tick from all schematics combined.
    // This now refers to blocks *iterated*, not just placed, to prevent lag from schematics with lots of air.
    private static final int MAX_BLOCKS_PER_TICK = 500;

    public static JavaPlugin plugin;
    private static final ConcurrentLinkedQueue<PasteTask> activePastes = new ConcurrentLinkedQueue<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            return;
        }
        plugin = pluginInstance;
        // Ensure dependent APIs are initialized.
        BlocksAPI.init(pluginInstance);
        startProcessorTask();
    }

    /**
     * Asynchronously copies a cuboid region into a new Schematic object relative to a pivot point.
     * This operation is performed on the main thread to safely access world data.
     * @param pos1 One corner of the cuboid selection.
     * @param pos2 The opposite corner of the cuboid selection.
     * @param pivot The location to use as the origin (0,0,0) of the schematic. When pasting, this point will be placed at the paste location.
     * @return A future that will complete with the created Schematic.
     */
    public static CompletableFuture<Schematic> copy(Location pos1, Location pos2, Location pivot) {
        CompletableFuture<Schematic> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = pos1.getWorld();
                int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
                int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
                int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
                int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
                int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
                int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

                int pivotX = pivot.getBlockX();
                int pivotY = pivot.getBlockY();
                int pivotZ = pivot.getBlockZ();

                List<RelativeBlockData> relativeBlocks = new java.util.ArrayList<>();
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            // This must run on the main thread.
                            Block block = world.getBlockAt(x, y, z);
                            BlockWrapper wrapper = BlocksAPI.dataHandler.capture(block);
                            relativeBlocks.add(new RelativeBlockData(
                                    x - pivotX, y - pivotY, z - pivotZ, // Use pivot for relative coords
                                    wrapper.serialize()
                            ));
                        }
                    }
                }
                // The schematic's origin is set to the pivot, which is useful for saving
                // and restoring to the exact same location.
                future.complete(new Schematic(relativeBlocks).setOrigin(pivot));
            }
        }.runTask(plugin);
        return future;
    }

    /**
     * Asynchronously copies a cuboid region into a new Schematic object.
     * The schematic's pivot point will be set to the location of {@code pos1}.
     * This operation is performed on the main thread to safely access world data.
     */
    public static CompletableFuture<Schematic> copy(Location pos1, Location pos2) {
        // Use pos1 as the default pivot for simplicity and backward compatibility.
        return copy(pos1, pos2, pos1);
    }
    
    public static CompletableFuture<Schematic> copy(Location location) {
        // For a single block, the pivot is the block's location itself.
        return copy(location, location, location);
    }
    
    /**
     * Loads a schematic from a file asynchronously and safely.
     * File I/O is async, but final object creation with Bukkit objects is on the main thread.
     */
    public static CompletableFuture<Schematic> load(File file) {
        CompletableFuture<Schematic> finalFuture = new CompletableFuture<>();

        // Step 1: Read the file into a raw data object asynchronously.
        CompletableFuture.supplyAsync(
            () -> SchematicIO.loadRaw(file),
            run -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, run)
        ).thenAccept(rawData -> {
            // Step 2: Use the raw data to create the final Schematic on the main thread.
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (rawData == null) {
                        finalFuture.complete(null);
                        return;
                    }
                    
                    Location origin = null;
                    // This is now thread-safe as it runs on the main server thread.
                    if (rawData.hasOrigin()) {
                        World world = Bukkit.getWorld(rawData.getWorldName());
                        if (world != null) {
                            origin = new Location(world, rawData.getOriginX(), rawData.getOriginY(), rawData.getOriginZ());
                        }
                    }
                    
                    finalFuture.complete(new Schematic(rawData.getBlocks()).setOrigin(origin));
                }
            }.runTask(plugin);
        });

        return finalFuture;
    }

    public static void restoreAllFromFolder(File folder, boolean deleteOnPaste) {
        if (!folder.isDirectory()) return;
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".arkschem"));
        if (files == null) return;

        for (File file : files) {
            load(file).thenAccept(schematic -> {
                if (schematic != null && schematic.getOrigin() != null) {
                    // Restore, ignoring air is often safer to not punch holes in existing terrain.
                    schematic.paste(schematic.getOrigin(), true, pasted -> {
                        if (pasted && deleteOnPaste) {
                            file.delete();
                        }
                    });
                }
            });
        }
    }
    
    // Internal method to add a new paste job to the processor.
    static void queuePaste(Schematic schematic, Location pasteLocation, boolean ignoreAir, Consumer<Boolean> callback) {
        activePastes.add(new PasteTask(schematic, pasteLocation, ignoreAir, callback));
    }

    private static void startProcessorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activePastes.isEmpty()) {
                    return;
                }

                int blocksProcessedThisTick = 0;
                Iterator<PasteTask> iterator = activePastes.iterator();

                while (iterator.hasNext() && blocksProcessedThisTick < MAX_BLOCKS_PER_TICK) {
                    PasteTask task = iterator.next();
                    
                    // Process a chunk of this specific paste task.
                    // The return value is the number of blocks considered from the schematic's list.
                    blocksProcessedThisTick += task.process(MAX_BLOCKS_PER_TICK - blocksProcessedThisTick);

                    if (task.isFinished()) {
                        // Task is complete, call its callback and remove it.
                        task.complete(plugin);
                        iterator.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}