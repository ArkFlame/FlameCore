package com.arkflame.flamecore.commandapi;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.arkflame.flamecore.commandapi.argument.Argument;
import com.arkflame.flamecore.commandapi.parser.ArgumentParseException;
import com.arkflame.flamecore.commandapi.parser.ArgumentParser;
import com.arkflame.flamecore.commandapi.parser.ParserRegistry;
import com.arkflame.flamecore.commandapi.sender.SenderType;

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
            LinkedList<String> processedArgs = processQuotedArgs(args);
            executeCommand(sender, command, processedArgs);
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    /**
     * NEW METHOD: Processes an array of string arguments, combining arguments
     * enclosed in double quotes into a single argument.
     *
     * @param args The raw arguments from onCommand.
     * @return A LinkedList of processed arguments.
     */
    private LinkedList<String> processQuotedArgs(String[] args) {
        LinkedList<String> processed = new LinkedList<>();
        boolean inQuotes = false;
        StringBuilder quotedArg = new StringBuilder();

        for (String arg : args) {
            if (inQuotes) {
                // We are inside a quoted block.
                if (arg.endsWith("\"")) {
                    // This is the end of the quote. Append the word (without the quote) and finish.
                    quotedArg.append(" ").append(arg.substring(0, arg.length() - 1));
                    processed.add(quotedArg.toString());
                    inQuotes = false;
                    quotedArg = new StringBuilder();
                } else {
                    // This is a word in the middle of a quote. Just append it.
                    quotedArg.append(" ").append(arg);
                }
            } else {
                // We are not inside a quote.
                if (arg.startsWith("\"")) {
                    if (arg.endsWith("\"") && arg.length() > 1) {
                        // The entire quote is in one element (e.g., "/cmd "hello world"").
                        processed.add(arg.substring(1, arg.length() - 1));
                    } else {
                        // This is the start of a quote.
                        inQuotes = true;
                        quotedArg.append(arg.substring(1));
                    }
                } else {
                    // This is a normal, unquoted argument.
                    processed.add(arg);
                }
            }
        }
        
        // Safety check for unclosed quotes, though it's unlikely with how commands are parsed.
        if(inQuotes) {
            processed.add(quotedArg.toString());
        }

        return processed;
    }

    private void executeCommand(CommandSender sender, Command command, LinkedList<String> args) throws CommandException {
        // --- Subcommand Resolution ---
        if (!args.isEmpty()) {
            String potentialSubCmd = args.peek().toLowerCase();
            for (Command sub : command.getSubCommands()) {
                if (sub.getName().equals(potentialSubCmd) || sub.getAliases().contains(potentialSubCmd)) {
                    args.poll();
                    executeCommand(sender, sub, args);
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

        // --- Argument Check & Parsing (Updated Logic) ---
        List<Argument<?>> expectedArgs = command.getArguments();
        long requiredArgsCount = expectedArgs.stream().filter(Argument::isRequired).count();
        
        // Check if the number of provided arguments is valid
        if (args.size() < requiredArgsCount) {
            throw new CommandException("Invalid usage. Missing required arguments.");
        }
        if (args.size() > expectedArgs.size()) {
            throw new CommandException("Invalid usage. Too many arguments provided.");
        }
        
        Map<String, Object> parsedArgs = new HashMap<>();
        for (int i = 0; i < expectedArgs.size(); i++) {
            Argument<?> expectedArg = expectedArgs.get(i);
            
            // If an argument string was provided for this position...
            if (i < args.size()) {
                String rawArg = args.get(i);
                ArgumentParser<?> parser = parserRegistry.getParser((Class)expectedArg.getType());
                if (parser == null) {
                    throw new CommandException("Internal error: No parser found for type " + expectedArg.getType().getSimpleName());
                }
                try {
                    Object parsed = parser.parse(rawArg, (Class) expectedArg.getType());
                    parsedArgs.put(expectedArg.getName(), parsed);
                } catch (ArgumentParseException e) {
                    // Prepend the argument name to the error for clarity (e.g., "target: Player not found")
                    throw new CommandException(expectedArg.getName() + ": " + e.getMessage());
                }
            } else if (expectedArg.isRequired()) {
                // This case should be caught by the size check above, but is a safeguard.
                throw new CommandException("Missing required argument: " + expectedArg.getName());
            }
            // If the argument is optional and not provided, we simply do nothing.
            // The getArgumentOrDefault in CommandContext will handle it.
        }
        
        // --- Execution ---
        if (command.getExecutor() != null) {
            command.getExecutor().accept(new CommandContext(sender, parsedArgs));
        } else {
            // If no executor is defined, but there are subcommands, show help/usage.
            if (!command.getSubCommands().isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Invalid usage. Please specify a subcommand.");
                // You could generate a more detailed help message here.
            } else {
                // This case is unlikely but possible if a command is registered with no executor.
                throw new CommandException("This command has no defined action.");
            }
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