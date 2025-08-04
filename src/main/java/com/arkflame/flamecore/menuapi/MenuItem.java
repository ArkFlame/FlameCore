package com.arkflame.flamecore.menuapi;

import com.arkflame.flamecore.colorapi.ColorAPI;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MenuItem {
    private ItemStack currentStack;
    private boolean takeable = false;
    private Consumer<InventoryClickEvent> clickAction = event -> {};

    private int animationInterval = 20;
    private final List<Material> materialFrames = new ArrayList<>();
    private final List<String> nameFrames = new ArrayList<>();
    private final List<List<String>> loreFrames = new ArrayList<>();
    private int currentFrame = 0;
    
    // NEW: Tracks the direction of the animation (1 for forward, -1 for backward)
    private int animationDirection = 1;

    public MenuItem(ItemStack itemStack) {
        this.currentStack = itemStack.clone();
        this.materialFrames.add(itemStack.getType());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            this.nameFrames.add(meta.getDisplayName());
        } else {
            this.nameFrames.add(null);
        }
        if (meta != null && meta.hasLore()) {
            this.loreFrames.add(meta.getLore());
        } else {
            this.loreFrames.add(new ArrayList<>());
        }
    }
    
    // --- Getters & Setters (Unchanged) ---
    public ItemStack getCurrentStack() { return currentStack; }
    public boolean isTakeable() { return takeable; }
    public Consumer<InventoryClickEvent> getClickAction() { return clickAction; }
    public boolean isAnimated() { return materialFrames.size() > 1 || nameFrames.size() > 1 || loreFrames.size() > 1; }
    public int getAnimationInterval() { return animationInterval; }
    public void setTakeable(boolean takeable) { this.takeable = takeable; }
    public void setClickAction(Consumer<InventoryClickEvent> clickAction) { this.clickAction = clickAction; }
    public void setAnimationInterval(int animationInterval) { this.animationInterval = animationInterval; }

    // --- Animation Frame Management (Unchanged) ---
    private void prepareFrameList(List<?> list, Object defaultValue) {
        if (list.size() == 1 && Objects.equals(list.get(0), defaultValue)) {
            list.clear();
        }
    }
    public void addMaterialFrame(Material material) { prepareFrameList(materialFrames, currentStack.getType()); materialFrames.add(material); }
    public void addNameFrame(String name) { prepareFrameList(nameFrames, null); nameFrames.add(name); }
    public void addLoreFrame(List<String> lore) { prepareFrameList(loreFrames, new ArrayList<>()); loreFrames.add(lore); }
    
    public void applyInitialFrame() {
        this.currentStack = buildFrame(0);
    }
    
    /**
     * Called by the MenuAnimator to advance the animation frame.
     * This version implements a back-and-forth "ping-pong" animation loop.
     */
    public void tick() {
        if (!isAnimated()) return;

        int totalFrames = getTotalFrames();
        if (totalFrames <= 1) return;

        // Calculate the next frame index based on the current direction
        int nextFrameIndex = currentFrame + animationDirection;

        // Check for boundaries and reverse direction if needed
        if (nextFrameIndex >= totalFrames - 1) {
            nextFrameIndex = totalFrames - 1; // Clamp to the end
            animationDirection = -1; // Go backward next time
        } else if (nextFrameIndex <= 0) {
            nextFrameIndex = 0; // Clamp to the start
            animationDirection = 1; // Go forward next time
        }
        
        // Update the internal state
        this.currentFrame = nextFrameIndex;
        this.currentStack = buildFrame(this.currentFrame);
    }

    private int getTotalFrames() {
        int max = 1;
        if (materialFrames.size() > 1) max = Math.max(max, materialFrames.size());
        if (nameFrames.size() > 1) max = Math.max(max, nameFrames.size());
        if (loreFrames.size() > 1) max = Math.max(max, loreFrames.size());
        return max;
    }

    private ItemStack buildFrame(int frameIndex) {
        Material baseMaterial = getFrame(materialFrames, frameIndex, currentStack.getType());
        if (baseMaterial == null) baseMaterial = Material.AIR;
        
        ItemStack frameStack = new ItemStack(baseMaterial);
        ItemMeta meta = frameStack.getItemMeta();
        
        if (meta != null) {
            String name = getFrame(nameFrames, frameIndex, null);
            if (name != null) {
                meta.setDisplayName(ColorAPI.colorize(name).toLegacyText());
            }

            List<String> lore = getFrame(loreFrames, frameIndex, null);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(line -> ColorAPI.colorize(line).toLegacyText())
                        .collect(Collectors.toList()));
            }
            frameStack.setItemMeta(meta);
        }
        return frameStack;
    }
    
    private <T> T getFrame(List<T> frames, int index, T defaultValue) {
        if (frames == null || frames.isEmpty()) return defaultValue;
        return frames.get(index % frames.size());
    }
}