package com.arkflame.flamecore.menuapi;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.arkflame.flamecore.colorapi.ColorAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MenuItem {
    private ItemStack currentStack;
    private boolean takeable = false;
    private Consumer<InventoryClickEvent> clickAction = event -> {}; // Default empty action

    // Animation data
    private int animationInterval = 20;
    private final List<Material> materialFrames = new ArrayList<>();
    private final List<String> nameFrames = new ArrayList<>();
    private final List<List<String>> loreFrames = new ArrayList<>();
    private int currentFrame = 0;

    public MenuItem(ItemStack itemStack) {
        this.currentStack = itemStack;
        // Seed animations with the initial state
        this.materialFrames.add(itemStack.getType());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            this.nameFrames.add(meta.hasDisplayName() ? meta.getDisplayName() : null);
            this.loreFrames.add(meta.hasLore() ? meta.getLore() : new ArrayList<>());
        }
    }
    
    // --- Getters & Setters ---
    public ItemStack getCurrentStack() { return currentStack; }
    public boolean isTakeable() { return takeable; }
    public Consumer<InventoryClickEvent> getClickAction() { return clickAction; }
    public boolean isAnimated() { return materialFrames.size() > 1 || nameFrames.size() > 1 || loreFrames.size() > 1; }
    public int getAnimationInterval() { return animationInterval; }
    
    public void setTakeable(boolean takeable) { this.takeable = takeable; }
    public void setClickAction(Consumer<InventoryClickEvent> clickAction) { this.clickAction = clickAction; }
    public void setAnimationInterval(int animationInterval) { this.animationInterval = animationInterval; }

    // --- Animation Frame Management ---
    public void addMaterialFrame(Material material) { if(materialFrames.size() == 1 && materialFrames.get(0) == currentStack.getType()) materialFrames.clear(); materialFrames.add(material); }
    public void addNameFrame(String name) { if(nameFrames.size() == 1 && name.equals(nameFrames.get(0))) nameFrames.clear(); nameFrames.add(name); }
    public void addLoreFrame(List<String> lore) { if(loreFrames.size() == 1 && lore.equals(loreFrames.get(0))) loreFrames.clear(); loreFrames.add(lore); }
    
    public void updateInitialStack() {
        this.currentStack = buildFrame(0);
    }
    
    /**
     * Called by the MenuAnimator to advance the animation frame.
     * @return The ItemStack for the new frame.
     */
    public ItemStack tick() {
        if (!isAnimated()) {
            return null; // No change
        }
        currentFrame++;
        this.currentStack = buildFrame(currentFrame);
        return this.currentStack;
    }

    private ItemStack buildFrame(int frameIndex) {
        ItemStack frameStack = new ItemStack(getFrame(materialFrames, frameIndex, currentStack.getType()));
        ItemMeta meta = frameStack.getItemMeta();
        
        if (meta != null) {
            String name = getFrame(nameFrames, frameIndex, null);
            if (name != null) {
                meta.setDisplayName(ColorAPI.colorize(name).toLegacyText());
            }

            List<String> lore = getFrame(loreFrames, frameIndex, null);
            if (lore != null) {
                meta.setLore(lore.stream()
                        .map(line -> ColorAPI.colorize(line).toLegacyText())
                        .collect(Collectors.toList()));
            }
            frameStack.setItemMeta(meta);
        }
        return frameStack;
    }
    
    private <T> T getFrame(List<T> frames, int index, T defaultValue) {
        if (frames == null || frames.isEmpty()) {
            return defaultValue;
        }
        return frames.get(index % frames.size());
    }
}