package com.arkflame.core.schematicapi;

import org.bukkit.Location;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility for handling schematic I/O.
 * Now loads into a raw, thread-safe data object before final conversion.
 */
class SchematicIO {
    private static final int FORMAT_VERSION = 2;

    public static void save(Schematic schematic, File file) {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            dos.writeInt(FORMAT_VERSION);

            Location origin = schematic.getOrigin();
            dos.writeBoolean(origin != null);
            if (origin != null) {
                dos.writeUTF(origin.getWorld().getName());
                dos.writeDouble(origin.getX());
                dos.writeDouble(origin.getY());
                dos.writeDouble(origin.getZ());
            }

            List<RelativeBlockData> blocks = schematic.getBlocks();
            dos.writeInt(blocks.size());
            for (RelativeBlockData blockData : blocks) {
                dos.writeInt(blockData.getRelativeX());
                dos.writeInt(blockData.getRelativeY());
                dos.writeInt(blockData.getRelativeZ());
                dos.writeUTF(blockData.getSerializedBlockData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the schematic into a raw, intermediate data object that is thread-safe.
     */
    public static SchematicData loadRaw(File file) {
        if (!file.exists()) return null;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int version = dis.readInt();
            if (version != FORMAT_VERSION) {
                System.err.println("Schematic " + file.getName() + " has an unsupported format version!");
                return null;
            }
            
            SchematicData data = new SchematicData();
            if (dis.readBoolean()) {
                data.setWorldName(dis.readUTF());
                data.setOriginX(dis.readDouble());
                data.setOriginY(dis.readDouble());
                data.setOriginZ(dis.readDouble());
                data.setHasOrigin(true);
            }

            int blockCount = dis.readInt();
            List<RelativeBlockData> blocks = new ArrayList<>(blockCount);
            for (int i = 0; i < blockCount; i++) {
                int x = dis.readInt();
                int y = dis.readInt();
                int z = dis.readInt();
                String serializedData = dis.readUTF();
                blocks.add(new RelativeBlockData(x, y, z, serializedData));
            }
            data.setBlocks(blocks);
            
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * A simple, thread-safe container for raw schematic data read from a file.
     */
    static class SchematicData {
        private boolean hasOrigin = false;
        private String worldName;
        private double originX, originY, originZ;
        private List<RelativeBlockData> blocks;
        
        // Getters and Setters
        public boolean hasOrigin() { return hasOrigin; }
        public void setHasOrigin(boolean hasOrigin) { this.hasOrigin = hasOrigin; }
        public String getWorldName() { return worldName; }
        public void setWorldName(String worldName) { this.worldName = worldName; }
        public double getOriginX() { return originX; }
        public void setOriginX(double originX) { this.originX = originX; }
        public double getOriginY() { return originY; }
        public void setOriginY(double originY) { this.originY = originY; }
        public double getOriginZ() { return originZ; }
        public void setOriginZ(double originZ) { this.originZ = originZ; }
        public List<RelativeBlockData> getBlocks() { return blocks; }
        public void setBlocks(List<RelativeBlockData> blocks) { this.blocks = blocks; }
    }
}