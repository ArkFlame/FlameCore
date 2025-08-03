package com.arkflame.flamecore.commandapi.argument;

public class Argument<T> {
    private final String name;
    private final Class<T> type;
    private final String description;
    private final boolean required;

    public Argument(String name, Class<T> type, String description, boolean required) {
        this.name = name.toLowerCase();
        this.type = type;
        this.description = description;
        this.required = required;
    }

    public String getName() { return name; }
    public Class<T> getType() { return type; }
    public String getDescription() { return description; }
    public boolean isRequired() { return required; }
}