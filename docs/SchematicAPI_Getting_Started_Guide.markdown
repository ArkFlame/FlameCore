# SchematicAPI Getting Started Guide

The `SchematicAPI` provides a robust interface for saving, loading, and pasting schematics in Spigot/BungeeCord plugins, enabling efficient handling of block structures such as arenas or restorable blocks.

## 1. Initialize SchematicAPI

Initialize the `SchematicAPI` in your plugin's `onEnable` method to set up the API and optionally restore schematics from a folder.

```java
import com.arkflame.core.schematicapi.SchematicAPI;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        SchematicAPI.init(this);
        File brokenBlocksFolder = new File(getDataFolder(), "broken_blocks");
        if (!brokenBlocksFolder.exists()) brokenBlocksFolder.mkdirs();
        SchematicAPI.restoreAllFromFolder(brokenBlocksFolder, true);
    }
}
```

## 2. Save and Paste an Arena

Save a selected region as a schematic and paste it at a specified location, useful for managing arenas or structures.

```java
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.io.File;

Player player = // ... get your player object
Location pos1 = // ... first corner of selection
Location pos2 = // ... second corner of selection
String arenaName = "mycoolarena";

// Save the arena
player.sendMessage("Saving arena...");
SchematicAPI.copy(pos1, pos2).thenAccept(schematic -> {
    File arenaFile = new File(plugin.getDataFolder() + "/arenas/", arenaName + ".arkschem");
    schematic.save(arenaFile).thenRun(() -> {
        player.sendMessage("Arena saved successfully!");
    });
});

// Paste the arena
Location pasteLocation = // ... where to paste the arena
player.sendMessage("Pasting arena, please wait...");
File fileToLoad = new File(plugin.getDataFolder() + "/arenas/", arenaName + ".arkschem");
SchematicAPI.load(fileToLoad).thenAccept(schematic -> {
    schematic.paste(pasteLocation, (success) -> {
        player.sendMessage("Arena has been pasted!");
    });
});
```

## 3. Persistent Block Breaking and Restoration

Save a block's state when broken and restore it later, useful for temporary block changes.

```java
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;

public class BlockListener implements Listener {
    private final MyPlugin plugin;

    public BlockListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        SchematicAPI.copy(block.getLocation()).thenAccept(schematic -> {
            schematic.setOrigin(block.getLocation());
            File brokenBlocksFolder = new File(plugin.getDataFolder(), "broken_blocks");
            String fileName = "block_" + block.getX() + "_" + block.getY() + "_" + block.getZ() + ".arkschem";
            schematic.save(new File(brokenBlocksFolder, fileName));
            new BukkitRunnable() {
                public void run() {
                    File fileToRestore = new File(brokenBlocksFolder, fileName);
                    if (fileToRestore.exists()) {
                        SchematicAPI.load(fileToRestore).thenAccept(loadedSchematic -> {
                            loadedSchematic.paste(loadedSchematic.getOrigin(), success -> fileToRestore.delete());
                        });
                    }
                }
            }.runTaskLater(plugin, 20 * 60 * 5); // 5 minutes
        });
    }
}
```