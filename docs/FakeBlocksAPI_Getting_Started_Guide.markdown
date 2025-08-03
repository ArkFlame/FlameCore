# FakeBlocksAPI Getting Started Guide

The `FakeBlocksAPI` enables Spigot/BungeeCord plugins to send per-player "fake" blocks that are resilient and can expire automatically, with changes processed client-side to avoid server lag. This guide outlines the essential steps to use the API effectively, integrating the `MaterialAPI` for version-agnostic material handling.

## 1. Initialize FakeBlocksAPI

Initialize the `FakeBlocksAPI` in your plugin's `onEnable` method to set up listeners and tasks.

```java
import com.arkflame.core.fakeblocksapi.FakeBlocksAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        FakeBlocksAPI.init(this);
    }
}
```

## 2. Send a Fake Block

Use the fluent `FakeBlock.builder()` to create and send a fake block to a specific player at a given location, using `MaterialAPI` to select materials safely across Minecraft versions.

```java
import com.arkflame.core.fakeblocksapi.FakeBlock;
import com.arkflame.core.materialapi.MaterialAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

Player player = // ... get player
Location location = // ... get location

Material diamondBlock = MaterialAPI.getOrAir("DIAMOND_BLOCK");
FakeBlock.builder(location, diamondBlock).send(player);
```

## 3. Send a Timed Fake Block

Chain `.duration()` to make the fake block disappear after a specified time, automatically restoring the original block, using `MaterialAPI` for material selection.

```java
Material goldBlock = MaterialAPI.getOrAir("GOLD_BLOCK");
FakeBlock.builder(location, goldBlock)
    .duration(10) // Duration in seconds
    .send(player);
```

## 4. Send a Fake Block with Legacy Data

Chain `.data()` to specify a legacy data value (e.g., for colored wool in Minecraft 1.8–1.12), using `MaterialAPI` to retrieve the material with version-agnostic names.

```java
Optional<Material> woolMaterial = MaterialAPI.get("WOOL");
woolMaterial.ifPresent(material -> {
    FakeBlock.builder(location, material)
        .data((byte) 14) // Red wool
        .send(player);
});
```

## 5. Restore Fake Blocks Manually

Manually restore a single fake block or all fake blocks for a player to their original state.

```java
// Restore a single fake block at a specific location
FakeBlocksAPI.restore(player, location);

// Restore all fake blocks for a player (automatically called on player quit)
FakeBlocksAPI.restoreAll(player);
```