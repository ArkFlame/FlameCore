package com.arkflame.core.commandapi;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;

public class CommandContext {
    private final CommandSender sender;
    private final Map<String, Object> arguments;

    public CommandContext(CommandSender sender, Map<String, Object> arguments) {
        this.sender = sender;
        this.arguments = arguments;
    }

    public CommandSender getSender() {
        return sender;
    }

    public Player getPlayer() {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        // This should ideally not be reached if requires(SenderType.PLAYER) is used.
        // The handler throws an exception before this.
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getArgument(String name) {
        return (T) arguments.get(name.toLowerCase());
    }
}