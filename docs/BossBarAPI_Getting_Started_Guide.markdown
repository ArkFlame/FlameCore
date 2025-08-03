# BossBarAPI Getting Started Guide

The `BossBarAPI` allows creating and managing boss bars in Spigot/BungeeCord, supporting `ColorAPI` formatting and customizable styles.

## 1. Initialize BossBarAPI

Initialize the `BossBarManager` in your plugin's `onEnable` method.

```java
import com.arkflame.core.bossbarapi.BossBarManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        BossBarManager.init(this);
    }
}
```

## 2. Create and Display a Boss Bar

Create a boss bar with text, progress, color, and style, then add a player to it.

```java
import com.arkflame.core.bossbarapi.BossBarAPI;
import com.arkflame.core.bossbarapi.enums.BarColor;
import com.arkflame.core.bossbarapi.enums.BarStyle;
import org.bukkit.entity.Player;

Player somePlayer = // ... get your player object

BossBarAPI eventBar = BossBarAPI.create()
    .text("&e&lEvent starting in &a&l30s")
    .progress(1.0)
    .color(BarColor.YELLOW)
    .style(BarStyle.SEGMENTED_20);

eventBar.addPlayer(somePlayer);
```

## 3. Update and Remove a Boss Bar

Dynamically update the boss bar's text and progress, and destroy it when done.

```java
import org.bukkit.scheduler.BukkitRunnable;

BossBarAPI eventBar = BossBarAPI.create()
    .text("&e&lEvent starting in &a&l30s")
    .progress(1.0)
    .color(BarColor.YELLOW)
    .style(BarStyle.SEGMENTED_20);

eventBar.addPlayer(somePlayer);

new BukkitRunnable() {
    int i = 30;
    public void run() {
        if (i <= 0) {
            eventBar.text("&a&lEvent has started!");
            eventBar.progress(1.0);
            new BukkitRunnable() {
                public void run() { eventBar.destroy(); }
            }.runTaskLater(plugin, 100L);
            this.cancel();
            return;
        }
        eventBar.text("&e&lEvent starting in &a&l" + i + "s");
        eventBar.progress((double) i / 30.0);
        i--;
    }
}.runTaskTimer(plugin, 0L, 20L);
```