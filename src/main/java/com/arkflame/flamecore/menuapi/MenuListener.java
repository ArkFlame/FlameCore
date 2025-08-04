package com.arkflame.flamecore.menuapi;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {
    private final MenuAnimator animator;

    public MenuListener(MenuAnimator animator) {
        this.animator = animator;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) topInventory.getHolder();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {
            return;
        }

        // --- THE FIX: Explicitly handle actions that can move items across inventories ---
        InventoryAction action = event.getAction();
        
        // Block actions that could PULL items FROM our menu when the primary click is in the player's inventory.
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
             event.setCancelled(true);
             return;
        }

        // Handle clicks within the player's own inventory
        if (clickedInventory.equals(event.getView().getBottomInventory())) {
            // Block actions that try to PUSH items INTO our menu if it's not placeable.
            // This now includes shift-clicks and number-key swaps.
            if ((action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) && !menu.isPlaceable()) {
                event.setCancelled(true);
            }
            
            // Allow all other actions (like moving items within the player inventory).
            return;
        }

        // Handle clicks inside our custom menu
        if (clickedInventory.equals(topInventory)) {
            MenuItem menuItem = menu.getMenuItem(event.getSlot());
            
            event.setCancelled(true);

            if (menuItem != null) {
                if (menuItem.isTakeable()) {
                    event.setCancelled(false);
                }
                menuItem.getClickAction().accept(event);
            } else {
                if (menu.isPlaceable()) {
                    event.setCancelled(false);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            Menu menu = (Menu) event.getView().getTopInventory().getHolder();
            if (!menu.isPlaceable()) {
                for (int slot : event.getRawSlots()) {
                    if (slot < menu.getSize()) {
                        event.setCancelled(true);
                        break;
                    }
                }
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