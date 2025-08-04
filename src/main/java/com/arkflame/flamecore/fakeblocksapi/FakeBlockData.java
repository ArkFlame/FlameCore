package com.arkflame.flamecore.fakeblocksapi;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Internal class to hold the state of a single fake block for a player.
 */
class FakeBlockData {
    // --- State Storage ---
    private final Location location; // NEW: Store the actual location of the block.
    private final Material originalMaterial;
    private final byte originalData;

    private Material fakeMaterial;
    private byte fakeData;
    private long creationTimestamp;
    private long durationMillis;

    @SuppressWarnings("deprecation")
    public FakeBlockData(Block originalBlock) {
        this.location = originalBlock.getLocation(); // Capture the location on creation.
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
            // THE FIX: Use the stored, correct location.
            player.sendBlockChange(this.location, fakeMaterial, fakeData);
        }
    }
    
    /**
     * Restores the block to its original state for the player.
     */
    @SuppressWarnings("deprecation")
    public void restore(Player player) {
        if (player != null && player.isOnline()) {
            // THE FIX: Use the stored, correct location.
            player.sendBlockChange(this.location, originalMaterial, originalData);
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