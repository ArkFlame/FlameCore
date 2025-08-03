package com.arkflame.core.blocksapi.util;

import com.arkflame.core.blocksapi.BlockWrapper;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.inventory.InventoryHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles block data for legacy servers (1.8 - 1.12) using reflection
 * to ensure it can be compiled on modern (1.13+) APIs.
 */
public class LegacyBlockDataHandler implements BlockDataHandler {

    // --- Reflection Caches ---
    private static Method getDataMethod;
    private static Method setDataMethod;

    static {
        // This block runs once when the class is loaded.
        // It safely finds the old methods without crashing on modern versions.
        try {
            // Find the byte-based methods: getData() and setData(byte)
            getDataMethod = Block.class.getMethod("getData");
            setDataMethod = Block.class.getMethod("setData", byte.class);
        } catch (NoSuchMethodException e) {
            // This is expected if the server is NOT a legacy version.
            // We can leave them as null; they will only be called if the Legacy handler is active.
        }
    }
    
    @Override
    public BlockWrapper capture(Block block) {
        String materialName = block.getType().name();
        byte data = getLegacyData(block);
        Map<String, String> extraData = new HashMap<>();

        // Tile entity data capturing remains the same.
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            for (int i = 0; i < 4; i++) {
                extraData.put("line" + i, sign.getLine(i));
            }
        } else if (block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();
            if (skull.hasOwner()) {
                extraData.put("skullOwner", skull.getOwner());
            }
        } else if (block.getState() instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) block.getState();
            extraData.put("inventory", InventoryUtil.inventoryToBase64(holder.getInventory()));
        } else if (block.getState() instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            if(spawner.getSpawnedType() != null) {
                extraData.put("spawnerType", spawner.getSpawnedType().name());
            }
        }

        return new BlockWrapper(materialName, data, extraData);
    }

    @Override
    public boolean needsUpdate(Block block, BlockWrapper wrapper) {
        // This is the optimization check.
        return block.getType() != wrapper.getMaterial() || getLegacyData(block) != wrapper.getLegacyData();
    }

    @Override
    public void apply(Block block, BlockWrapper wrapper) {
        Material material = wrapper.getMaterial();
        byte data = wrapper.getLegacyData();

        // Optimization: Only change the block if necessary
        if (block.getType() != material || getLegacyData(block) != data) {
            block.setType(material, false);
            setLegacyData(block, data, false);
        }

        Map<String, String> extraData = wrapper.getExtraData();
        if (extraData.isEmpty()) return;

        // Tile entity application logic remains the same.
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, extraData.getOrDefault("line" + i, ""));
            }
            sign.update(true, false);
        } else if (block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();
            if (extraData.containsKey("skullOwner")) {
                skull.setOwner(extraData.get("skullOwner"));
            }
            skull.update(true, false);
        } else if (block.getState() instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) block.getState();
            if (extraData.containsKey("inventory")) {
                try {
                    InventoryUtil.inventoryFromBase64(extraData.get("inventory"), holder.getInventory());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } // Add other tile entities as needed
    }

    /**
     * Safely gets the legacy data value of a block using reflection.
     * @param block The block to get data from.
     * @return The block's data value, or 0 if reflection fails.
     */
    private byte getLegacyData(Block block) {
        if (getDataMethod == null) return 0; // Should not happen if this handler is used correctly.
        try {
            // Invokes block.getData() and casts the result to a byte.
            return (byte) getDataMethod.invoke(block);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Safely sets the legacy data value of a block using reflection.
     * The `setData(byte, boolean)` method also needs to be reflected for physics control.
     * @param block The block to set data on.
     * @param data The data value.
     * @param applyPhysics Whether to apply physics.
     */
    private void setLegacyData(Block block, byte data, boolean applyPhysics) {
        // For simplicity and because we almost always set physics to false during schem pasting,
        // we will reflect the `setData(byte)` method. For physics control, a second
        // reflected method would be needed, but this is the most common use case.
        if (setDataMethod == null) return;
        try {
            // Invokes block.setData(data)
            setDataMethod.invoke(block, data);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}