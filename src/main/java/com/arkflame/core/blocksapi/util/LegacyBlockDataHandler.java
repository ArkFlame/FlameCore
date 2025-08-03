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
import java.util.Objects;

/**
 * Handles block data for legacy servers (1.8 - 1.12) using reflection
 * to ensure it can be compiled on modern (1.13+) APIs.
 */
public class LegacyBlockDataHandler implements BlockDataHandler {

    // --- Reflection Caches ---
    private static Method getDataMethod;
    private static Method setDataMethod; // Reflects setData(byte, boolean)

    static {
        try {
            getDataMethod = Block.class.getMethod("getData");
            setDataMethod = Block.class.getMethod("setData", byte.class, boolean.class);
        } catch (NoSuchMethodException e) {
            // Expected on non-legacy servers.
        }
    }

    @Override
    public BlockWrapper capture(Block block) {
        String materialName = block.getType().name();
        byte data = getLegacyData(block);
        Map<String, String> extraData = new HashMap<>();

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
        // First, check the physical block state.
        if (block.getType() != wrapper.getMaterial() || getLegacyData(block) != wrapper.getLegacyData()) {
            return true;
        }

        // If physical state matches, check tile entity state for changes.
        Map<String, String> wrapperExtra = wrapper.getExtraData();
        if (!wrapperExtra.isEmpty()) {
            // Capture the current block's state to compare against the wrapper's state.
            BlockWrapper currentBlockWrapper = this.capture(block);
            // If the extra data maps are not equal, an update is needed.
            if (!currentBlockWrapper.getExtraData().equals(wrapperExtra)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void apply(Block block, BlockWrapper wrapper) {
        // Set the primary block state first.
        block.setType(wrapper.getMaterial(), false);
        setLegacyData(block, wrapper.getLegacyData(), false);

        Map<String, String> extraData = wrapper.getExtraData();
        if (extraData.isEmpty()) return;

        // NOW get the state object AFTER the block type is correct. This is the fix.
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
        }
    }

    private byte getLegacyData(Block block) {
        if (getDataMethod == null) return 0;
        try {
            return (byte) getDataMethod.invoke(block);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void setLegacyData(Block block, byte data, boolean applyPhysics) {
        if (setDataMethod == null) return;
        try {
            setDataMethod.invoke(block, data, applyPhysics);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}