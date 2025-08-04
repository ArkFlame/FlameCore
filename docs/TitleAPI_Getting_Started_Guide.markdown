# TitleAPI Getting Started Guide

The `TitleAPI` provides a fluent interface for sending main titles and subtitles to players in Spigot/BungeeCord, with full support for `ColorAPI` formatting, including legacy codes, hex colors, and gradients.

## 1. Send a Basic Title

Create and send a simple title with default fade-in, stay, and fade-out timings.

```java
import com.arkflame.flamecore.titleapi.TitleAPI;
import org.bukkit.entity.Player;

Player player = // ... get your player object

TitleAPI.create()
    .title("&a&lQUEST COMPLETE!")
    .send(player);
```

## 2. Send a Title with Subtitle and Custom Timings

Specify a subtitle and customize the timing (in ticks, where 20 ticks = 1 second).

```java
TitleAPI.create()
    .title("<#00BFFF>Level Up!</#FFFFFF>")
    .subtitle("&eYou are now level 10!")
    .fadeIn(10)  // 0.5 seconds
    .stay(60)    // 3 seconds
    .fadeOut(20) // 1 second
    .send(player);
```

## 3. Send a Title to Multiple Players

Broadcast a title to all online players or a specific group.

```java
import java.util.List;

List<Player> staff = // ... get a list of staff members

// To all online players
TitleAPI.create()
    .title("&c&lSERVER RESTART")
    .subtitle("&7in 5 minutes!")
    .sendToAll();

// To a specific group
TitleAPI.create()
    .title("&9[STAFF ALERT]")
    .subtitle("&fPlayer 'Notch' needs help.")
    .send(staff);
```