package com.arkflame.flamecore.menuapi;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class Menu implements InventoryHolder {
    private final int size;
    private final String title;
    private final boolean placeable;
    private final Map<Integer, MenuItem> items = new HashMap<>();
    private Inventory inventory;

    public Menu(int size, String title, boolean placeable) {
        this.size = size;
        this.title = title;
        this.placeable = placeable;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    void setInventory(Inventory inventory) { this.inventory = inventory; }
    
    public void setItem(int slot, MenuItem item) { items.put(slot, item); }
    
    public MenuItem getMenuItem(int slot) { return items.get(slot); }

    public Map<Integer, MenuItem> getItems() { return items; }
    
    public int getSize() { return size; }

    public String getTitle() { return title; }

    public boolean isPlaceable() { return placeable; }
    
    public void updateInventory() {
        if (inventory != null) {
            items.forEach((slot, item) -> inventory.setItem(slot, item.getCurrentStack()));
        }
    }
}