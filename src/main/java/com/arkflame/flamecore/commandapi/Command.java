package com.arkflame.flamecore.commandapi;

import org.bukkit.command.CommandSender;

import com.arkflame.flamecore.commandapi.argument.Argument;
import com.arkflame.flamecore.commandapi.sender.SenderType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Command {
    private final String name;
    private final List<String> aliases = new ArrayList<>();
    private final List<Argument<?>> arguments = new ArrayList<>();
    private final List<Command> subCommands = new ArrayList<>();

    private String description = "";
    private String permission = "";
    private SenderType requiredSender = SenderType.ANY;
    private Consumer<CommandContext> executor;
    private String usage = null;

    private Command(String name) {
        this.name = name.toLowerCase().trim();
    }

    public static Command create(String name) {
        return new Command(name);
    }

    public Command setAliases(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    public Command setDescription(String description) {
        this.description = description;
        return this;
    }

    public Command setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public Command requires(SenderType type) {
        this.requiredSender = type;
        return this;
    }

    public <T> Command addArgument(String name, Class<T> type, String description) {
        this.arguments.add(new Argument<>(name, type, description, true));
        return this;
    }

    public <T> Command addOptionalArgument(String name, Class<T> type, String description) {
        this.arguments.add(new Argument<>(name, type, description, false));
        return this;
    }

    public Command setExecutor(Consumer<CommandContext> executor) {
        this.executor = executor;
        return this;
    }

    public Command addSubCommand(Command subCommand) {
        this.subCommands.add(subCommand);
        return this;
    }

    public void register() {
        CommandAPI.registerCommand(this);
    }
    
    // --- Getters for the CommandHandler ---
    public String getName() { return name; }
    public List<String> getAliases() { return aliases; }
    public String getDescription() { return description; }
    public String getPermission() { return permission; }
    public SenderType getRequiredSender() { return requiredSender; }
    public List<Argument<?>> getArguments() { return arguments; }
    public Consumer<CommandContext> getExecutor() { return executor; }
    public List<Command> getSubCommands() { return subCommands; }
    
    public Command setUsage(String usage) {
        this.usage = usage;
        return this;
    }

    public String getUsage() {
        return usage;
    }
}