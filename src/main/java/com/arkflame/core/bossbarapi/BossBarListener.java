package com.arkflame.core.bossbarapi;

import com.arkflame.core.bossbarapi.bridge.LegacyWitherBossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Handles player events to ensure legacy Wither-based boss bars function correctly.
 * This listener is only registered on 1.8 servers.
 */
class BossBarListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BossBarAPI.create().removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // After a teleport, immediately update the position of any withers for this player.
        for (BossBarAPI bar : BossBarManager.activeBars) {
            if (bar.getPlayers().contains(event.getPlayer().getUniqueId())) {
                ((LegacyWitherBossBar) bar.getBridge()).updatePosition(event.getPlayer());
            }
        }
    }
}