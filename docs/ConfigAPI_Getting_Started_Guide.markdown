# ConfigAPI Getting Started Guide

The `ConfigAPI` provides a clean, centralized interface for managing configuration files in Spigot/BungeeCord plugins, supporting easy access to values, colorized strings, and persistence across multiple files.

## 1. Initialize ConfigAPI

Initialize the `ConfigAPI` in your plugin's `onEnable` method to set up configuration file management.

```java
import com.arkflame.flamecore.configapi.ConfigAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        ConfigAPI.init(this);
        ConfigAPI.getConfig("config.yml");
        ConfigAPI.getConfig("menus/main.yml");
    }
}
```

**Note**: Ensure `config.yml` and `menus/main.yml` are included in your plugin's resources folder to be copied to the data folder if they don't exist.

## 2. Read Values from a Config

Access configuration values in a single line using the `ConfigAPI` to retrieve a `Config` object.

```java
import com.arkflame.flamecore.configapi.Config;
import java.util.List;

Config mainConfig = ConfigAPI.getConfig("config.yml");

int maxPlayers = mainConfig.getInt("server.max-players", 100);
String welcomeMessage = mainConfig.getString("messages.welcome");
List<String> motdLines = mainConfig.getStringList("motd.lines");
```

## 3. Access Multiple Config Files

Easily retrieve values from configuration files in subdirectories.

```java
Config menuConfig = ConfigAPI.getConfig("menus/main.yml");

String menuTitle = menuConfig.getString("main-menu.title");
List<String> itemLore = menuConfig.getStringList("main-menu.items.server-selector.lore");
```

## 4. Set and Save Config Values

Modify configuration values and persist them to the file.

```java
Config dataConfig = ConfigAPI.getConfig("player_data_count.yml");

int joinCount = dataConfig.getInt("total-joins", 0);
joinCount++;
dataConfig.set("total-joins", joinCount);
dataConfig.save();
```