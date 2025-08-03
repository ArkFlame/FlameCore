package com.arkflame.core.schematicapi;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility for handling the saving and loading of schematics.
 * This version reads and writes serialized BlockWrapper strings.
 */
class SchematicIO {
    private static final int FORMAT_VERSION = 2; // New format version!

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
                dos.writeShort(blockData.getRelativePosition().getBlockX());
                dos.writeShort(blockData.getRelativePosition().getBlockY());
                dos.writeShort(blockData.getRelativePosition().getBlockZ());
                // The magic is here: just write the full serialized string.
                dos.writeUTF(blockData.getSerializedBlockData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Schematic load(File file) {
        if (!file.exists()) return null;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int version = dis.readInt();
            if (version != FORMAT_VERSION) {
                System.err.println("Schematic " + file.getName() + " has an unsupported format version!");
                return null;
            }

            Location origin = null;
            if (dis.readBoolean()) {
                String worldName = dis.readUTF();
                double x = dis.readDouble();
                double y = dis.readDouble();
                double z = dis.readDouble();
                origin = new Location(Bukkit.getWorld(worldName), x, y, z);
            }

            int blockCount = dis.readInt();
            List<RelativeBlockData> blocks = new ArrayList<>(blockCount);
            for (int i = 0; i < blockCount; i++) {
                short x = dis.readShort();
                short y = dis.readShort();
                short z = dis.readShort();
                // Just read the full string. Deserialization happens later.
                String serializedData = dis.readUTF();
                blocks.add(new RelativeBlockData(new Vector(x, y, z), serializedData));
            }
            
            return new Schematic(blocks).setOrigin(origin);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}