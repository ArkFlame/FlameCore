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
     * This version correctly loops the animation indefinitely.
     * @return The ItemStack for the new frame, or null if no visual change occurred.
     */
    public ItemStack tick() {
        if (!isAnimated()) {
            return null;
        }

        int totalFrames = getTotalFrames();
        if (totalFrames <= 1) {
            return null; // Not enough frames to animate.
        }

        // --- THIS IS THE FIX ---
        // Calculate the next frame index, ensuring it wraps around to 0.
        int nextFrameIndex = (this.currentFrame + 1) % totalFrames;
        
        ItemStack nextStack = buildFrame(nextFrameIndex);
        
        // Update the internal state to the new frame index.
        this.currentFrame = nextFrameIndex;

        // Only return the new stack if it's visually different from the old one.
        if (!nextStack.isSimilar(this.currentStack)) {
            this.currentStack = nextStack;
            return this.currentStack;
        }

        return null; // No visual update needed for this tick.
    }

    /**
     * Calculates the total number of frames in the animation, determined by the
     * longest list of frames (material, name, or lore).
     * @return The total number of animation frames.
     */
    private int getTotalFrames() {
        int max = 1; // Default to 1 to avoid division by zero errors
        if (!materialFrames.isEmpty()) max = Math.max(max, materialFrames.size());
        if (!nameFrames.isEmpty()) max = Math.max(max, nameFrames.size());
        if (!loreFrames.isEmpty()) max = Math.max(max, loreFrames.size());
        return max;
    }

    private ItemStack buildFrame(int frameIndex) {
        Material baseMaterial = getFrame(materialFrames, frameIndex, currentStack.getType());
        if (baseMaterial == null) {
            baseMaterial = Material.AIR;
        }
        ItemStack frameStack = new ItemStack(baseMaterial);
        ItemMeta meta = frameStack.getItemMeta();
        
        if (meta != null) {
            String name = getFrame(nameFrames, frameIndex, null);
            if (name != null) {
                meta.setDisplayName(ColorAPI.colorize(name).toLegacyText());
            }

            List<String> lore = getFrame(loreFrames, frameIndex, new ArrayList<>());
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
        if (frames == null || frames.isEmpty()) {
            return defaultValue;
        }
        // The modulo operator ensures the index wraps around correctly.
        return frames.get(index % frames.size());
    }
}