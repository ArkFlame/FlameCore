package com.arkflame.core.blocksapi.util;

import com.arkflame.core.blocksapi.BlockWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles block data for modern servers (1.13+).
 */
public class ModernBlockDataHandler implements BlockDataHandler {

    @Override
    public boolean needsUpdate(Block block, BlockWrapper wrapper) {
        // The optimization check for modern versions.
        if (wrapper.getExtraData().containsKey("blockData")) {
            BlockData newBlockData = Bukkit.createBlockData(wrapper.getExtraData().get("blockData"));
            return !block.getBlockData().equals(newBlockData);
        } else {
            // Fallback check for legacy-generated wrappers.
            return block.getType() != wrapper.getMaterial();
        }
    }

    @Override
    public BlockWrapper capture(Block block) {
        String materialName = block.getType().name();
        // Legacy data is still captured for maximum backwards compatibility if needed
        byte data = block.getData(); 
        Map<String, String> extraData = new HashMap<>();

        // Capture the full modern BlockData string. This is key for forward compatibility.
        extraData.put("blockData", block.getBlockData().getAsString());

        // Tile entity data is captured the same way as legacy
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            for (int i = 0; i < 4; i++) {
                extraData.put("line" + i, sign.getLine(i));
            }
        } else if (block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();
            if (skull.getOwningPlayer() != null) {
                extraData.put("skullOwner", skull.getOwningPlayer().getName());
            }
        } else if (block.getState() instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) block.getState();
            extraData.put("inventory", InventoryUtil.inventoryToBase64(holder.getInventory()));
        }
        
        return new BlockWrapper(materialName, data, extraData);
    }

    @Override
    public void apply(Block block, BlockWrapper wrapper) {
        Map<String, String> extraData = wrapper.getExtraData();
        
        if (extraData.containsKey("blockData")) {
            BlockData newBlockData = Bukkit.createBlockData(extraData.get("blockData"));
            // Optimization: Only set the BlockData if it has changed
            if (!block.getBlockData().equals(newBlockData)) {
                block.setBlockData(newBlockData, false);
            }
        } else {
            // Fallback for wrappers created on legacy versions
            Material material = wrapper.getMaterial();
            if (block.getType() != material) {
                block.setType(material, false);
            }
        }

        if (extraData.isEmpty()) return;

        // Apply tile entity data
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, extraData.getOrDefault("line" + i, ""));
            }
            sign.update(true, false);
        } else if (block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();
            if (extraData.containsKey("skullOwner")) {
                // Skull logic requires a bit more care on modern versions if using profiles
                // For now, setting by name is a robust fallback.
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
}