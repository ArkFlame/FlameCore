package com.arkflame.flamecore.menuapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.arkflame.flamecore.colorapi.ColorAPI;

import java.util.HashMap;
import java.util.Map;

public class MenuBuilder {
    private String title;
    private int size;
    private final Map<Integer, MenuItem> items = new HashMap<>();
    private boolean placeable = false;
    private MenuItem backgroundItem = null;

    public MenuBuilder(int size, String title) {
        if (size % 9 != 0) {
            throw new IllegalArgumentException("Menu size must be a multiple of 9.");
        }
        this.size = size;
        this.title = title;
    }

    public MenuBuilder title(String title) {
        this.title = title;
        return this;
    }

    public MenuBuilder setItem(int slot, MenuItem item) {
        this.items.put(slot, item);
        return this;
    }

    public MenuBuilder addItem(MenuItem item) {
        for (int i = 0; i < size; i++) {
            if (!items.containsKey(i)) {
                items.put(i, item);
                break;
            }
        }
        return this;
    }

    public MenuBuilder placeable(boolean placeable) {
        this.placeable = placeable;
        return this;
    }

    public MenuBuilder background(MenuItem item) {
        this.backgroundItem = item;
        return this;
    }

    public Menu build() {
        Menu menu = new Menu(size, title, placeable);
        // Apply background first
        if (backgroundItem != null) {
            for (int i = 0; i < size; i++) {
                menu.setItem(i, backgroundItem);
            }
        }
        // Then apply specific items, overwriting the background
        items.forEach(menu::setItem);
        return menu;
    }

    public void open(Player player) {
        Menu menu = build();
        // Use the modern component-based title if available (Paper 1.16.5+)
        // For Spigot 1.8-1.21, it falls back to the legacy string.
        Inventory inventory = Bukkit.createInventory(menu, menu.getSize(), ColorAPI.colorize(menu.getTitle()).toLegacyText());
        
        for (Map.Entry<Integer, MenuItem> entry : menu.getItems().entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getCurrentStack());
        }
        
        // Now, set this fully populated inventory on our menu object
        menu.setInventory(inventory);
        
        // Register the open menu with the animator for future updates
        MenuAPI.getAnimator().onMenuOpen(player, menu);
        
        // Finally, open the now-perfect inventory for the player.
        player.openInventory(inventory);
    }
}