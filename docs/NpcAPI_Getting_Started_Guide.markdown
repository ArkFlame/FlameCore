# NpcAPI Getting Started Guide

The `NpcAPI` is a version-agnostic wrapper around the Citizens plugin, providing a simple, fluent interface for creating, controlling, and managing interactive NPCs with behaviors like pathfinding, combat, and automatic respawning in Spigot/BungeeCord plugins.

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

## 3. Create a Basic NPC

Use the fluent `Npc.builder()` to create, configure, and spawn an NPC with a name and skin. By default, NPCs are temporary and hittable.

```java
import com.arkflame.flamecore.npcapi.Npc;
import org.bukkit.Location;
import org.bukkit.World;

World world = // ... get world
Location spawnLocation = new Location(world, 100, 64, 100);

Npc guard = Npc.builder("Guard")
    .skin("Notch") // Fetches skin asynchronously
    .location(spawnLocation)
    .buildAndSpawn();
```

## 4. Make NPCs Persistent and Respawnable

Configure NPCs to persist across server restarts and respawn after being killed using `.persistent()` and `.respawnTime()`.

```java
Location bossRoomLocation = // ... get location

Npc boss = Npc.builder("&c&lMagma Lord")
    .skin("Jeb_")
    .location(bossRoomLocation)
    .persistent(true) // Saves NPC in Citizens' config
    .respawnTime(60) // Respawns 60 seconds after death
    .buildAndSpawn();
```

## 5. Control NPC Behavior

Use high-level methods to manage NPC movement, combat, and guard behavior.

### a) Movement

Direct NPCs to walk to a location, follow a player, or stop all behavior.

```java
import org.bukkit.entity.Player;

Location market = // ... get location
Player playerToFollow = // ... get player

guard.moveTo(market); // Walk to a specific spot
guard.follow(playerToFollow); // Follow a player
guard.stop(); // Stop all movement and behavior
```

### b) Combat

Configure NPCs to attack a specific player with automatic pathfinding and damage calculation (based on 1.8 PvP values).

```java
Player target = // ... get player
guard.attack(target); // Attack a specific player
guard.stop(); // Stop the current attack
```

### c) Guard Mode (Attack Nearby)

Enable NPCs to automatically attack the nearest valid player (e.g., non-creative) within a specified radius.

```java
boss.attackNearby(15.0); // Attack non-creative players within 15 blocks
boss.stop(); // Stop guard mode
```

## 6. Manage NPCs

Safely find and manage NPCs created by your plugin.

### a) Find NPCs

Locate NPCs by proximity or retrieve all NPCs created by your plugin.

```java
import java.util.List;
import java.util.Optional;

Optional<Npc> nearestNpc = NpcAPI.getNearest(player.getLocation());
nearestNpc.ifPresent(npc -> {
    player.sendMessage("The nearest NPC is " + npc.getName());
});

List<Npc> nearbyNpcs = NpcAPI.getNearby(player.getLocation(), 20.0);
```

### b) Remove NPCs

Remove specific or all NPCs, including cleanup for temporary NPCs.

```java
import java.util.Collection;

Collection<Npc> myNpcs = NpcAPI.getAll(); // Get all plugin NPCs
NpcAPI.destroyAll(); // Permanently destroy all plugin NPCs

@Override
public void onDisable() {
    NpcAPI.destroyAllTemporary(); // Clean up temporary NPCs
}
```