package com.arkflame.flamecore.menuapi;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.arkflame.flamecore.colorapi.ColorAPI;

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
    
    public ItemBuilder displayName(String name) { menuItem.addNameFrame(name); return this; }
    public ItemBuilder lore(String... lines) { menuItem.addLoreFrame(java.util.Arrays.asList(lines)); return this; }
    public ItemBuilder lore(java.util.List<String> lines) { menuItem.addLoreFrame(lines); return this; }
    public ItemBuilder takeable(boolean takeable) { menuItem.setTakeable(takeable); return this; }
    public ItemBuilder onClick(Consumer<InventoryClickEvent> action) { menuItem.setClickAction(action); return this; }
    public ItemBuilder animationInterval(int ticks) { menuItem.setAnimationInterval(ticks); return this; }
    public ItemBuilder addMaterialFrame(Material material) { menuItem.addMaterialFrame(material); return this; }
    public ItemBuilder addNameFrame(String name) { menuItem.addNameFrame(name); return this; }
    public ItemBuilder addLoreFrame(java.util.List<String> lore) { menuItem.addLoreFrame(lore); return this; }

    /**
     * NEW: Sets a static damage/durability value for the item.
     * Useful for legacy items like colored wool or wood types on 1.8-1.12.
     * This will override any damage animations.
     * @param damage The damage value.
     * @return This builder for chaining.
     */
    public ItemBuilder damage(short damage) {
        menuItem.clearDamageFrames();
        menuItem.addDamageFrame(damage);
        return this;
    }

    /**
     * NEW: Adds a damage/durability value as a frame to an animation sequence.
     * @param damage The damage value for this frame.
     * @return This builder for chaining.
     */
    public ItemBuilder addDamageFrame(short damage) {
        menuItem.addDamageFrame(damage);
        return this;
    }
    
    public MenuItem build() {
        menuItem.applyInitialFrame();
        return menuItem;
    }
}