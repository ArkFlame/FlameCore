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
    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();
    private long currentTick = 0;

    public MenuAnimator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                currentTick++;
                if (openMenus.isEmpty()) return;
                
                final Map<Inventory, Map<Integer, ItemStack>> potentialUpdates = new ConcurrentHashMap<>();

                for (Map.Entry<UUID, Menu> entry : openMenus.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) continue;

                    Menu menu = entry.getValue();
                    for (Map.Entry<Integer, MenuItem> itemEntry : menu.getItems().entrySet()) {
                        MenuItem menuItem = itemEntry.getValue();
                        int interval = menuItem.getAnimationInterval();
                        if (menuItem.isAnimated() && interval > 0 && currentTick % interval == 0) {
                            menuItem.tick();
                            potentialUpdates.computeIfAbsent(menu.getInventory(), k -> new ConcurrentHashMap<>())
                                          .put(itemEntry.getKey(), menuItem.getCurrentStack());
                        }
                    }
                }

                if (!potentialUpdates.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Map.Entry<Inventory, Map<Integer, ItemStack>> updateEntry : potentialUpdates.entrySet()) {
                                Inventory inv = updateEntry.getKey();
                                if (inv.getViewers().isEmpty()) continue;

                                for (Map.Entry<Integer, ItemStack> itemUpdate : updateEntry.getValue().entrySet()) {
                                    int slot = itemUpdate.getKey();
                                    ItemStack newItem = itemUpdate.getValue();
                                    ItemStack currentItem = inv.getItem(slot);

                                    // --- THE FIX ---
                                    // isSimilar() IGNORES durability. We must check it manually.
                                    boolean needsUpdate = currentItem == null ||
                                                          !currentItem.isSimilar(newItem) ||
                                                          currentItem.getDurability() != newItem.getDurability();

                                    if (needsUpdate) {
                                        inv.setItem(slot, newItem);
                                        for (HumanEntity viewer : inv.getViewers()) {
                                            if (viewer instanceof Player) {
                                                ((Player) viewer).updateInventory();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }
    
    public void onMenuOpen(Player player, Menu menu) {
        openMenus.put(player.getUniqueId(), menu);
    }

    public void onMenuClose(Player player) {
        openMenus.remove(player.getUniqueId());
    }
}