package com.arkflame.flamecore.schematicapi;

/**
 * A simple data class pairing a relative position with serialized block data.
 * Using integer primitives for performance and smaller memory footprint.
 */
public class RelativeBlockData {
    private final int relativeX, relativeY, relativeZ;
    private final String serializedBlockData;

    public RelativeBlockData(int relativeX, int relativeY, int relativeZ, String serializedBlockData) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.relativeZ = relativeZ;
        this.serializedBlockData = serializedBlockData;
    }

    public int getRelativeX() { return relativeX; }
    public int getRelativeY() { return relativeY; }
    public int getRelativeZ() { return relativeZ; }
    public String getSerializedBlockData() { return serializedBlockData; }
}