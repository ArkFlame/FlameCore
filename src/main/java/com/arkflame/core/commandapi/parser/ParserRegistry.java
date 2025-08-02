package com.arkflame.core.commandapi.parser;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ParserRegistry {
    private final Map<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();

    public ParserRegistry() {
        register(String.class, new ArgumentParser<>(s -> s, (sender, input) -> Collections.emptyList()));
        register(Integer.class,
                new ArgumentParser<>(Integer::parseInt, (sender, input) -> Arrays.asList("1", "16", "32", "64")));
        register(Double.class,
                new ArgumentParser<>(Double::parseDouble, (sender, input) -> Arrays.asList("1.0", "10.5", "100.0")));
        register(Boolean.class,
                new ArgumentParser<>(Boolean::parseBoolean, (sender, input) -> Arrays.asList("true", "false")));

        register(Player.class, new ArgumentParser<>(
                s -> {
                    Player p = Bukkit.getPlayerExact(s);
                    if (p == null)
                        throw new ArgumentParseException("Player not found: " + s);
                    return p;
                },
                (sender, input) -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                        .collect(Collectors.toList())));

        // Generic Enum parser
        parsers.put(Enum.class, new ArgumentParser<>(
                (str, type) -> {
                    for (Object constant : type.getEnumConstants()) {
                        if (constant.toString().equalsIgnoreCase(str)) {
                            // We found a case-insensitive match, return it cast to the correct type.
                            // The unchecked cast is safe because getEnumConstants() returns T[].
                            @SuppressWarnings("unchecked")
                            Enum<?> enumValue = (Enum<?>) constant;
                            return enumValue;
                        }
                    }

                    // If the loop completes without finding a match, throw an exception.
                    throw new IllegalArgumentException(
                            "No enum constant " + type.getCanonicalName() + " for value '" + str + "'");
                },
                (sender, input, type) -> Arrays.stream(type.getEnumConstants())
                        .map(Object::toString)
                        .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                        .collect(Collectors.toList())));
    }

    public <T> void register(Class<T> clazz, ArgumentParser<T> parser) {
        parsers.put(clazz, parser);
    }

    @SuppressWarnings("unchecked")
    public <T> ArgumentParser<T> getParser(Class<T> clazz) {
        if (clazz.isEnum()) {
            return (ArgumentParser<T>) parsers.get(Enum.class);
        }
        return (ArgumentParser<T>) parsers.get(clazz);
    }
}