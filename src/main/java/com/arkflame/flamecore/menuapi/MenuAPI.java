package com.arkflame.flamecore.menuapi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MenuAPI {
    private static JavaPlugin plugin;
    private static MenuAnimator animator;

    /**
     * Initializes the MenuAPI. Must be called once in your plugin's onEnable method.
     * @param pluginInstance The instance of your plugin.
     */
    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            throw new IllegalStateException("MenuAPI is already initialized.");
        }
        plugin = pluginInstance;

        // Start the central animator task
        animator = new MenuAnimator(plugin);
        animator.start();

        // Register the necessary event listeners
        Bukkit.getPluginManager().registerEvents(new MenuListener(animator), plugin);
    }

    static MenuAnimator getAnimator() {
        if (animator == null) {
            throw new IllegalStateException("MenuAPI has not been initialized! Call MenuAPI.init(plugin) in onEnable.");
        }
        return animator;
    }

    static JavaPlugin getPlugin() {
        return plugin;
    }
}