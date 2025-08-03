package com.arkflame.flamecore.fakeblocksapi;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens to player events to ensure fake blocks are resilient and are cleaned up properly.
 */
class FakeBlockListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up all fake blocks for the player when they leave.
        FakeBlocksAPI.restoreAll(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();
        FakeBlockData fakeBlock = FakeBlocksAPI.getFakeBlock(player, location);

        if (fakeBlock != null) {
            // Cancel the event to prevent interaction with the real block.
            event.setCancelled(true);
            // Immediately re-send the fake block to counter any client-side prediction glitches.
            fakeBlock.sendFake(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (FakeBlocksAPI.getFakeBlock(player, location) != null) {
            // A player is trying to break a fake block. Deny it.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (FakeBlocksAPI.getFakeBlock(player, location) != null) {
            // A player is trying to place a block where a fake block exists. Deny it.
            event.setCancelled(true);
        }
    }
}