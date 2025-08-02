package com.arkflame.core.menuapi;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {
    private final MenuAnimator animator;

    public MenuListener(MenuAnimator animator) {
        this.animator = animator;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) topInventory.getHolder();
        int slot = event.getRawSlot();

        // Case 1: Click is in the player's inventory
        if (slot >= menu.getSize()) {
            if (!menu.isPlaceable()) {
                event.setCancelled(true);
            }
            return;
        }

        // Case 2: Click is inside the menu
        MenuItem menuItem = menu.getMenuItem(event.getSlot());
        
        // Prevent taking items unless specified
        if (menuItem != null && !menuItem.isTakeable()) {
            event.setCancelled(true);
        }
        
        // Prevent placing items unless specified
        if (menuItem == null && !menu.isPlaceable()) {
            event.setCancelled(true);
        }

        // Execute the item's action
        if (menuItem != null && !event.isCancelled()) {
            menuItem.getClickAction().accept(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            Menu menu = (Menu) event.getView().getTopInventory().getHolder();
            if (!menu.isPlaceable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            animator.onMenuClose((Player) event.getPlayer());
        }
    }
}