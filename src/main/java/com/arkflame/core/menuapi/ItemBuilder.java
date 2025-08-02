package com.arkflame.core.menuapi;

import com.arkflame.core.colorapi.ColorAPI;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemBuilder {
    private final MenuItem menuItem;

    public ItemBuilder(Material material) {
        this.menuItem = new MenuItem(new ItemStack(material));
    }

    public ItemBuilder(ItemStack itemStack) {
        this.menuItem = new MenuItem(itemStack);
    }
    
    public ItemBuilder displayName(String name) {
        menuItem.addNameFrame(name);
        return this;
    }

    public ItemBuilder lore(String... lines) {
        menuItem.addLoreFrame(Arrays.asList(lines));
        return this;
    }
    
    public ItemBuilder lore(List<String> lines) {
        menuItem.addLoreFrame(lines);
        return this;
    }

    public ItemBuilder takeable(boolean takeable) {
        menuItem.setTakeable(takeable);
        return this;
    }

    public ItemBuilder onClick(Consumer<InventoryClickEvent> action) {
        menuItem.setClickAction(action);
        return this;
    }

    // --- Animation Methods ---

    public ItemBuilder animationInterval(int ticks) {
        menuItem.setAnimationInterval(ticks);
        return this;
    }

    public ItemBuilder addMaterialFrame(Material material) {
        menuItem.addMaterialFrame(material);
        return this;
    }

    public ItemBuilder addNameFrame(String name) {
        menuItem.addNameFrame(name);
        return this;
    }

    public ItemBuilder addLoreFrame(List<String> lore) {
        menuItem.addLoreFrame(lore);
        return this;
    }
    
    public MenuItem build() {
        // Update the initial item stack with the first frame of animation if it exists
        menuItem.updateInitialStack();
        return menuItem;
    }
}