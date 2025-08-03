package com.arkflame.flamecore.blocksapi.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.InventoryHolder;

import com.arkflame.flamecore.blocksapi.BlockWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles block data for modern servers (1.13+).
 */
public class ModernBlockDataHandler implements BlockDataHandler {

    @Override
    public BlockWrapper capture(Block block) {
        String materialName = block.getType().name();
        byte data = block.getData(); // Still capture for compatibility
        Map<String, String> extraData = new HashMap<>();

        extraData.put("blockData", block.getBlockData().getAsString());

        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            for (int i = 0; i < 4; i++) {
                extraData.put("line" + i, sign.getLine(i));
            }
        } else if (block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();
            if (skull.getOwningPlayer() != null && skull.getOwningPlayer().getName() != null) {
                extraData.put("skullOwner", skull.getOwningPlayer().getName());
            }
        } else if (block.getState() instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder) block.getState();
            extraData.put("inventory", InventoryUtil.inventoryToBase64(holder.getInventory()));
        }
        
        return new BlockWrapper(materialName, data, extraData);
    }

    @Override
    public boolean needsUpdate(Block block, BlockWrapper wrapper) {
        // Check physical block data first.
        if (wrapper.getExtraData().containsKey("blockData")) {
            BlockData newBlockData = Bukkit.createBlockData(wrapper.getExtraData().get("blockData"));
            if (!block.getBlockData().equals(newBlockData)) {
                return true;
            }
        } else {
            // Fallback for legacy wrappers.
            if (block.getType() != wrapper.getMaterial()) {
                return true;
            }
        }

        // If physical state is the same, check for tile entity differences.
        Map<String, String> wrapperExtra = wrapper.getExtraData();
        if (!wrapperExtra.isEmpty()) {
            BlockWrapper currentBlockWrapper = this.capture(block);
            if (!currentBlockWrapper.getExtraData().equals(wrapperExtra)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void apply(Block block, BlockWrapper wrapper) {
        // Set the primary block state first.
        if (wrapper.getExtraData().containsKey("blockData")) {
            block.setBlockData(Bukkit.createBlockData(wrapper.getExtraData().get("blockData")), false);
        } else {
            block.setType(wrapper.getMaterial(), false);
        }

        Map<String, String> extraData = wrapper.getExtraData();
        if (extraData.isEmpty()) return;

        // NOW get the state object AFTER the block type is correct.
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
}