package com.arkflame.core.fakeblocksapi;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * A fluent builder for creating and sending fake blocks to players.
 */
public final class FakeBlock {

    private FakeBlock() {}

    /**
     * Starts building a new fake block.
     * @param location The location where the fake block will appear.
     * @param material The material of the fake block.
     * @return A new builder instance.
     */
    public static Builder builder(Location location, Material material) {
        return new Builder(location, material);
    }

    public static class Builder {
        final Location location;
        final Material material;
        byte data = 0;
        long durationSeconds = -1; // -1 means permanent

        Builder(Location location, Material material) {
            this.location = location;
            this.material = material;
        }

        /**
         * Sets the legacy data value for this fake block (e.g., wool color).
         * @param data The data value.
         * @return This builder for chaining.
         */
        public Builder data(byte data) {
            this.data = data;
            return this;
        }

        /**
         * Sets a duration for this fake block. After this time, it will be automatically restored.
         * @param seconds The duration in seconds.
         * @return This builder for chaining.
         */
        public Builder duration(long seconds) {
            this.durationSeconds = seconds;
            return this;
        }

        /**
         * Sends the configured fake block to the specified player.
         * @param player The player who will see the fake block.
         */
        public void send(Player player) {
            FakeBlocksAPI.send(player, this);
        }
    }
}