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
    
    // --- THE FIX: State flags to manage frame initialization ---
    private boolean materialFramesInitialized = false;
    private boolean nameFramesInitialized = false;
    private boolean loreFramesInitialized = false;
    
    private int currentFrame = 0;
    private int animationDirection = 1;

    public MenuItem(ItemStack itemStack) {
        this.currentStack = itemStack.clone();
        // Seed the lists with the initial state.
        this.materialFrames.add(itemStack.getType());
        ItemMeta meta = itemStack.getItemMeta();
        this.nameFrames.add(meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null);
        this.loreFrames.add(meta != null && meta.hasLore() ? meta.getLore() : new ArrayList<>());
    }
    
    // Getters & Setters are unchanged
    public ItemStack getCurrentStack() { return currentStack; }
    public boolean isTakeable() { return takeable; }
    public Consumer<InventoryClickEvent> getClickAction() { return clickAction; }
    public boolean isAnimated() { return materialFrames.size() > 1 || nameFrames.size() > 1 || loreFrames.size() > 1; }
    public int getAnimationInterval() { return animationInterval; }
    public void setTakeable(boolean takeable) { this.takeable = takeable; }
    public void setClickAction(Consumer<InventoryClickEvent> clickAction) { this.clickAction = clickAction; }
    public void setAnimationInterval(int animationInterval) { this.animationInterval = animationInterval; }

    // --- Animation Frame Management (Completely Reworked) ---
    
    public void addMaterialFrame(Material material) {
        // If this is the first time a custom material frame is added, clear the seed value.
        if (!materialFramesInitialized) {
            materialFrames.clear();
            materialFramesInitialized = true;
        }
        materialFrames.add(material);
    }
    
    public void addNameFrame(String name) {
        // If this is the first time a custom name frame is added, clear the seed value.
        if (!nameFramesInitialized) {
            nameFrames.clear();
            nameFramesInitialized = true;
        }
        nameFrames.add(name);
    }
    
    public void addLoreFrame(List<String> lore) {
        // If this is the first time a custom lore frame is added, clear the seed value.
        if (!loreFramesInitialized) {
            loreFrames.clear();
            loreFramesInitialized = true;
        }
        loreFrames.add(lore);
    }
    
    public void applyInitialFrame() {
        this.currentStack = buildFrame(0);
    }
    
    public void tick() {
        if (!isAnimated()) return;

        int totalFrames = getTotalFrames();
        if (totalFrames <= 1) return;

        int nextFrameIndex = currentFrame + animationDirection;

        if (nextFrameIndex >= totalFrames - 1) {
            nextFrameIndex = totalFrames - 1;
            animationDirection = -1;
        } else if (nextFrameIndex <= 0) {
            nextFrameIndex = 0;
            animationDirection = 1;
        }
        
        this.currentFrame = nextFrameIndex;
        this.currentStack = buildFrame(this.currentFrame);
    }

    private int getTotalFrames() {
        int max = 1;
        // Only consider lists that the user has actually added frames to.
        if (materialFramesInitialized) max = Math.max(max, materialFrames.size());
        if (nameFramesInitialized) max = Math.max(max, nameFrames.size());
        if (loreFramesInitialized) max = Math.max(max, loreFrames.size());
        return max;
    }

    private ItemStack buildFrame(int frameIndex) {
        // Use the seeded material as a fallback if no custom material frames were added.
        Material baseMaterial = getFrame(materialFrames, frameIndex, this.materialFrames.get(0));
        if (baseMaterial == null) baseMaterial = Material.AIR;
        
        ItemStack frameStack = new ItemStack(baseMaterial);
        ItemMeta meta = frameStack.getItemMeta();
        
        if (meta != null) {
            // Use the seeded name as a fallback if no custom name frames were added.
            String name = getFrame(nameFrames, frameIndex, this.nameFrames.get(0));
            if (name != null) {
                meta.setDisplayName(ColorAPI.colorize(name).toLegacyText());
            }

            // Use the seeded lore as a fallback if no custom lore frames were added.
            List<String> lore = getFrame(loreFrames, frameIndex, this.loreFrames.get(0));
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
        // This logic is now more robust. If the user didn't add custom frames, it returns the default.
        if (frames == null || frames.isEmpty()) return defaultValue;
        return frames.get(index % frames.size());
    }
}