package com.arkflame.core.commandapi.argument;

public class Argument<T> {
    private final String name;
    private final Class<T> type;
    private final String description;

    public Argument(String name, Class<T> type, String description) {
        this.name = name.toLowerCase();
        this.type = type;
        this.description = description;
    }

    public String getName() { return name; }
    public Class<T> getType() { return type; }
    public String getDescription() { return description; }
}