package com.arkflame.core.schematicapi;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Main entry point for the Schematic API.
 * Handles queuing, processing, and I/O for schematics.
 */
public final class SchematicAPI {
    public static JavaPlugin plugin;
    private static final ConcurrentLinkedQueue<PasteOperation> pasteQueue = new ConcurrentLinkedQueue<>();

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            throw new IllegalStateException("SchematicAPI is already initialized.");
        }
        plugin = pluginInstance;
        startProcessorTask();
    }

    /**
     * Copies a cuboid region into a new Schematic object asynchronously.
     * @param pos1 The first corner of the region.
     * @param pos2 The second corner of the region.
     * @return A CompletableFuture that will complete with the Schematic.
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

                List<SchematicBlock> blocks = new ArrayList<>();
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block block = world.getBlockAt(x, y, z);
                            blocks.add(new SchematicBlock(
                                    new Vector(x - minX, y - minY, z - minZ),
                                    block.getType().name(),
                                    block.getData()
                            ));
                        }
                    }
                }
                future.complete(new Schematic(blocks));
            }
        }.runTask(plugin);
        return future;
    }

    /**
     * Creates a schematic of a single block asynchronously.
     * @param location The location of the block.
     * @return A CompletableFuture that will complete with the Schematic.
     */
    public static CompletableFuture<Schematic> copy(Location location) {
        return copy(location, location);
    }
    
    /**
     * Loads a schematic from a file asynchronously.
     * @param file The .arkschem file to load.
     * @return A CompletableFuture that will complete with the loaded Schematic.
     */
    public static CompletableFuture<Schematic> load(File file) {
        return CompletableFuture.supplyAsync(() -> SchematicIO.load(file),
                run -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, run));
    }

    /**
     * Asynchronously loads all schematics from a folder and queues them for pasting at their origin.
     * Useful for restoring persistent block changes on startup.
     *
     * @param folder The folder to scan for .arkschem files.
     * @param deleteOnPaste If true, the schematic file will be deleted after it is successfully pasted.
     */
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

    // Internal method to add a paste job to the queue.
    static void queuePaste(Schematic schematic, Location pasteLocation, Consumer<Boolean> callback) {
        pasteQueue.add(new PasteOperation(schematic, pasteLocation, callback));
    }

    private static void startProcessorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                PasteOperation operation = pasteQueue.poll();
                if (operation == null) {
                    return; // Queue is empty
                }

                Schematic schematic = operation.getSchematic();
                Location pasteOrigin = operation.getPasteOrigin();
                World world = pasteOrigin.getWorld();
                
                // Prepare a batch of blocks to change on the main thread
                List<Runnable> blockChanges = new ArrayList<>();

                for (SchematicBlock schemBlock : schematic.getBlocks()) {
                    Vector offset = schemBlock.getRelativePosition();
                    Location blockLocation = pasteOrigin.clone().add(offset);
                    
                    blockChanges.add(() -> {
                        Block worldBlock = world.getBlockAt(blockLocation);
                        // THE OPTIMIZATION: Only update the block if it's different.
                        if (!worldBlock.getType().name().equals(schemBlock.getMaterialName()) || worldBlock.getData() != schemBlock.getData()) {
                            worldBlock.setType(schemBlock.getMaterial(), false);
                            // TODO: USE BLOCKS API, NEW API WE NEED TO CREATE!!!!
                            worldBlock.setData(schemBlock.getData(), false);
                        }
                    });
                }

                // Execute the batch of changes on the main thread
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Runnable change : blockChanges) {
                            change.run();
                        }
                        // Notify the callback that the paste is complete.
                        if(operation.getCallback() != null) {
                            operation.getCallback().accept(true);
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 1L); // Start after 1 second, run every tick.
    }
}