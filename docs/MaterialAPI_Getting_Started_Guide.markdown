# MaterialAPI Getting Started Guide

The `MaterialAPI` provides a simple and safe way to handle materials in Spigot/BungeeCord plugins, ensuring compatibility across different Minecraft server versions by supporting legacy and modern material names.

## 1. Get a Material with Fallbacks

Use `getOrAir` to retrieve a material by providing a list of possible names, starting with legacy names for older versions, ensuring the first valid material is returned.

```java
import com.arkflame.flamecore.materialapi.MaterialAPI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

Material logMaterial = MaterialAPI.getOrAir("LOG", "OAK_LOG");
Material gunpowder = MaterialAPI.getOrAir("SULPHUR", "GUNPOWDER");

ItemStack item = new ItemStack(logMaterial);
```

## 2. Safe Material Handling with Optional

Use `Optional` to safely handle potentially invalid material names from user configurations, avoiding `NullPointerException`.

```java
import java.util.Optional;
import org.bukkit.entity.Player;

Player player = // ... get your player object
String configuredMaterial = "SOME_INVALID_MATERIAL"; // From config.yml

Optional<Material> optionalMaterial = MaterialAPI.get(configuredMaterial);

optionalMaterial.ifPresent(material -> {
    player.sendMessage("Found the material: " + material.name());
    player.getInventory().addItem(new ItemStack(material));
});

if (!optionalMaterial.isPresent()) {
    player.sendMessage("Error: Material '" + configuredMaterial + "' is not valid!");
}
```

## 3. Use Helper Methods for Material Checks

Leverage built-in helper methods to check material types in a version-independent manner.

```java
import org.bukkit.inventory.ItemStack;

ItemStack someItem = // ... get an item stack
Material material = someItem.getType();

if (MaterialAPI.isArmor(material)) {
    player.sendMessage("That's a piece of armor!");
}

if (MaterialAPI.isTool(material)) {
    player.sendMessage("That's a tool!");
}
```