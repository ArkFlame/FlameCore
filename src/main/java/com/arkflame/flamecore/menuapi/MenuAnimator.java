package com.arkflame.flamecore.menuapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuAnimator {
    private final JavaPlugin plugin;
    // THE FIX: Store UUID instead of Player to prevent memory leaks and invalid object issues.
    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();
    private long currentTick = 0;

    public MenuAnimator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // The tick should always increment, regardless of whether menus are open.
                currentTick++;
                if (openMenus.isEmpty()) {
                    return;
                }
                
                final Map<Inventory, Map<Integer, ItemStack>> potentialUpdates = new ConcurrentHashMap<>();

                // Asynchronously determine which items need to be ticked.
                for (Map.Entry<UUID, Menu> entry : openMenus.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    
                    // Safely skip if the player is offline.
                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    Menu menu = entry.getValue();
                    for (Map.Entry<Integer, MenuItem> itemEntry : menu.getItems().entrySet()) {
                        MenuItem menuItem = itemEntry.getValue();
                        
                        // --- THE CRITICAL FIX FOR THE REPORTED BUG ---
                        // We now correctly get the item's specific interval every time.
                        int interval = menuItem.getAnimationInterval();
                        
                        // Check if it's time for this item's animation to tick.
                        if (menuItem.isAnimated() && interval > 0 && currentTick % interval == 0) {
                            // Tick the item to update its internal state to the next frame.
                            menuItem.tick();
                            // Add the new state to our map of potential updates.
                            potentialUpdates.computeIfAbsent(menu.getInventory(), k -> new ConcurrentHashMap<>())
                                          .put(itemEntry.getKey(), menuItem.getCurrentStack());
                        }
                    }
                }

                // If any items were ticked, schedule a synchronous task to apply the visual changes.
                if (!potentialUpdates.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Map.Entry<Inventory, Map<Integer, ItemStack>> updateEntry : potentialUpdates.entrySet()) {
                                Inventory inv = updateEntry.getKey();
                                // Double-check viewers to be safe.
                                if (inv.getViewers().isEmpty()) {
                                    continue;
                                }

                                for (Map.Entry<Integer, ItemStack> itemUpdate : updateEntry.getValue().entrySet()) {
                                    int slot = itemUpdate.getKey();
                                    ItemStack newItem = itemUpdate.getValue();
                                    ItemStack currentItem = inv.getItem(slot);

                                    // The final, authoritative check on the main thread.
                                    if (currentItem == null || !currentItem.isSimilar(newItem)) {
                                        inv.setItem(slot, newItem);
                                        for (HumanEntity viewer : inv.getViewers()) {
                                            if (!(viewer instanceof Player)) {
                                                continue;
                                            }
                                            ((Player) viewer).updateInventory();
                                        }
                                    }
                                }
                            }
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L); // Run the main loop every single tick.
    }
    
    public void onMenuOpen(Player player, Menu menu) {
        // Store by UUID
        openMenus.put(player.getUniqueId(), menu);
    }

    public void onMenuClose(Player player) {
        // Remove by UUID
        openMenus.remove(player.getUniqueId());
    }
}