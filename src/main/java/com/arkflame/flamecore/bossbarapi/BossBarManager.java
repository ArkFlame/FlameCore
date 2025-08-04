package com.arkflame.flamecore.bossbarapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.arkflame.flamecore.bossbarapi.bridge.BossBarBridge;
import com.arkflame.flamecore.bossbarapi.bridge.LegacyWitherBossBar;
import com.arkflame.flamecore.bossbarapi.bridge.ModernBossBar;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active BossBars, handles version detection, and runs update tasks.
 * This class is for internal use only.
 */
public class BossBarManager {
    private static JavaPlugin plugin;
    private static boolean isLegacy;
    static final Set<BossBarAPI> activeBars = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            return;
        }
        plugin = pluginInstance;

        // Check server version
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        isLegacy = version.startsWith("v1_8");

        // Register listeners and start tasks if on a legacy version
        if (isLegacy) {
            Bukkit.getPluginManager().registerEvents(new BossBarListener(), plugin);
            startLegacyUpdateTask();
        }
    }

    static BossBarBridge createBridge() {
        if (plugin == null) {
            throw new IllegalStateException("BossBarManager has not been initialized! Call BossBarManager.init(plugin) in onEnable.");
        }
        BossBarAPI bar = new BossBarAPI();
        activeBars.add(bar);
        return isLegacy ? new LegacyWitherBossBar() : new ModernBossBar();
    }

    static void removeBar(BossBarAPI bar) {
        activeBars.remove(bar);
    }
    
    private static void startLegacyUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (BossBarAPI bar : activeBars) {
                    for (UUID uuid : bar.getPlayers()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            // Tell the legacy bridge to update the wither position for this player
                            ((LegacyWitherBossBar) bar.getBridge()).updatePosition(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // Update positions every 2 seconds
    }
}