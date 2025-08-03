package com.arkflame.core.fakeblocksapi;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Internal class to hold the state of a single fake block for a player.
 */
class FakeBlockData {
    // Original State
    private final Material originalMaterial;
    private final byte originalData;

    // Fake State
    private Material fakeMaterial;
    private byte fakeData;
    private long creationTimestamp;
    private long durationMillis;

    @SuppressWarnings("deprecation")
    public FakeBlockData(Block originalBlock) {
        this.originalMaterial = originalBlock.getType();
        this.originalData = originalBlock.getData();
    }
    
    public void updateFakeState(FakeBlock.Builder builder) {
        this.fakeMaterial = builder.material;
        this.fakeData = builder.data;
        this.creationTimestamp = System.currentTimeMillis();
        this.durationMillis = builder.durationSeconds > 0 ? builder.durationSeconds * 1000 : -1;
    }
    
    /**
     * Sends the fake block change to the player.
     */
    @SuppressWarnings("deprecation")
    public void sendFake(Player player) {
        if (player != null && player.isOnline()) {
            player.sendBlockChange(new Location(player.getWorld(), originalMaterial.getId(), fakeData, durationMillis), fakeMaterial, fakeData);
        }
    }
    
    /**
     * Restores the block to its original state for the player.
     */
    @SuppressWarnings("deprecation")
    public void restore(Player player) {
        if (player != null && player.isOnline()) {
            player.sendBlockChange(new Location(player.getWorld(), originalMaterial.getId(), fakeData, durationMillis), originalMaterial, originalData);
        }
    }
    
    /**
     * Checks if this timed block has expired.
     */
    public boolean isExpired(long currentTime) {
        if (durationMillis < 0) { // Permanent block
            return false;
        }
        return (currentTime - creationTimestamp) >= durationMillis;
    }
}