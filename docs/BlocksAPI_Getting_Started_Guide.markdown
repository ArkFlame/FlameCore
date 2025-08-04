# BlocksAPI Getting Started Guide

The `BlocksAPI` provides a version-agnostic, asynchronous interface for capturing and restoring block states in Spigot/BungeeCord plugins, with a throttled queue to prevent server lag. This guide outlines the essential steps to use the API effectively.

## 1. Initialize BlocksAPI

Initialize the `BlocksAPI` in your plugin's `onEnable` method to set up version detection and the block processing queue.

```java
import com.arkflame.flamecore.blocksapi.BlocksAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        BlocksAPI.init(this);
    }
}
```

## 2. Capture a Block's State

Asynchronously capture a block's state using `getBlockAsync`, which returns a `CompletableFuture<BlockWrapper>` containing all necessary data for restoration.

```java
import com.arkflame.flamecore.blocksapi.BlocksAPI;
import com.arkflame.flamecore.blocksapi.BlockWrapper;
import org.bukkit.Location;
import org.bukkit.entity.Player;

Player player = // ... get player
Location targetLocation = player.getTargetBlock(null, 5).getLocation();

BlocksAPI.getBlockAsync(targetLocation).thenAccept(blockWrapper -> {
    if (blockWrapper != null) {
        player.sendMessage("You captured a " + blockWrapper.getMaterialName() + " block!");
    }
});
```

## 3. Restore a Block's State

Use `setBlock` to restore a block's state at a specified location, leveraging the saved `BlockWrapper` for accurate placement.

```java
import com.arkflame.flamecore.blocksapi.BlockWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {
    private final Map<UUID, BlockWrapper> clipboard = new HashMap<>();

    @EventHandler
    public void onBlockCopy(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getItem() == null || event.getItem().getType() != Material.STICK) {
            return;
        }
        Player player = event.getPlayer();
        Location sourceLocation = event.getClickedBlock().getLocation();
        BlocksAPI.getBlockAsync(sourceLocation).thenAccept(wrapper -> {
            clipboard.put(player.getUniqueId(), wrapper);
            player.sendMessage("Block copied to your clipboard!");
        });
    }

    @EventHandler
    public void onBlockPaste(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getItem() == null || event.getItem().getType() != Material.STICK) {
            return;
        }
        Player player = event.getPlayer();
        BlockWrapper savedWrapper = clipboard.get(player.getUniqueId());
        if (savedWrapper == null) {
            player.sendMessage("Your clipboard is empty!");
            return;
        }
        Location pasteLocation = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
        BlocksAPI.setBlock(pasteLocation, savedWrapper);
        player.sendMessage("Pasting block from clipboard...");
    }
}
```

## 4. Persist Block States

Serialize a `BlockWrapper` to a string for persistent storage (e.g., in a file or database) and restore it later, ensuring perfect block state restoration even after a server restart.

```java
import com.arkflame.flamecore.blocksapi.BlocksAPI;
import com.arkflame.flamecore.blocksapi.BlockWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;

public class PersistentBlockListener implements Listener {
    private final MyPlugin plugin;
    private final Map<Location, String> brokenBlocks = new HashMap<>();

    public PersistentBlockListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DIAMOND_ORE) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.sendMessage("This block is protected and will regenerate!");
        BlocksAPI.getBlockAsync(block.getLocation()).thenAccept(wrapper -> {
            String serializedBlock = wrapper.serialize();
            brokenBlocks.put(block.getLocation(), serializedBlock);
            BlocksAPI.setBlock(block.getLocation(), new BlockWrapper("BEDROCK", (byte)0, null));
            new BukkitRunnable() {
                @Override
                public void run() {
                    String savedState = brokenBlocks.remove(block.getLocation());
                    if (savedState != null) {
                        BlocksAPI.setBlock(block.getLocation(), savedState);
                    }
                }
            }.runTaskLater(plugin, 20 * 10); // 10 seconds
        });
    }
}
```