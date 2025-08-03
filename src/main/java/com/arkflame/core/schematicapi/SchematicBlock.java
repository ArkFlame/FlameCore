package com.arkflame.core.schematicapi;

import org.bukkit.Material;
import org.bukkit.util.Vector;

/**
 * Represents a single block within a schematic, storing its relative position,
 * material name, and data value for maximum version compatibility.
 */
public class SchematicBlock {
    private final Vector relativePosition;
    private final String materialName;
    private final byte data;

    public SchematicBlock(Vector relativePosition, String materialName, byte data) {
        this.relativePosition = relativePosition;
        this.materialName = materialName;
        this.data = data;
    }

    public Vector getRelativePosition() {
        return relativePosition;
    }

    public String getMaterialName() {
        return materialName;
    }
    
    public Material getMaterial() {
        // Use your MaterialAPI for robust, version-agnostic lookups.
        // If you don't have it, Material.valueOf() is a simpler alternative.
        return Material.valueOf(materialName);
    }

    public byte getData() {
        return data;
    }
}