# ColorAPI Usage Guide

The `ColorAPI` provides a powerful and fluent interface for composing rich text messages in Spigot/BungeeCord plugins, supporting legacy color codes, hex colors, gradients, and interactive click/hover events. This guide focuses on composing messages by combining multiple components using the `.append()` method.

## 1. Chaining Simple Text Segments

Create a message by chaining multiple text segments with different formatting using `ColorAPI.create()` and `.append(String)`.

```java
import com.arkflame.core.colorapi.ColorAPI;
import org.bukkit.entity.Player;

Player player = // ... get player

ColorAPI.create()
    .append("&7Welcome, ") // Part 1
    .append("<#7C1FF>" + player.getName()) // Part 2 (with hex color)
    .append("&7, to our server!") // Part 3
    .send(player);
```

## 2. Appending Pre-built, Interactive Components

Create individual components with their own formatting and events, then combine them into a single message using `.append(ColorAPI)`.

```java
ColorAPI normalText = ColorAPI.colorize("&eFor more information, ");
ColorAPI hoverableText = ColorAPI.colorize("&a&l[Click Here]")
    .onHover("<#FFAA00>This will open our website!");

ColorAPI finalMessage = ColorAPI.create()
    .append(normalText)
    .append(hoverableText);

finalMessage.send(player);
```

**Result**: The player sees a seamless message: `For more information, [Click Here]`, with a hover tooltip on `[Click Here]`.

## 3. A Practical Example: Teleport Request

Build a complex, interactive teleport request by creating separate components for text and clickable buttons, then combining them.

```java
import com.arkflame.core.colorapi.ColorAPI;
import com.arkflame.core.colorapi.ClickAction;

Player requester = // ... get requester
Player target = // ... get target

ColorAPI infoText = ColorAPI.colorize("&6" + requester.getName() + " has requested to teleport to you.\n");
ColorAPI acceptButton = ColorAPI.colorize("&a&l[ACCEPT]")
    .onHover("&aClick to accept the request from " + requester.getName())
    .onClick(ClickAction.RUN_COMMAND, "/tpaccept " + requester.getName());
ColorAPI denyButton = ColorAPI.colorize(" &c&l[DENY]")
    .onHover("&cClick to deny the request from " + requester.getName())
    .onClick(ClickAction.RUN_COMMAND, "/tpdeny " + requester.getName());

ColorAPI.create()
    .append(infoText)
    .append(acceptButton)
    .append(denyButton)
    .send(target);
```

**Result**: The player sees a message with a description and two clickable buttons (`[ACCEPT]` and `[DENY]`) that execute commands when clicked, each with its own hover tooltip.

## 4. Sending to Console or Multiple Players

Send composed messages to the console or multiple players.

```java
// Send to console
ColorAPI.create()
    .append("&aServer status: ")
    .append("<#00FF00>Online")
    .sendToConsole();

// Broadcast to all online players
ColorAPI.create()
    .append("<#FF5555>Server restarting in ")
    .append("&c5 minutes!")
    .sendToAll();

// Send to a specific list of players
List<Player> staff = // ... get staff list
ColorAPI.create()
    .append("&9[STAFF] ")
    .append("&fMaintenance scheduled.")
    .send(staff);
```