# MySQLAPI Getting Started Guide

The `MySQLAPI` provides a robust interface for managing data in a MySQL database for Spigot/BungeeCord plugins. This guide demonstrates how to initialize the API, define a data class, and use it to store and retrieve player statistics.

## 1. Dependencies

Add the following MySQL and HikariCP dependencies to your `pom.xml` to enable database connectivity. These dependencies must be bundled to ensure they are included in your plugin's JAR file.

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>${mysql.version}</version>
</dependency>
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>${hikaricp.version}</version>
</dependency>
```

Replace `${mysql.version}` and `${hikaricp.version}` with the latest versions (e.g., `8.0.33` for MySQL and `5.0.1` for HikariCP).

## 2. Initialize MySQLAPI

Initialize the `MySQLAPI` in your plugin's `onEnable` method, providing database connection details.

```java
import com.arkflame.flamecore.mysqlapi.MySQLAPI;
import com.arkflame.flamecore.mysqlapi.MySQLConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class MyMySQLPlugin extends JavaPlugin {
    private MySQLAPI mysqlAPI;

    @Override
    public void onEnable() {
        MySQLConfig dbConfig = new MySQLConfig("localhost", 3306, "minecraft_server", "user", "password");
        this.mysqlAPI = new MySQLAPI(this, dbConfig);
        getLogger().info("MySQLAPI initialized.");
    }

    @Override
    public void onDisable() {
        if (mysqlAPI != null) {
            mysqlAPI.shutdown();
        }
    }

    public MySQLAPI getMySQLAPI() {
        return mysqlAPI;
    }
}
```

## 3. Define a Data Class

Create a data class to represent the data you want to store, using annotations to define the table structure.

```java
import com.arkflame.flamecore.mysqlapi.annotations.PrimaryKey;
import com.arkflame.flamecore.mysqlapi.annotations.StoreAsTable;
import com.arkflame.flamecore.mysqlapi.annotations.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    @PrimaryKey
    private UUID uuid;
    private String lastKnownName;
    private long joinDate;
    private int kills;
    private int deaths;
    @StoreAsTable
    private Map<String, Long> kitCooldowns;
    @Transient
    private boolean isOnline = false;

    public PlayerStats() {}

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.lastKnownName = name;
        this.joinDate = System.currentTimeMillis();
        this.kills = 0;
        this.deaths = 0;
        this.kitCooldowns = new HashMap<>();
    }

    public UUID getUuid() { return uuid; }
    public String getLastKnownName() { return lastKnownName; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public void addKill() { this.kills++; }
    public void addDeath() { this.deaths++; }
    public void setLastKnownName(String name) { this.lastKnownName = name; }
}
```

## 4. Load and Save Data

Use the `MySQLAPI` to load and save player data, such as when a player joins the server.

```java
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {
    private final MyMySQLPlugin plugin;

    public PlayerListener(MyMySQLPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getMySQLAPI().loadById(PlayerStats.class, player.getUniqueId()).thenAccept(stats -> {
            PlayerStats playerStats;
            if (stats == null) {
                player.sendMessage("Welcome! Creating your stats profile...");
                playerStats = new PlayerStats(player.getUniqueId(), player.getName());
            } else {
                player.sendMessage("Welcome back!");
                playerStats = stats;
                playerStats.setLastKnownName(player.getName());
            }
            plugin.getMySQLAPI().save(playerStats);
        });
    }
}
```

## 5. Query Data with Commands

Create a command to query player statistics, demonstrating how to retrieve data by ID or field.

```java
import com.arkflame.flamecore.commandapi.Command;
import com.arkflame.flamecore.commandapi.sender.SenderType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyMySQLPlugin extends JavaPlugin {
    // ... (initialization code from Step 2)

    private void registerStatsCommand() {
        Command.create("stats")
            .setDescription("View player statistics.")
            .requires(SenderType.PLAYER)
            .setExecutor(ctx -> {
                Player player = ctx.getPlayer();
                mysqlAPI.loadById(PlayerStats.class, player.getUniqueId()).thenAccept(stats -> {
                    if (stats == null) {
                        player.sendMessage(ChatColor.RED + "Your stats could not be found. Please relog.");
                        return;
                    }
                    displayStats(player, stats);
                });
            })
            .addSubCommand(Command.create("lookup")
                .addArgument("name", String.class, "Player name to look up.")
                .setExecutor(ctx -> {
                    String targetName = ctx.getArgument("name");
                    mysqlAPI.loadAllBy(PlayerStats.class, "lastKnownName", targetName).thenAccept(statsList -> {
                        if (statsList.isEmpty()) {
                            ctx.getSender().sendMessage(ChatColor.RED + "No player found with that name.");
                            return;
                        }
                        ctx.getSender().sendMessage(ChatColor.GOLD + "--- Stats for " + targetName + " ---");
                        statsList.forEach(stats -> displayStats(ctx.getPlayer(), stats));
                    });
                })
            )
            .register();
    }

    private void displayStats(Player receiver, PlayerStats stats) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        receiver.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.WHITE + stats.getUuid());
        receiver.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + stats.getKills());
        receiver.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + stats.getDeaths());
        receiver.sendMessage(ChatColor.YELLOW + "Join Date: " + ChatColor.WHITE + sdf.format(new Date(stats.getJoinDate())));
    }
}
```