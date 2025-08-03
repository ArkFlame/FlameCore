# ActionBarAPI Getting Started Guide

The `ActionBarAPI` enables sending single-line messages above the player's hotbar in Spigot/BungeeCord, with seamless integration of `ColorAPI` for advanced formatting.

## 1. Send a Basic Action Bar

Create and send a simple action bar message.

```java
import com.arkflame.core.actionbarapi.ActionBarAPI;
import org.bukkit.entity.Player;

Player player = // ... get your player object

ActionBarAPI.create("&eYou have picked up &65 Gold&e.")
    .send(player);
```

## 2. Send an Action Bar with Advanced Formatting

Use `ColorAPI` features like hex colors and gradients for dynamic displays.

```java
int mana = 85;
int maxMana = 100;

ActionBarAPI.create("<#2980B9>Mana: &b" + mana + " / " + maxMana + " &7- &#F1C40FStamina: &e100%")
    .send(player);
```

## 3. Send an Action Bar to Multiple Players

Broadcast an action bar to all online players or a specific group.

```java
import java.util.List;

List<Player> playersInDanger = // ... get players with low health

// To all online players
ActionBarAPI.create("&a&lA special event has started!")
    .sendToAll();

// To a specific group
ActionBarAPI.create("&c&lYour health is critically low!")
    .send(playersInDanger);
```