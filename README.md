# FlameCore

FlameCore is a comprehensive and lightweight library designed to streamline Spigot and BungeeCord plugin development. It provides a suite of intuitive APIs for creating modern, feature-rich Minecraft plugins with minimal boilerplate code. FlameCore supports a wide range of functionalities, including rich text formatting, command management, GUI menus, database integration, in-game notifications, material handling, and schematic operations, all while ensuring compatibility across different Minecraft server versions.

## Features

- **ColorAPI**: Format chat messages with legacy codes, hex colors, gradients, and interactive click/hover events.
- **CommandAPI**: Build complex, type-safe commands with subcommands and permissions using a fluent builder.
- **MenuAPI**: Create protected, animated GUI menus with customizable click actions and item behaviors.
- **SQLiteAPI & MySQLAPI**: Manage data with simple, annotation-based persistence for local SQLite or remote MySQL databases.
- **TitleAPI**: Send formatted titles and subtitles to players with customizable timings.
- **ActionBarAPI**: Display single-line messages above players' hotbars with advanced formatting.
- **BossBarAPI**: Create dynamic boss bars with customizable text, progress, colors, and styles.
- **MaterialAPI**: Safely handle materials across Minecraft versions with fallback support and helper methods.
- **SchematicAPI**: Save, load, and paste block structures for arenas or restorable blocks.

## Getting Started

### Installation

1. Download the latest FlameCore JAR from the [Releases](https://github.com/yourusername/FlameCore/releases) page.
2. Add the JAR to your plugin's `libs` folder and include it in your build path.
3. Ensure your plugin depends on FlameCore in your `plugin.yml`:
   ```yaml
   depend: [FlameCore]
   ```

### Basic Setup

Initialize the necessary APIs in your plugin's `onEnable` method. Below is an example initializing key APIs:

```java
import com.arkflame.core.bossbarapi.BossBarManager;
import com.arkflame.core.menuapi.MenuAPI;
import com.arkflame.core.schematicapi.SchematicAPI;
import com.arkflame.core.commandapi.CommandAPI;
import com.arkflame.core.sqliteapi.SQLiteAPI;
import com.arkflame.core.sqliteapi.SQLiteConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    private SQLiteAPI sqliteAPI;

    @Override
    public void onEnable() {
        // Initialize APIs
        CommandAPI.init(this);
        MenuAPI.init(this);
        BossBarManager.init(this);
        SchematicAPI.init(this);

        // Initialize SQLiteAPI
        SQLiteConfig dbConfig = new SQLiteConfig(this, "playerdata.db");
        this.sqliteAPI = new SQLiteAPI(this, dbConfig);

        getLogger().info("FlameCore APIs initialized.");
    }

    @Override
    public void onDisable() {
        if (sqliteAPI != null) {
            sqliteAPI.shutdown();
        }
    }
}
```

## Usage Examples

### ColorAPI: Sending a Gradient Message

```java
import com.arkflame.core.colorapi.ColorAPI;
import org.bukkit.entity.Player;

Player player = // ... get player
ColorAPI.colorize("<#55C1FF>Welcome to the server!</#FFAA00>")
    .send(player);
```

### CommandAPI: Creating a Command with Subcommands

```java
import com.arkflame.core.commandapi.Command;
import com.arkflame.core.commandapi.sender.SenderType;
import org.bukkit.ChatColor;

Command.create("kit")
    .setDescription("Gives a player a kit.")
    .requires(SenderType.PLAYER)
    .setExecutor(ctx -> ctx.getPlayer().sendMessage(ChatColor.GOLD + "Available kits: knight, archer"))
    .addSubCommand(Command.create("get")
        .addArgument("kitname", String.class, "The kit to receive.")
        .setExecutor(ctx -> ctx.getPlayer().sendMessage(ChatColor.GREEN + "You received the '" + ctx.getArgument("kitname") + "' kit!"))
    )
    .register();
```

### MenuAPI: Creating an Animated Menu

```java
import com.arkflame.core.menuapi.MenuBuilder;
import com.arkflame.core.menuapi.ItemBuilder;
import org.bukkit.Material;

new MenuBuilder(9, "&eWeapon Forge")
    .setItem(4, new ItemBuilder(Material.WOOD_SWORD)
        .animationInterval(10)
        .addMaterialFrame(Material.WOOD_SWORD)
        .addMaterialFrame(Material.STONE_SWORD)
        .addNameFrame("&7Basic Sword")
        .addNameFrame("&b&lUltimate Sword")
        .build())
    .open(player);
```

### SQLiteAPI/MySQLAPI: Storing Player Data

```java
import com.arkflame.core.sqliteapi.SQLiteAPI;
import com.arkflame.core.sqliteapi.annotations.PrimaryKey;
import java.util.UUID;

public class PlayerStats {
    @PrimaryKey
    private UUID uuid;
    private int kills;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.kills = 0;
    }
    public void addKill() { this.kills++; }
}

// In PlayerListener
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    sqliteAPI.loadById(PlayerStats.class, player.getUniqueId()).thenAccept(stats -> {
        if (stats == null) {
            stats = new PlayerStats(player.getUniqueId());
        }
        sqliteAPI.save(stats);
    });
}
```

### TitleAPI: Sending a Title

```java
import com.arkflame.core.titleapi.TitleAPI;

TitleAPI.create()
    .title("<#00BFFF>Level Up!</#FFFFFF>")
    .subtitle("&eYou are now level 10!")
    .fadeIn(10)
    .stay(60)
    .fadeOut(20)
    .send(player);
```

### ActionBarAPI: Displaying a Status

```java
import com.arkflame.core.actionbarapi.ActionBarAPI;

ActionBarAPI.create("<#2980B9>Mana: &b85 / 100")
    .send(player);
```

### BossBarAPI: Creating a Dynamic Boss Bar

```java
import com.arkflame.core.bossbarapi.BossBarAPI;
import com.arkflame.core.bossbarapi.enums.BarColor;
import com.arkflame.core.bossbarapi.enums.BarStyle;
import org.bukkit.scheduler.BukkitRunnable;

BossBarAPI bar = BossBarAPI.create()
    .text("&e&lEvent starting in &a&l30s")
    .progress(1.0)
    .color(BarColor.YELLOW)
    .style(BarStyle.SEGMENTED_20)
    .addPlayer(player);

new BukkitRunnable() {
    int i = 30;
    public void run() {
        if (i <= 0) {
            bar.text("&a&lEvent has started!");
            bar.destroy();
            cancel();
            return;
        }
        bar.text("&e&lEvent starting in &a&l" + i + "s");
        bar.progress((double) i / 30.0);
        i--;
    }
}.runTaskTimer(plugin, 0L, 20L);
```

### MaterialAPI: Handling Materials Safely

```java
import com.arkflame.core.materialapi.MaterialAPI;
import org.bukkit.Material;

Material logMaterial = MaterialAPI.getOrAir("LOG", "OAK_LOG");
player.getInventory().addItem(new ItemStack(logMaterial));
```

### SchematicAPI: Saving and Pasting Structures

```java
import com.arkflame.core.schematicapi.SchematicAPI;
import org.bukkit.Location;
import java.io.File;

Location pos1 = // ... first corner
Location pos2 = // ... second corner
SchematicAPI.copy(pos1, pos2).thenAccept(schematic -> {
    File file = new File(plugin.getDataFolder() + "/arenas/", "arena.arkschem");
    schematic.save(file).thenRun(() -> player.sendMessage("Arena saved!"));
});

Location pasteLocation = // ... paste location
SchematicAPI.load(file).thenAccept(schematic -> {
    schematic.paste(pasteLocation, (success) -> player.sendMessage("Arena pasted!"));
});
```

## Contributing

Contributions are welcome! Please submit pull requests or open issues on the [GitHub repository](https://github.com/yourusername/FlameCore). Ensure code follows the project's style guidelines and includes appropriate tests.

## License

FlameCore is licensed under the [MIT License](LICENSE). See the LICENSE file for details.
