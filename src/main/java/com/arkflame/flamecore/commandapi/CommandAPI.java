package com.arkflame.flamecore.commandapi;

import org.bukkit.plugin.java.JavaPlugin;

public class CommandAPI {
    private static JavaPlugin plugin;
    private static CommandHandler commandHandler;

    public static void init(JavaPlugin pluginInstance) {
        if (plugin != null) {
            return;
        }
        plugin = pluginInstance;
        commandHandler = new CommandHandler(plugin);
    }

    static void registerCommand(Command command) {
        if (plugin == null) {
            throw new IllegalStateException("CommandAPI has not been initialized! Call CommandAPI.init(plugin) in onEnable.");
        }
        commandHandler.register(command);
    }
}