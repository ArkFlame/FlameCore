package com.arkflame.core.schematicapi;

import org.bukkit.Location;

import java.util.function.Consumer;

/**
 * A data-holder class representing a queued paste operation.
 */
class PasteOperation {
    private final Schematic schematic;
    private final Location pasteOrigin;
    private final Consumer<Boolean> callback;

    public PasteOperation(Schematic schematic, Location pasteOrigin, Consumer<Boolean> callback) {
        this.schematic = schematic;
        this.pasteOrigin = pasteOrigin;
        this.callback = callback;
    }

    public Schematic getSchematic() { return schematic; }
    public Location getPasteOrigin() { return pasteOrigin; }
    public Consumer<Boolean> getCallback() { return callback; }
}