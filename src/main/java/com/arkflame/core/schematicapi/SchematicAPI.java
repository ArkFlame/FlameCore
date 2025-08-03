package com.arkflame.core.schematicapi;

import com.arkflame.core.blocksapi.BlocksAPI;
import com.arkflame.core.blocksapi.BlockWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    private static final int MAX_BLOCKS_PER_TICK = 500; // How many blocks to process per tick from all schematics combined.

    public static JavaPlugin plugin;
    private static final ConcurrentLinkedQueue<PasteTask> activePastes = new ConcurrentLinkedQueue<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            throw new IllegalStateException("SchematicAPI is already initialized.");
        }
        plugin = pluginInstance;
        // Ensure dependent APIs are initialized.
        BlocksAPI.init(pluginInstance);
        startProcessorTask();
    }

    /**
     * Asynchronously copies a cuboid region into a new Schematic object.
     * This operation is performed on the main thread to safely access world data.
     */
    public static CompletableFuture<Schematic> copy(Location pos1, Location pos2) {
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

                List<RelativeBlockData> relativeBlocks = new java.util.ArrayList<>();
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            // This must run on the main thread.
                            Block block = world.getBlockAt(x, y, z);
                            BlockWrapper wrapper = BlocksAPI.dataHandler.capture(block);
                            relativeBlocks.add(new RelativeBlockData(
                                    x - minX, y - minY, z - minZ,
                                    wrapper.serialize()
                            ));
                        }
                    }
                }
                future.complete(new Schematic(relativeBlocks));
            }
        }.runTask(plugin);
        return future;
    }
    
    public static CompletableFuture<Schematic> copy(Location location) {
        return copy(location, location);
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
                    schematic.paste(schematic.getOrigin(), pasted -> {
                        if (pasted && deleteOnPaste) {
                            file.delete();
                        }
                    });
                }
            });
        }
    }
    
    // Internal method to add a new paste job to the processor.
    static void queuePaste(Schematic schematic, Location pasteLocation, Consumer<Boolean> callback) {
        activePastes.add(new PasteTask(schematic, pasteLocation, callback));
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