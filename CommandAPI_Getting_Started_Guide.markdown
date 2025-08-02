# CommandAPI Getting Started Guide

The `CommandAPI` provides a fluent, builder-style interface for creating complex commands in Spigot/BungeeCord with support for subcommands, permissions, and type-safe arguments. This guide demonstrates how to set up and use the `CommandAPI` to create a `/kit` command with subcommands.

## 1. Initialize the CommandAPI

Initialize the `CommandAPI` once in your plugin's `onEnable` method.

```java
import com.yourpackage.commandapi.CommandAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandAPI.init(this);
    }
}
```

## 2. Create a Basic Command

Define a command using `Command.create()`, set its properties, and register it. The following example creates a `/kit` command that lists available kits.

```java
import com.yourpackage.commandapi.Command;
import com.yourpackage.commandapi.sender.SenderType;
import org.bukkit.ChatColor;

Command.create("kit")
    .setDescription("Gives a player a kit.")
    .setPermission("myplugin.kit.use")
    .requires(SenderType.PLAYER)
    .setExecutor(ctx -> {
        ctx.getPlayer().sendMessage(ChatColor.GOLD + "Available kits: knight, archer");
        ctx.getPlayer().sendMessage(ChatColor.GRAY + "Usage: /kit get <kitname>");
    })
    .register();
```

## 3. Add Subcommands

Use `.addSubCommand()` to create subcommands like `/kit get <kitname>` for retrieving kits.

```java
Command.create("kit")
    .setDescription("Gives a player a kit.")
    .setPermission("myplugin.kit.use")
    .requires(SenderType.PLAYER)
    .setExecutor(ctx -> {
        ctx.getPlayer().sendMessage(ChatColor.GOLD + "Available kits: knight, archer");
        ctx.getPlayer().sendMessage(ChatColor.GRAY + "Usage: /kit get <kitname>");
    })
    .addSubCommand(Command.create("get")
        .setDescription("Gives you a specific kit.")
        .addArgument("kitname", String.class, "The name of the kit to receive.")
        .setExecutor(ctx -> {
            Player player = ctx.getPlayer();
            String kitName = ctx.getArgument("kitname");
            player.sendMessage(ChatColor.GREEN + "You received the '" + kitName + "' kit!");
        })
    )
    .register();
```

## 4. Support Console and Player Commands

Create subcommands like `/kit give <target> <kitname>` that can be executed by both players and the console using `SenderType.ANY`.

```java
Command.create("kit")
    .setDescription("Gives a player a kit.")
    .setPermission("myplugin.kit.use")
    .requires(SenderType.PLAYER)
    .setExecutor(ctx -> {
        ctx.getPlayer().sendMessage(ChatColor.GOLD + "Available kits: knight, archer");
        ctx.getPlayer().sendMessage(ChatColor.GRAY + "Usage: /kit get <kitname>");
    })
    .addSubCommand(Command.create("give")
        .setDescription("Gives a kit to another player.")
        .setPermission("myplugin.kit.give")
        .requires(SenderType.ANY)
        .addArgument("target", Player.class, "The player to give the kit to.")
        .addArgument("kitname", String.class, "The name of the kit.")
        .setExecutor(ctx -> {
            Player target = ctx.getArgument("target");
            String kitName = ctx.getArgument("kitname");
            target.sendMessage(ChatColor.GOLD + "You have received the '" + kitName + "' kit!");
            ctx.getSender().sendMessage(ChatColor.GREEN + "Gave kit '" + kitName + "' to " + target.getName());
        })
    )
    .register();
```

## 5. Use Enum Arguments

Add arguments with enum types, such as `Difficulty`, for automatic parsing and tab-completion, as shown in the `/kit setworlddifficulty <difficulty>` subcommand.

```java
import org.bukkit.Difficulty;

Command.create("kit")
    .setDescription("Gives a player a kit.")
    .setPermission("myplugin.kit.use")
    .requires(SenderType.PLAYER)
    .setExecutor(ctx -> {
        ctx.getPlayer().sendMessage(ChatColor.GOLD + "Available kits: knight, archer");
        ctx.getPlayer().sendMessage(ChatColor.GRAY + "Usage: /kit get <kitname>");
    })
    .addSubCommand(Command.create("setworlddifficulty")
        .setDescription("Set the world's difficulty.")
        .setPermission("myplugin.admin.difficulty")
        .requires(SenderType.PLAYER)
        .addArgument("difficulty", Difficulty.class, "The difficulty to set.")
        .setExecutor(ctx -> {
            Difficulty difficulty = ctx.getArgument("difficulty");
            ctx.getPlayer().getWorld().setDifficulty(difficulty);
            ctx.getPlayer().sendMessage(ChatColor.GREEN + "World difficulty set to " + difficulty.name());
        })
    )
    .register();
```

## 6. Complete Example

Combine all elements into a fully functional `/kit` command with subcommands, permissions, and type-safe arguments.

```java
import com.yourpackage.commandapi.Command;
import com.yourpackage.commandapi.CommandAPI;
import com.yourpackage.commandapi.sender.SenderType;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandAPI.init(this);

        Command.create("kit")
            .setAliases("kits")
            .setDescription("Gives a player a kit.")
            .setPermission("myplugin.kit.use")
            .requires(SenderType.PLAYER)
            .setExecutor(ctx -> {
                ctx.getPlayer().sendMessage(ChatColor.GOLD + "Available kits: knight, archer");
                ctx.getPlayer().sendMessage(ChatColor.GRAY + "Usage: /kit get <kitname>");
            })
            .addSubCommand(Command.create("get")
                .setDescription("Gives you a specific kit.")
                .addArgument("kitname", String.class, "The name of the kit to receive.")
                .setExecutor(ctx -> {
                    Player player = ctx.getPlayer();
                    String kitName = ctx.getArgument("kitname");
                    player.sendMessage(ChatColor.GREEN + "You received the '" + kitName + "' kit!");
                })
            )
            .addSubCommand(Command.create("give")
                .setDescription("Gives a kit to another player.")
                .setPermission("myplugin.kit.give")
                .requires(SenderType.ANY)
                .addArgument("target", Player.class, "The player to give the kit to.")
                .addArgument("kitname", String.class, "The name of the kit.")
                .setExecutor(ctx -> {
                    Player target = ctx.getArgument("target");
                    String kitName = ctx.getArgument("kitname");
                    target.sendMessage(ChatColor.GOLD + "You have received the '" + kitName + "' kit!");
                    ctx.getSender().sendMessage(ChatColor.GREEN + "Gave kit '" + kitName + "' to " + target.getName());
                })
            )
            .addSubCommand(Command.create("setworlddifficulty")
                .setDescription("Set the world's difficulty.")
                .setPermission("myplugin.admin.difficulty")
                .requires(SenderType.PLAYER)
                .addArgument("difficulty", Difficulty.class, "The difficulty to set.")
                .setExecutor(ctx -> {
                    Difficulty difficulty = ctx.getArgument("difficulty");
                    ctx.getPlayer().getWorld().setDifficulty(difficulty);
                    ctx.getPlayer().sendMessage(ChatColor.GREEN + "World difficulty set to " + difficulty.name());
                })
            )
            .register();

        getLogger().info("Custom Command API example loaded!");
    }
}
```