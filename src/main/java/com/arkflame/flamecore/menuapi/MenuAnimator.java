package com.arkflame.flamecore.menuapi;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MenuAnimator {
    private final JavaPlugin plugin;
    private final Map<Player, Menu> openMenus = new ConcurrentHashMap<>();
    private long currentTick = 0;

    public MenuAnimator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // This map will store updates ONLY for inventories that need them.
                final Map<Inventory, Map<Integer, ItemStack>> syncUpdates = new ConcurrentHashMap<>();

                // Asynchronously calculate all animation frames
                for (Map.Entry<Player, Menu> entry : openMenus.entrySet()) {
                    // Skip processing for offline players
                    if (entry.getKey() == null || !entry.getKey().isOnline()) {
                        continue;
                    }

                    Menu menu = entry.getValue();
                    for (Map.Entry<Integer, MenuItem> itemEntry : menu.getItems().entrySet()) {
                        MenuItem menuItem = itemEntry.getValue();
                        
                        // Check if it's time for this item's animation to tick.
                        if (menuItem.isAnimated() && currentTick % menuItem.getAnimationInterval() == 0) {
                            // The tick() method now only returns a new stack if it's different.
                            ItemStack newStack = menuItem.tick();
                            
                            if (newStack != null) {
                                // An update is needed. Add it to our map for processing.
                                syncUpdates.computeIfAbsent(menu.getInventory(), k -> new ConcurrentHashMap<>())
                                           .put(itemEntry.getKey(), newStack);
                            }
                        }
                    }
                }

                // --- OPTIMIZATION 1 ---
                // Only schedule the synchronous task if there are actual updates to perform.
                if (!syncUpdates.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Map.Entry<Inventory, Map<Integer, ItemStack>> updateEntry : syncUpdates.entrySet()) {
                                Inventory inv = updateEntry.getKey();
                                // Ensure the inventory is still being viewed before updating.
                                if (inv.getViewers().isEmpty()) {
                                    continue;
                                }

                                for (Map.Entry<Integer, ItemStack> itemUpdate : updateEntry.getValue().entrySet()) {
                                    int slot = itemUpdate.getKey();
                                    ItemStack newItem = itemUpdate.getValue();
                                    ItemStack currentItem = inv.getItem(slot);

                                    // --- OPTIMIZATION 2 ---
                                    // Final check: only set the item if the one in the inventory is different.
                                    // This prevents overriding changes from other plugins or race conditions.
                                    if (currentItem == null || !currentItem.isSimilar(newItem)) {
                                        inv.setItem(slot, newItem);
                                    }
                                }
                            }
                        }
                    }.runTask(plugin);
                }

                currentTick++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L); // Runs every tick
    }

    public void onMenuOpen(Player player, Menu menu) {
        openMenus.put(player, menu);
    }

    public void onMenuClose(Player player) {
        openMenus.remove(player);
    }
}