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
                // This map will store updates to be performed on the main thread.
                final Map<Inventory, Map<Integer, ItemStack>> syncUpdates = new ConcurrentHashMap<>();

                // Asynchronously calculate all animation frames
                for (Map.Entry<Player, Menu> entry : openMenus.entrySet()) {
                    Menu menu = entry.getValue();
                    for (Map.Entry<Integer, MenuItem> itemEntry : menu.getItems().entrySet()) {
                        MenuItem menuItem = itemEntry.getValue();
                        
                        if (menuItem.isAnimated() && currentTick % menuItem.getAnimationInterval() == 0) {
                            ItemStack newStack = menuItem.tick();
                            if (newStack != null) {
                                syncUpdates.computeIfAbsent(menu.getInventory(), k -> new ConcurrentHashMap<>())
                                           .put(itemEntry.getKey(), newStack);
                            }
                        }
                    }
                }

                // If there are updates, schedule them to run on the main server thread
                if (!syncUpdates.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Map.Entry<Inventory, Map<Integer, ItemStack>> updateEntry : syncUpdates.entrySet()) {
                                Inventory inv = updateEntry.getKey();
                                for (Map.Entry<Integer, ItemStack> itemUpdate : updateEntry.getValue().entrySet()) {
                                    inv.setItem(itemUpdate.getKey(), itemUpdate.getValue());
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