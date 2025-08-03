package com.arkflame.flamecore.commandapi.parser;

import org.bukkit.command.CommandSender;
import java.util.List;

@FunctionalInterface
interface TriFunction<T, U, V, R> { R apply(T t, U u, V v); }

@FunctionalInterface
interface ThrowingBiFunction<T, U, R> { R apply(T t, U u) throws Exception; }

public class ArgumentParser<T> {
    private final ThrowingBiFunction<String, Class<T>, T> parser;
    private final TriFunction<CommandSender, String, Class<T>, List<String>> tabCompleter;

    // Constructor for simple types like Integer, Player
    public ArgumentParser(ThrowingFunction<String, T> parser, BiFunction<CommandSender, String, List<String>> tabCompleter) {
        this.parser = (str, type) -> parser.apply(str);
        this.tabCompleter = (sender, input, type) -> tabCompleter.apply(sender, input);
    }

    // Constructor for complex/generic types like Enums
    public ArgumentParser(ThrowingBiFunction<String, Class<T>, T> parser, TriFunction<CommandSender, String, Class<T>, List<String>> tabCompleter) {
        this.parser = parser;
        this.tabCompleter = tabCompleter;
    }

    public T parse(String input, Class<T> type) throws ArgumentParseException {
        try {
            return parser.apply(input, type);
        } catch (Exception e) {
            if (e instanceof ArgumentParseException) throw (ArgumentParseException) e;
            throw new ArgumentParseException("Invalid format for type " + type.getSimpleName() + ".");
        }
    }

    public List<String> getTabCompletions(CommandSender sender, String input, Class<T> type) {
        return tabCompleter.apply(sender, input, type);
    }
}

// Helper functional interfaces to allow throwing exceptions
@FunctionalInterface interface ThrowingFunction<T, R> { R apply(T t) throws Exception; }
@FunctionalInterface interface BiFunction<T, U, R> { R apply(T t, U u); }