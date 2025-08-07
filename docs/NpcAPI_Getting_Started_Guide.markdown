# NpcAPI Getting Started Guide

The `NpcAPI` is a simple yet powerful, version-agnostic wrapper around the Citizens plugin, designed for Spigot/BungeeCord plugins. It provides a direct, fluent interface for creating, configuring, and controlling server-side NPCs with behaviors like pathfinding, combat, and automatic respawning.

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

Enable the `NpcAPI` in your plugin's `onEnable` method to set up necessary listeners and tasks.

```java
import com.arkflame.flamecore.npcapi.NpcAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        NpcAPI.enable(this);
    }
}
```

## 3. Create an NPC

Create and spawn an NPC with a single call to `Npc.create()`, specifying its name and spawn location.

```java
import com.arkflame.flamecore.npcapi.Npc;
import org.bukkit.Location;
import org.bukkit.World;

World world = // ... get world
Location spawnLocation = new Location(world, 100, 64, 100);

Npc guard = Npc.create("Guard", spawnLocation);
```

## 4. Configure NPC Properties

Configure the NPC using setter methods for properties like spawn location, respawn time, persistence, and appearance.

### a) Basic Properties

Set the spawn location, respawn time, and persistence.

```java
Location newLocation = // ... get location
guard.setSpawnLocation(newLocation); // Updates spawn point or teleports if spawned
guard.setRespawnTime(30); // Respawns after 30 seconds; < 0 disables
guard.setPersistent(true); // Saves NPC to Citizens' config
```

### b) Appearance

Set the NPC’s skin and entity type, and equip items compatible with Bukkit 1.8 and modern versions.

```java
import com.arkflame.flamecore.npcapi.Npc.EquipmentSlot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.EntityType;

guard.setSkin("Notch"); // Player skin, fetched asynchronously
guard.setEntityType(EntityType.ZOMBIE); // Change to zombie (skins for PLAYER only)
guard.setEquipment(EquipmentSlot.MAIN_HAND, new ItemStack(Material.DIAMOND_SWORD));
guard.setEquipment(EquipmentSlot.HELMET, new ItemStack(Material.DIAMOND_HELMET));
guard.setEquipment(EquipmentSlot.OFF_HAND, new ItemStack(Material.SHIELD)); // Safe for 1.9+
```

### c) Combat

Configure combat properties, including custom damage, hit frequency, and no-damage ticks for responsive combat.

```java
guard.setCustomDamage(8.0); // Deals 4 hearts of damage
guard.setHitFrequency(15); // Attacks every 15 ticks
guard.setNoDamageTicks(10); // Can be damaged 2 times per second
```

### d) Allies

Manage entities that the NPC ignores during `attackNearby()` behavior.

```java
import org.bukkit.entity.Player;

Player somePlayer = // ... get player
guard.addAlly(somePlayer); // NPC ignores this player
guard.removeAlly(somePlayer); // NPC can attack player again
guard.clearAllies(); // Clear all allies
```

## 5. Control NPC Behavior

Command the NPC to perform actions, with new behaviors automatically stopping previous ones.

```java
import org.bukkit.entity.Entity;

Location targetLocation = // ... get location
Entity targetEntity = // ... get entity

guard.attack(targetEntity); // Attack a specific entity
guard.attackNearby(20); // Attack non-allies within 20 blocks
guard.moveTo(targetLocation); // Move to a location
guard.follow(targetEntity); // Follow an entity
guard.stopBehavior(); // Stop all actions, become idle
```

## 6. Manage NPC Lifecycle

Remove or retrieve NPCs created by your plugin.

### a) Destroy NPCs

Permanently remove an NPC or all NPCs created by your plugin.

```java
import java.util.Collection;

guard.destroy(); // Remove a single NPC
Collection<Npc> myNpcs = NpcAPI.getAllNpcs(); // Get all plugin NPCs
NpcAPI.destroyAll(); // Remove all plugin NPCs

@Override
public void onDisable() {
    NpcAPI.destroyAll(); // Clean up all NPCs
}
```

### b) Retrieve NPCs

Find NPCs by ID or iterate through all managed NPCs.

```java
import java.util.Optional;
import java.util.UUID;

UUID npcId = guard.getUniqueId();
Optional<Npc> retrievedNpc = NpcAPI.getNpc(npcId);
retrievedNpc.ifPresent(npc -> npc.follow(somePlayer));

for (Npc npc : NpcAPI.getAllNpcs()) {
    System.out.println("Found managed NPC: " + npc.getName());
}
```

## 7. Full Example: Nether Guardian NPC

Create and configure a persistent NPC with combat and guard behavior.

```java
import com.arkflame.flamecore.npcapi.Npc;
import com.arkflame.flamecore.npcapi.Npc.EquipmentSlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

Player player = // ... get player
Location spawnLocation = player.getLocation().add(5, 0, 0);

Npc guardian = Npc.create("Nether Guardian", spawnLocation);
guardian.setPersistent(true);
guardian.setRespawnTime(60); // Respawn after 1 minute
guardian.setSkin("Herobrine");
guardian.setEquipment(EquipmentSlot.MAIN_HAND, new ItemStack(Material.NETHERITE_SWORD));
guardian.setEquipment(EquipmentSlot.CHESTPLATE, new ItemStack(Material.NETHERITE_CHESTPLATE));
guardian.setCustomDamage(10.0); // 5 hearts of damage
guardian.setHitFrequency(20); // Attacks once per second
guardian.attackNearby(25); // Attack non-allies within 25 blocks
```