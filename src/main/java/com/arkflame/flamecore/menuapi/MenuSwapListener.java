package com.arkflame.flamecore.menuapi;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * A dedicated listener to prevent the off-hand item swap (F key) inside custom menus.
 * This listener is only registered on servers 1.9 and newer, where the event exists.
 */
public class MenuSwapListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // Check if the player has an inventory open and if it's one of our menus.
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
            // If they are in a custom menu, cancel the swap action completely.
            event.setCancelled(true);
        }
    }
}