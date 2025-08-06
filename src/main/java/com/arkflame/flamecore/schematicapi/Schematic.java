package com.arkflame.flamecore.schematicapi;

import org.bukkit.Location;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a schematic in memory, containing a list of relative blocks.
 * This version uses serialized BlockWrapper strings for maximum compatibility.
 */
public class Schematic {
    private final List<RelativeBlockData> blocks;
    private Location origin;

    public Schematic(List<RelativeBlockData> blocks) {
        this.blocks = blocks;
    }

    public List<RelativeBlockData> getBlocks() {
        return blocks;
    }

    public Location getOrigin() {
        return origin;
    }

    public Schematic setOrigin(Location origin) {
        this.origin = origin;
        return this;
    }

    /**
     * Queues this schematic to be pasted at a specific location, not ignoring air.
     * The operation is handled asynchronously by the SchematicAPI.
     * @param pasteLocation The location where the schematic's pivot point should be placed.
     */
    public void paste(Location pasteLocation) {
        paste(pasteLocation, false, null);
    }

    /**
     * Queues this schematic to be pasted at a specific location.
     * The operation is handled asynchronously by the SchematicAPI.
     * @param pasteLocation The location where the schematic's pivot point should be placed.
     * @param ignoreAir If true, air blocks in the schematic will not be placed.
     */
    public void paste(Location pasteLocation, boolean ignoreAir) {
        paste(pasteLocation, ignoreAir, null);
    }

    /**
     * Queues this schematic to be pasted, with a callback for completion, not ignoring air.
     * @param pasteLocation The location to paste at.
     * @param callback A consumer that receives 'true' upon completion.
     */
    public void paste(Location pasteLocation, Consumer<Boolean> callback) {
        paste(pasteLocation, false, callback);
    }
    
    /**
     * Queues this schematic to be pasted, with a callback for completion.
     * @param pasteLocation The location to paste at.
     * @param ignoreAir If true, air blocks in the schematic will not be placed.
     * @param callback A consumer that receives 'true' upon completion.
     */
    public void paste(Location pasteLocation, boolean ignoreAir, Consumer<Boolean> callback) {
        SchematicAPI.queuePaste(this, pasteLocation, ignoreAir, callback);
    }
    
    /**
     * Saves this schematic to a file asynchronously using our custom binary format.
     * @param file The file to save to (e.g., new File(folder, "myarena.arkschem")).
     * @return A CompletableFuture that completes when the save is finished.
     */
    public CompletableFuture<Void> save(File file) {
        final Schematic schematicToSave = this;
        return CompletableFuture.runAsync(() -> SchematicIO.save(schematicToSave, file),
                run -> SchematicAPI.plugin.getServer().getScheduler().runTaskAsynchronously(SchematicAPI.plugin, run));
    }
}