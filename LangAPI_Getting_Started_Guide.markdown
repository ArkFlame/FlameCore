# LangAPI Getting Started Guide

The `LangAPI` provides a fluent and powerful interface for managing localized messages in Spigot/BungeeCord plugins, supporting placeholder replacement and integration with PlaceholderAPI for dynamic text.

## 1. Dependencies

Add the following dependency to your `pom.xml` to enable PlaceholderAPI support, which is required for placeholders like `%player_health%`. Ensure PlaceholderAPI is installed on your server.

```xml
<dependency>
    <groupId>me.clip</groupId>
    <artifactId>placeholderapi</artifactId>
    <version>${placeholderapi.version}</version>
    <scope>provided</scope>
</dependency>
```

Replace `${placeholderapi.version}` with the latest version (e.g., `2.11.5`).

## 2. Set Up Language Files

Create language files in your plugin's `src/main/resources/lang/` directory to define localized messages.

### Example: `lang/en.yml`

```yaml
# Default English Language File
errors:
  no-permission: "&cYou do not have permission to do that."
  player-not-found: "&cPlayer '{target}' could not be found."
success:
  healed: "&aYou have been healed!"
  healed-other: "&aYou healed {target}. Their health is now &c%player_health%/20&a."
```

### Example: `lang/es.yml`

```yaml
# Spanish Language File
errors:
  no-permission: "&cNo tienes permiso para hacer eso."
  player-not-found: "&cNo se pudo encontrar al jugador '{target}'."
success:
  healed: "&aHas sido curado!"
  healed-other: "&aCuraste a {target}. Su vida ahora es &c%player_health%/20&a."
```

## 3. Initialize LangAPI

Initialize the `LangAPI` in your plugin's `onEnable` method to load language files.

```java
import com.arkflame.core.langapi.LangAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        LangAPI.init(this);
        // Optional: Set a different default language
        // LangAPI.setDefaultLanguage("es");
    }
}
```

## 4. Send Localized Messages

Send messages with placeholders to players using a single, fluent line.

### Example: Simple Message

```java
import com.arkflame.core.langapi.LangAPI;
import org.bukkit.entity.Player;

Player player = // ... get player
LangAPI.getMessage("errors.no-permission").send(player);
```

### Example: Message with Placeholders

```java
import org.bukkit.Bukkit;

Player target = Bukkit.getPlayer("Notch");
if (target == null) {
    LangAPI.getMessage("errors.player-not-found")
           .with("target", "Notch")
           .send(player);
    return;
}
target.setHealth(20.0);
LangAPI.getMessage("success.healed-other")
       .with("target", target.getName())
       .send(player);
```

## 5. Retrieve Messages as Strings

Get localized strings for use in other contexts, such as inventory titles.

```java
String inventoryTitle = LangAPI.getMessage("menus.main.title").get(); // Default/console
String playerSpecificTitle = LangAPI.getMessage("menus.main.title").get(player); // Player-specific
```