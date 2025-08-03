package com.arkflame.core.schematicapi;

import org.bukkit.Location;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a schematic in memory, containing a list of blocks relative to an origin.
 */
public class Schematic {
    private final List<SchematicBlock> blocks;
    private Location origin; // Optional origin, primarily used for saving/restoring

    public Schematic(List<SchematicBlock> blocks) {
        this.blocks = blocks;
    }

    public List<SchematicBlock> getBlocks() {
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
     * Queues this schematic to be pasted at a specific location.
     * The operation is handled asynchronously by the SchematicAPI.
     * @param pasteLocation The location where the schematic's corner (0,0,0) should be placed.
     */
    public void paste(Location pasteLocation) {
        paste(pasteLocation, null);
    }

    /**
     * Queues this schematic to be pasted, with a callback for completion.
     * @param pasteLocation The location to paste at.
     * @param callback A consumer that receives 'true' upon completion.
     */
    public void paste(Location pasteLocation, Consumer<Boolean> callback) {
        SchematicAPI.queuePaste(this, pasteLocation, callback);
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