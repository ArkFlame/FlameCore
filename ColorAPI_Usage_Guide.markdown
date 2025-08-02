# ColorAPI Usage Guide

`ColorAPI` provides a fluent and easy-to-use interface for creating modern chat messages in Spigot/BungeeCord. It supports legacy codes, hex colors, gradients, and interactive events without requiring external dependencies.

## 1. Getting Started: Basic Colorization

The simplest way to use the API is with the static `colorize()` method, which processes strings containing standard legacy `&` codes.

```java
import com.arkflame.core.colorapi.ColorAPI;
import org.bukkit.entity.Player;

// Assume 'player' is your target Player object
Player player = /* ... */;

String message = "&aWelcome to the server, &e" + player.getName() + "&a!";

ColorAPI.colorize(message).send(player);
```

## 2. Using Hex Colors

Custom hex colors can be used with the `&#RRGGBB` format.

```java
String message = "&#FFA500This is a custom orange color!";

ColorAPI.colorize(message).send(player);
```

Hex and legacy codes can be mixed in the same string.

```java
String message = "&#0000FFThis is blue, &cbut this is red.";

ColorAPI.colorize(message).send(player);
```

## 3. Creating Gradients

Create text gradients using the `<#RRGGBB>...</#RRGGBB>` syntax.

```java
String gradientText = "<#55C1FF>This text fades from blue to orange!</#FFAA00>";

ColorAPI.colorize(gradientText).send(player);
```

## 4. Adding Click Events

Make text interactive by adding click events to execute commands, open URLs, or perform other actions.

```java
import net.md_5.bungee.api.chat.ClickEvent;

ColorAPI.colorize("&a&l[Click to Visit our Website]")
        .onClick(ClickEvent.Action.OPEN_URL, "https://store.example.com")
        .send(player);
```

To make text run a command for the player:

```java
ColorAPI.colorize("&eClick here to go to the hub!")
        .onClick(ClickEvent.Action.RUN_COMMAND, "/hub")
        .send(player);
```

## 5. Adding Hover Events

Add hover events to display additional information when a player hovers over the text.

```java
ColorAPI.colorize("&c&l[RULES]")
        .onHover("&eClick to see the server rules.")
        .onClick(ClickEvent.Action.RUN_COMMAND, "/rules")
        .send(player);
```

## 6. Chaining and Appending

For complex messages, use `create()` and `append()` to build messages incrementally. Events apply to the last appended text.

```java
ColorAPI.create()
    .append("&7Welcome! ") // This part has no events
    .append("&eClick &ahere &eto join the event!") // This part is clickable
    .onHover("&6Join the Summer Event!")
    .onClick(ClickEvent.Action.RUN_COMMAND, "/event join")
    .send(player);
```

## 7. Console Compatibility

The API is safe to use with `CommandSender`. If the sender is the console, it automatically converts the message to a simple string with legacy colors, stripping out non-console features like hover/click events.

```java
import org.bukkit.command.CommandSender;

public void sendAnnouncement(CommandSender sender) {
    ColorAPI.colorize("<#FF5555>ANNOUNCEMENT!</#FFAA00> &7Check our &b/discord&7!")
            .onHover("&9Click to get the invite link!")
            .onClick(ClickEvent.Action.RUN_COMMAND, "/discord")
            .send(sender); // Works for both Player and Console
}
```

Player output: A clickable, hoverable, gradient message.  
Console output: `&cANNOUNCEMENT! &7Check our &b/discord&7!`