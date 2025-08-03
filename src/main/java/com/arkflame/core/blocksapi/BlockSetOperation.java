package com.arkflame.core.blocksapi;

import org.bukkit.Location;

/**
 * A simple data class representing a single block placement job in the queue.
 */
class BlockSetOperation {
    private final Location location;
    private final BlockWrapper wrapper;

    BlockSetOperation(Location location, BlockWrapper wrapper) {
        this.location = location;
        this.wrapper = wrapper;
    }

    public Location getLocation() {
        return location;
    }

    public BlockWrapper getWrapper() {
        return wrapper;
    }
}