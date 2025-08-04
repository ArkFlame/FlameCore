package com.arkflame.flamecore.fakeblocksapi;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Internal class to hold the state of a single fake block for a player.
 */
class FakeBlockData {
    private final Location location;
    // We still store the original state in case it's needed for other logic,
    // but the restore method will no longer use it.
    private final Material originalMaterial;
    private final byte originalData;

    private Material fakeMaterial;
    private byte fakeData;
    private long creationTimestamp;
    private long durationMillis;

    @SuppressWarnings("deprecation")
    public FakeBlockData(Block originalBlock) {
        this.location = originalBlock.getLocation();
        this.originalMaterial = originalBlock.getType();
        this.originalData = originalBlock.getData();
    }
    
    public void updateFakeState(FakeBlock.Builder builder) {
        this.fakeMaterial = builder.material;
        this.fakeData = builder.data;
        this.creationTimestamp = System.currentTimeMillis();
        this.durationMillis = builder.durationSeconds > 0 ? builder.durationSeconds * 1000 : -1;
    }
    
    @SuppressWarnings("deprecation")
    public void sendFake(Player player) {
        if (player != null && player.isOnline()) {
            player.sendBlockChange(this.location, fakeMaterial, fakeData);
        }
    }
    
    /**
     * Restores the block for the player, syncing it with the CURRENT state of the block in the world.
     */
    @SuppressWarnings("deprecation")
    public void restore(Player player) {
        if (player != null && player.isOnline()) {
            // --- THE DEFINITIVE FIX ---
            // Get the block from the world at the moment of restoration.
            Block currentBlockInWorld = this.location.getBlock();
            
            // Send a block change packet using the real, current block's material and data.
            player.sendBlockChange(this.location, currentBlockInWorld.getType(), currentBlockInWorld.getData());
        }
    }
    
    public boolean isExpired(long currentTime) {
        if (durationMillis < 0) {
            return false;
        }
        return (currentTime - creationTimestamp) >= durationMillis;
    }
}