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
    // NEW: List to store damage/durability frames.
    private final List<Short> damageFrames = new ArrayList<>();
    
    private boolean materialFramesInitialized = false;
    private boolean nameFramesInitialized = false;
    private boolean loreFramesInitialized = false;
    // NEW: Initialization flag for damage frames.
    private boolean damageFramesInitialized = false;
    
    private int currentFrame = 0;
    private int animationDirection = 1;

    public MenuItem(ItemStack itemStack) {
        this.currentStack = itemStack.clone();
        // Seed all frame lists with the initial item's state.
        this.materialFrames.add(itemStack.getType());
        this.damageFrames.add(itemStack.getDurability());
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

    // --- Animation Frame Management ---
    public void addMaterialFrame(Material material) {
        if (!materialFramesInitialized) {
            materialFrames.clear();
            materialFramesInitialized = true;
        }
        materialFrames.add(material);
    }
    
    public void addNameFrame(String name) {
        if (!nameFramesInitialized) {
            nameFrames.clear();
            nameFramesInitialized = true;
        }
        nameFrames.add(name);
    }
    
    public void addLoreFrame(List<String> lore) {
        if (!loreFramesInitialized) {
            loreFrames.clear();
            loreFramesInitialized = true;
        }
        loreFrames.add(lore);
    }

    /**
     * NEW: Adds a damage value as a frame for animation.
     * @param damage The damage/durability value.
     */
    public void addDamageFrame(short damage) {
        if (!damageFramesInitialized) {
            damageFrames.clear();
            damageFramesInitialized = true;
        }
        damageFrames.add(damage);
    }

    /**
     * NEW: Clears all damage frames. Used by the ItemBuilder's static .damage() method.
     */
    public void clearDamageFrames() {
        damageFrames.clear();
        damageFramesInitialized = true; // Mark as initialized so new frames can be added.
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
        if (materialFramesInitialized) max = Math.max(max, materialFrames.size());
        if (nameFramesInitialized) max = Math.max(max, nameFrames.size());
        if (loreFramesInitialized) max = Math.max(max, loreFrames.size());
        if (damageFramesInitialized) max = Math.max(max, damageFrames.size()); // Consider damage frames
        return max;
    }

    private ItemStack buildFrame(int frameIndex) {
        Material baseMaterial = getFrame(materialFrames, frameIndex, this.materialFrames.get(0));
        if (baseMaterial == null) baseMaterial = Material.AIR;
        
        ItemStack frameStack = new ItemStack(baseMaterial);
        ItemMeta meta = frameStack.getItemMeta();
        
        // --- THE FIX: Apply the damage value for this frame ---
        short damage = getFrame(damageFrames, frameIndex, this.damageFrames.get(0));
        frameStack.setDurability(damage);
        
        if (meta != null) {
            String name = getFrame(nameFrames, frameIndex, this.nameFrames.get(0));
            if (name != null) {
                meta.setDisplayName(ColorAPI.colorize(name).toLegacyText());
            }

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
        if (frames == null || frames.isEmpty()) return defaultValue;
        return frames.get(index % frames.size());
    }
}