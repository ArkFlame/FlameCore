# PotionsAPI Getting Started Guide

The `PotionsAPI` provides a version-agnostic, thread-safe interface for managing potion effects in Spigot/BungeeCord plugins, offering safe access to `PotionEffectType`, effect application, removal, and inspection with case-insensitive lookups.

## 1. Get a PotionEffectType

Safely retrieve a `PotionEffectType` using `PotionsAPI.get()`, which returns an `Optional` to handle invalid names. Lookups are case-insensitive.

```java
import com.arkflame.flamecore.potionsapi.PotionsAPI;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import java.util.Optional;

Player player = // ... get player
Optional<PotionEffectType> speedType = PotionsAPI.get("SPEED");

speedType.ifPresent(type -> {
    player.sendMessage("Found PotionEffectType: " + type.getName());
});
```

Alternatively, use `getOrNull()` when you are certain the effect exists.

```java
PotionEffectType regenerationType = PotionsAPI.getOrNull("REGENERATION");
if (regenerationType != null) {
    // Use the type
}
```

## 2. Check for Effects

Check if a player has a specific potion effect using either a `PotionEffectType` or its name.

```java
PotionEffectType strengthType = PotionsAPI.getOrNull("STRENGTH");
if (strengthType != null && PotionsAPI.hasEffect(player, strengthType)) {
    player.sendMessage("You feel strong!");
}

// Check directly by name
if (PotionsAPI.hasEffect(player, "INVISIBILITY")) {
    player.sendMessage("No one can see you...");
}
```

## 3. Apply Effects

Apply potion effects in a thread-safe manner, suitable for async tasks. The amplifier is zero-indexed (0 = Level I, 1 = Level II, etc.), and duration is in server ticks (20 ticks = 1 second).

```java
Optional<PotionEffectType> jumpType = PotionsAPI.get("JUMP");
jumpType.ifPresent(type -> {
    PotionsAPI.addEffect(player, type, 20 * 30, 1); // Jump Boost II for 30 seconds
});
```

For more control, apply a pre-built `PotionEffect`.

```java
import org.bukkit.potion.PotionEffect;

PotionEffect hasteEffect = new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 60, 2, false, false); // Haste III, 1 minute, no particles
PotionsAPI.addEffect(player, hasteEffect);
```

## 4. Remove Effects

Remove specific or all potion effects from a player in a thread-safe manner.

```java
// Remove by type
PotionsAPI.get("WEAKNESS").ifPresent(type -> PotionsAPI.removeEffect(player, type));

// Remove by name
PotionsAPI.removeEffect(player, "SLOW");

// Remove all effects
PotionsAPI.removeAllEffects(player);
player.sendMessage("All your potion effects have been cleared.");
```

## 5. Retrieve Active Effects

Inspect active potion effects to retrieve details like amplifier or remaining duration.

```java
Optional<PotionEffectType> regenType = PotionsAPI.get("REGENERATION");
regenType.ifPresent(type -> {
    Optional<PotionEffect> activeRegen = PotionsAPI.getEffect(player, type);
    activeRegen.ifPresent(effect -> {
        int amplifier = effect.getAmplifier(); // 0 for Regen I, 1 for Regen II, etc.
        int durationInSeconds = effect.getDuration() / 20;
        player.sendMessage("You have Regeneration " + (amplifier + 1) + " for " + durationInSeconds + " more seconds.");
    });
});
```

## 6. Full Example: Warrior's Fury Command

Create a command that grants a player Strength I and Resistance I for 15 seconds.

```java
import com.arkflame.flamecore.potionsapi.PotionsAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class FuryCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;
        PotionEffectType strength = PotionsAPI.getOrNull("STRENGTH");
        PotionEffectType resistance = PotionsAPI.getOrNull("DAMAGE_RESISTANCE");

        if (strength == null || resistance == null) {
            player.sendMessage("Error: Could not apply buff. Potion types not found.");
            return true;
        }

        int durationTicks = 20 * 15; // 15 seconds
        int amplifier = 0; // Level I
        PotionsAPI.addEffect(player, strength, durationTicks, amplifier);
        PotionsAPI.addEffect(player, resistance, durationTicks, amplifier);
        player.sendMessage("You feel the Warrior's Fury! (Strength & Resistance)");

        return true;
    }
}
```