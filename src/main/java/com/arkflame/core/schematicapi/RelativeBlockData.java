package com.arkflame.core.schematicapi;

import org.bukkit.util.Vector;

/**
 * A simple data class that pairs a relative position with a serialized BlockWrapper string.
 * This replaces the old SchematicBlock class.
 */
public class RelativeBlockData {
    private final Vector relativePosition;
    private final String serializedBlockData;

    public RelativeBlockData(Vector relativePosition, String serializedBlockData) {
        this.relativePosition = relativePosition;
        this.serializedBlockData = serializedBlockData;
    }

    public Vector getRelativePosition() {
        return relativePosition;
    }

    public String getSerializedBlockData() {
        return serializedBlockData;
    }
}