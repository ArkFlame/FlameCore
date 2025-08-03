package com.arkflame.core.blocksapi.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility for serializing and deserializing inventories to and from Base64 strings.
 */
class InventoryUtil {
    public static String inventoryToBase64(Inventory inventory) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(inventory.getSize());
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize inventory.", e);
        }
    }

    public static void inventoryFromBase64(String data, Inventory inventory) throws IOException, ClassNotFoundException {
        if (data == null || data.isEmpty()) return;
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        inventory.clear(); // Clear the inventory before loading new items
        
        int size = dataInput.readInt();
        for (int i = 0; i < size; i++) {
            if (i < inventory.getSize()) { // Prevent errors if inventory sizes differ
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            } else {
                dataInput.readObject(); // Discard extra items
            }
        }
        dataInput.close();
    }
}