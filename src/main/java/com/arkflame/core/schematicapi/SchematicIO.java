package com.arkflame.core.schematicapi;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility for handling the saving and loading of schematics
 * to and from our custom, optimized binary format (.arkschem).
 */
class SchematicIO {
    private static final int FORMAT_VERSION = 1;

    public static void save(Schematic schematic, File file) {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            // Header
            dos.writeInt(FORMAT_VERSION);

            // Optional Origin Data
            Location origin = schematic.getOrigin();
            dos.writeBoolean(origin != null);
            if (origin != null) {
                dos.writeUTF(origin.getWorld().getName());
                dos.writeDouble(origin.getX());
                dos.writeDouble(origin.getY());
                dos.writeDouble(origin.getZ());
            }

            // Block Data
            List<SchematicBlock> blocks = schematic.getBlocks();
            dos.writeInt(blocks.size());
            for (SchematicBlock block : blocks) {
                dos.writeShort(block.getRelativePosition().getBlockX());
                dos.writeShort(block.getRelativePosition().getBlockY());
                dos.writeShort(block.getRelativePosition().getBlockZ());
                dos.writeUTF(block.getMaterialName());
                dos.writeByte(block.getData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Schematic load(File file) {
        if (!file.exists()) return null;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            // Header
            int version = dis.readInt();
            if (version != FORMAT_VERSION) {
                System.err.println("Schematic " + file.getName() + " has an unsupported format version!");
                return null;
            }

            // Optional Origin Data
            Location origin = null;
            if (dis.readBoolean()) {
                String worldName = dis.readUTF();
                double x = dis.readDouble();
                double y = dis.readDouble();
                double z = dis.readDouble();
                origin = new Location(Bukkit.getWorld(worldName), x, y, z);
            }

            // Block Data
            int blockCount = dis.readInt();
            List<SchematicBlock> blocks = new ArrayList<>(blockCount);
            for (int i = 0; i < blockCount; i++) {
                short x = dis.readShort();
                short y = dis.readShort();
                short z = dis.readShort();
                String materialName = dis.readUTF();
                byte data = dis.readByte();
                blocks.add(new SchematicBlock(new Vector(x, y, z), materialName, data));
            }
            
            return new Schematic(blocks).setOrigin(origin);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}