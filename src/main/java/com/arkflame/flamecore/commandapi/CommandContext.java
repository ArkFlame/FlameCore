package com.arkflame.flamecore.commandapi;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.Optional;

public class CommandContext {
    private final CommandSender sender;
    private final Map<String, Object> arguments;

    public CommandContext(CommandSender sender, Map<String, Object> arguments) {
        this.sender = sender;
        this.arguments = arguments;
    }

    public CommandSender getSender() { return sender; }

    public Player getPlayer() {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    /**
     * Gets a required argument by its name.
     */
    @SuppressWarnings("unchecked")
    public <T> T getArgument(String name) {
        return (T) arguments.get(name.toLowerCase());
    }

    /**
     * Gets an optional argument by its name, returning a default value if not present.
     */
    @SuppressWarnings("unchecked")
    public <T> T getArgumentOrDefault(String name, T defaultValue) {
        return (T) arguments.getOrDefault(name.toLowerCase(), defaultValue);
    }
    
    /**
     * Gets an optional argument by its name, returning an Optional.
     */
    public <T> Optional<T> getOptionalArgument(String name) {
        return Optional.ofNullable(getArgument(name));
    }
}