package com.arkflame.flamecore.fakeblocksapi;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

/**
 * A comprehensive listener to ensure fake blocks are resilient to player interaction
 * and environmental changes. This is the definitive, corrected version.
 */
class FakeBlockListener implements Listener {
    private final JavaPlugin plugin;
    private static final BlockFace[] ADJACENT_FACES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    public FakeBlockListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FakeBlocksAPI.restoreAll(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation();
        FakeBlockData fakeBlock = FakeBlocksAPI.getFakeBlock(player, location);

        if (fakeBlock != null) {
            // --- THE CRITICAL FIX ---
            // 1. Immediately cancel the event at the highest priority.
            // This stops the server from processing any interaction, like placing a block
            // from the player's hand or opening a fake chest.
            event.setCancelled(true);

            // 2. Schedule the re-send task for 1 tick later to fight client-side prediction.
            resendFakeBlock(player, location);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // This handles placing blocks (like torches) ON or NEXT TO a fake block.
        Block placedBlock = event.getBlock();
        Player player = event.getPlayer();

        // Check all 6 faces adjacent to the newly placed block.
        for (BlockFace face : ADJACENT_FACES) {
            Location adjacentLocation = placedBlock.getRelative(face).getLocation();
            // If any of the adjacent blocks are fake blocks for this player,
            // we need to re-send them to prevent visual glitches.
            resendFakeBlock(player, adjacentLocation);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        checkAndResendForAllPlayers(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        checkAndResendForAllPlayers(event.getToBlock().getLocation());
    }

    private void checkAndResendForAllPlayers(Location location) {
        for (Map.Entry<UUID, Map<Location, FakeBlockData>> entry : FakeBlocksAPI.getManagedBlocks().entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && entry.getValue().containsKey(location)) {
                resendFakeBlock(player, location);
            }
        }
    }

    private void resendFakeBlock(Player player, Location location) {
        FakeBlockData fakeBlock = FakeBlocksAPI.getFakeBlock(player, location);
        if (fakeBlock == null) return;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    fakeBlock.sendFake(player);
                }
            }
        }.runTask(plugin);
    }
}