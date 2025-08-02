package com.arkflame.core.commandapi;

import com.arkflame.core.commandapi.argument.Argument;
import com.arkflame.core.commandapi.parser.ArgumentParseException;
import com.arkflame.core.commandapi.parser.ArgumentParser;
import com.arkflame.core.commandapi.parser.ParserRegistry;
import com.arkflame.core.commandapi.sender.SenderType;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Map<String, Command> commands = new HashMap<>();
    private final ParserRegistry parserRegistry = new ParserRegistry();

    public CommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Command command) {
        commands.put(command.getName(), command);
        PluginCommand pluginCommand = plugin.getCommand(command.getName());
        if (pluginCommand == null) {
            plugin.getLogger().severe("Could not register command '" + command.getName() + "'. Did you forget to add it to your plugin.yml?");
            return;
        }
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
        pluginCommand.setDescription(command.getDescription());
        pluginCommand.setAliases(command.getAliases());
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command bukkitCommand, String label, String[] args) {
        Command command = commands.get(bukkitCommand.getName().toLowerCase());
        if (command == null) return false;

        try {
            executeCommand(sender, command, new LinkedList<>(Arrays.asList(args)));
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    private void executeCommand(CommandSender sender, Command command, LinkedList<String> args) throws CommandException {
        // Find the deepest matching subcommand
        if (!args.isEmpty()) {
            String potentialSubCmd = args.peek().toLowerCase();
            for (Command sub : command.getSubCommands()) {
                if (sub.getName().equals(potentialSubCmd) || sub.getAliases().contains(potentialSubCmd)) {
                    args.poll(); // Consume the argument
                    executeCommand(sender, sub, args); // Recurse
                    return;
                }
            }
        }
        
        // --- At the target command, now perform checks and execution ---
        // Permission Check
        if (!command.getPermission().isEmpty() && !sender.hasPermission(command.getPermission())) {
            throw new CommandException("You do not have permission to use this command.");
        }

        // Sender Type Check
        if (command.getRequiredSender() == SenderType.PLAYER && !(sender instanceof Player)) {
            throw new CommandException("This command can only be executed by a player.");
        }
        if (command.getRequiredSender() == SenderType.CONSOLE && sender instanceof Player) {
            throw new CommandException("This command can only be executed by the console.");
        }

        // Argument Check & Parsing
        List<Argument<?>> expectedArgs = command.getArguments();
        if (args.size() != expectedArgs.size()) {
            throw new CommandException("Invalid usage. Expected " + expectedArgs.size() + " arguments.");
        }

        Map<String, Object> parsedArgs = new HashMap<>();
        for (int i = 0; i < expectedArgs.size(); i++) {
            Argument<?> expectedArg = expectedArgs.get(i);
            String rawArg = args.get(i);
            ArgumentParser<?> parser = parserRegistry.getParser(expectedArg.getType());
            if (parser == null) {
                throw new CommandException("Internal error: No parser found for type " + expectedArg.getType().getSimpleName());
            }
            try {
                Object parsed = parser.parse(rawArg, (Class) expectedArg.getType());
                parsedArgs.put(expectedArg.getName(), parsed);
            } catch (ArgumentParseException e) {
                throw new CommandException(e.getMessage());
            }
        }
        
        // Execute the command's logic
        if (command.getExecutor() != null) {
            command.getExecutor().accept(new CommandContext(sender, parsedArgs));
        } else {
            // If no executor, show help/usage for subcommands
            sender.sendMessage(ChatColor.RED + "Invalid usage. Please specify a subcommand.");
            command.getSubCommands().forEach(sub -> sender.sendMessage(ChatColor.GRAY + " - /" + command.getName() + " " + sub.getName() + " - " + sub.getDescription()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command bukkitCommand, String alias, String[] args) {
        Command command = commands.get(bukkitCommand.getName().toLowerCase());
        if (command == null) return Collections.emptyList();
        
        return getCompletions(sender, command, new LinkedList<>(Arrays.asList(args)));
    }

    private List<String> getCompletions(CommandSender sender, Command command, LinkedList<String> args) {
        // Find the subcommand we are currently typing for
        if (args.size() > 1) {
            String potentialSubCmd = args.peek().toLowerCase();
            for (Command sub : command.getSubCommands()) {
                if ((sub.getName().equals(potentialSubCmd) || sub.getAliases().contains(potentialSubCmd)) && sender.hasPermission(sub.getPermission())) {
                    args.poll();
                    return getCompletions(sender, sub, args);
                }
            }
        }
        
        String currentInput = args.peekLast() != null ? args.peekLast() : "";
        int argIndex = args.size() - 1;

        // Suggest subcommands
        List<String> completions = new ArrayList<>();
        if (argIndex == 0) {
             command.getSubCommands().stream()
                .filter(sub -> sub.getName().toLowerCase().startsWith(currentInput.toLowerCase()))
                .filter(sub -> sub.getPermission().isEmpty() || sender.hasPermission(sub.getPermission()))
                .map(Command::getName)
                .forEach(completions::add);
        }

        // Suggest arguments if subcommands don't match
        List<Argument<?>> expectedArgs = command.getArguments();
        if (argIndex >= 0 && argIndex < expectedArgs.size()) {
            Argument<?> expectedArg = expectedArgs.get(argIndex);
            ArgumentParser<?> parser = parserRegistry.getParser(expectedArg.getType());
            if (parser != null) {
                List<String> argCompletions = parser.getTabCompletions(sender, currentInput, (Class)expectedArg.getType());
                if(argCompletions != null) completions.addAll(argCompletions);
            }
        }
        
        return completions;
    }
}

// Custom exception for cleaner error handling in the command handler
class CommandException extends Exception {
    public CommandException(String message) {
        super(message);
    }
}