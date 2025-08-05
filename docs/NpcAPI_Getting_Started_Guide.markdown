# NpcAPI Getting Started Guide

The `NpcAPI` provides a simple and powerful interface for controlling Citizens NPCs in Spigot/BungeeCord plugins, enabling features like spawning, movement, player interaction, and block breaking with a fluent, intuitive design. This guide outlines the essential steps to use the API effectively.

## 1. Dependencies

Add the CitizensNPCs dependency and repository to your `pom.xml` to enable NPC functionality. Ensure the Citizens plugin is installed on your server.

```xml
<repositories>
    <repository>
        <id>citizens-repo</id>
        <url>https://repo.citizensnpcs.co/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.citizensnpcs</groupId>
        <artifactId>citizens-main</artifactId>
        <version>2.0.30-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Replace `2.0.30-SNAPSHOT` with the desired CitizensNPCs version compatible with your server.

## 2. Initialize NpcAPI

Initialize the `NpcAPI` in your plugin's `onEnable` method to set up necessary listeners and tasks.

```java
import com.arkflame.flamecore.npcapi.NpcAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        NpcAPI.init(this);
    }
}
```

## 3. Create and Spawn an NPC

Use the fluent `Npc.builder()` to create, configure, and spawn an NPC with a name and skin in one chain.

```java
import com.arkflame.flamecore.npcapi.Npc;
import org.bukkit.Location;
import org.bukkit.World;

World world = // ... get world
Location spawnLocation = new Location(world, 100, 64, 100);

Npc guard = Npc.builder("Sir Reginald")
    .skin("Notch")
    .location(spawnLocation)
    .buildAndSpawn();
```

## 4. Make an NPC Move

Direct the NPC to walk to a specified location using the `.moveTo()` method.

```java
Location marketStall = new Location(world, 150, 65, 120);
guard.moveTo(marketStall);
```

## 5. Make an NPC Follow and Attack a Player

Configure the NPC to chase, look at, and attack a player using the `.attack()` method. Stop the attack with `.stopAttacking()` if needed.

```java
import org.bukkit.entity.Player;

Player targetPlayer = // ... get player
guard.attack(targetPlayer);

// To stop the attack
// guard.stopAttacking();
```

## 6. Simulate Block Breaking

Instruct the NPC to walk to a block and simulate breaking it using the `.breakBlock()` method.

```java
import org.bukkit.block.Block;

Block targetBlock = world.getBlockAt(105, 64, 100);
guard.breakBlock(targetBlock);
```