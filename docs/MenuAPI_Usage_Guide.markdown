# MenuAPI Usage Guide

The `MenuAPI` enables the creation of powerful, protected, and animated GUI menus in Spigot/BungeeCord.

## Initializing MenuAPI

Initialize the API once in your plugin's `onEnable` method.

```java
@Override
public void onEnable() {
    MenuAPI.init(this);
}
```

## Creating a Simple Menu

Use `MenuBuilder` to define the size and title, then `.open()` it for a player.

```java
import com.arkflame.flamecore.menuapi.MenuBuilder;
import com.arkflame.flamecore.menuapi.ItemBuilder;
import org.bukkit.Material;

new MenuBuilder(27, "&8My First Menu")
    .setItem(13, new ItemBuilder(Material.DIAMOND)
                      .displayName("&b&lSpecial Diamond")
                      .lore("&7This is a very cool item.")
                      .build())
    .open(player);
```

## Adding Click Actions

Use `.onClick()` on an `ItemBuilder` to execute code when an item is clicked.

```java
MenuItem closeButton = new ItemBuilder(Material.BARRIER)
    .displayName("&c&lClose Menu")
    .onClick(event -> {
        // The event is a standard InventoryClickEvent
        event.getWhoClicked().closeInventory();
    })
    .build();

new MenuBuilder(9, "&4Admin Panel")
    .setItem(8, closeButton)
    .open(player);
```

## Creating Animated Items

Chain animation frames to an `ItemBuilder` to create animated menu items. The system handles updates automatically.

```java
MenuItem animatedSword = new ItemBuilder(Material.WOOD_SWORD)
    .animationInterval(10) // Update every 10 ticks (0.5 seconds)
    .displayName("&7Loading weapon...")
    .addMaterialFrame(Material.WOOD_SWORD)
    .addMaterialFrame(Material.STONE_SWORD)
    .addMaterialFrame(Material.IRON_SWORD)
    .addMaterialFrame(Material.DIAMOND_SWORD)
    .addNameFrame("&7Basic Sword")
    .addNameFrame("&8Strong Sword")
    .addNameFrame("&fAdvanced Sword")
    .addNameFrame("&b&lUltimate Sword")
    .build();

new MenuBuilder(9, "Weapon Forge")
    .setItem(4, animatedSword)
    .open(player);
```

## Allowing Items to be Taken

By default, items are protected. Use `.takeable(true)` to allow players to take an item from the menu.

```java
MenuItem freeGold = new ItemBuilder(Material.GOLD_INGOT)
    .displayName("&6Free Gold!")
    .takeable(true) // This allows the player to take the item
    .build();

new MenuBuilder(9, "&eDaily Rewards")
    .setItem(4, freeGold)
    .open(player);
```