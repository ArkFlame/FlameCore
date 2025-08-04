# CommandAPI Getting Started Guide

The `CommandAPI` provides a fluent, builder-style interface for creating commands with subcommands, permissions, and type-safe arguments in Spigot/BungeeCord plugins.

## 1. Initialize CommandAPI

Initialize the `CommandAPI` in your plugin's `onEnable` method to set up command registration.

```java
import com.arkflame.flamecore.commandapi.CommandAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        CommandAPI.init(this);
    }
}
```

## 2. Create a Simple Command

Define a basic command with its logic and register it.

```java
import com.arkflame.flamecore.commandapi.Command;
import com.arkflame.flamecore.commandapi.sender.SenderType;

Command.create("heal")
    .setDescription("Heals the player.")
    .requires(SenderType.PLAYER)
    .setExecutor(ctx -> {
        ctx.getPlayer().setHealth(20.0);
        ctx.getPlayer().sendMessage("You have been healed.");
    })
    .register();
```

## 3. Add Required Arguments

Use `.addArgument()` to define mandatory arguments that the user must provide.

```java
import org.bukkit.entity.Player;

Command.create("msg")
    .addArgument("target", Player.class, "The player to message.")
    .addArgument("message", String.class, "The message to send.")
    .setExecutor(ctx -> {
        Player target = ctx.getArgument("target");
        String message = ctx.getArgument("message");
        target.sendMessage("From " + ctx.getSender().getName() + ": " + message);
    })
    .register();
```

## 4. Add Optional Arguments

Use `.addOptionalArgument()` for non-required arguments, defined after all required arguments.

```java
Command.create("broadcast")
    .setPermission("myplugin.broadcast")
    .addArgument("message", String.class, "The message to broadcast.")
    .addOptionalArgument("prefix", String.class, "An optional prefix.")
    .setExecutor(ctx -> {
        String message = ctx.getArgument("message");
        String prefix = ctx.getArgumentOrDefault("prefix", "[Broadcast]");
        Bukkit.broadcastMessage(prefix + " " + message);
    })
    .register();
```

## 5. Build a Command Tree (Subcommands)

Chain `.addSubCommand()` to create complex commands with subcommands, such as `/gmc` and `/gmc <player>`.

```java
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

Command.create("gmc")
    .setDescription("Sets your gamemode to Creative.")
    .requires(SenderType.PLAYER)
    .setExecutor(ctx -> {
        ctx.getPlayer().setGameMode(GameMode.CREATIVE);
        ctx.getPlayer().sendMessage("Gamemode set to Creative.");
    })
    .addSubCommand(Command.create("target")
        .setPermission("myplugin.gmc.other")
        .addArgument("player", Player.class, "The target player.")
        .setExecutor(ctx -> {
            Player target = ctx.getArgument("player");
            target.setGameMode(GameMode.CREATIVE);
            target.sendMessage("Your gamemode was set to Creative.");
            ctx.getSender().sendMessage("Set " + target.getName() + "'s gamemode to Creative.");
        })
    )
    .register();
```

## 6. Complete Example: Set Block Command

Create a command that sets a block at the player's target location, using `MaterialAPI` for version-agnostic material selection.

```java
import com.arkflame.flamecore.commandapi.Command;
import com.arkflame.flamecore.commandapi.sender.SenderType;
import com.arkflame.flamecore.materialapi.MaterialAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

Command.create("setblock")
    .setPermission("myplugin.setblock")
    .requires(SenderType.PLAYER)
    .addArgument("material", String.class, "The block material (e.g., STONE or DIAMOND_BLOCK).")
    .setExecutor(ctx -> {
        Player player = ctx.getPlayer();
        String materialName = ctx.getArgument("material");
        Optional<Material> material = MaterialAPI.get(materialName);

        if (material.isEmpty()) {
            player.sendMessage("Invalid material: " + materialName);
            return;
        }

        Location targetLocation = player.getTargetBlock(null, 5).getLocation();
        targetLocation.getBlock().setType(material.get());
        player.sendMessage("Set block to " + material.get().name() + " at " + targetLocation.toVector());
    })
    .register();
```