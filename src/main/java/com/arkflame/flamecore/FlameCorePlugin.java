package com.arkflame.flamecore;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.arkflame.flamecore.actionbarapi.ActionBarAPI;
import com.arkflame.flamecore.blocksapi.BlockWrapper;
import com.arkflame.flamecore.blocksapi.BlocksAPI;
import com.arkflame.flamecore.bossbarapi.BossBarAPI;
import com.arkflame.flamecore.bossbarapi.BossBarManager;
import com.arkflame.flamecore.bossbarapi.enums.BarColor;
import com.arkflame.flamecore.colorapi.ColorAPI;
import com.arkflame.flamecore.commandapi.Command;
import com.arkflame.flamecore.commandapi.CommandAPI;
import com.arkflame.flamecore.commandapi.sender.SenderType;
import com.arkflame.flamecore.configapi.ConfigAPI;
import com.arkflame.flamecore.fakeblocksapi.FakeBlock;
import com.arkflame.flamecore.fakeblocksapi.FakeBlocksAPI;
import com.arkflame.flamecore.langapi.LangAPI;
import com.arkflame.flamecore.materialapi.MaterialAPI;
import com.arkflame.flamecore.menuapi.ItemBuilder;
import com.arkflame.flamecore.menuapi.MenuAPI;
import com.arkflame.flamecore.menuapi.MenuBuilder;
import com.arkflame.flamecore.menuapi.MenuItem;
import com.arkflame.flamecore.schematicapi.Schematic;
import com.arkflame.flamecore.schematicapi.SchematicAPI;
import com.arkflame.flamecore.titleapi.TitleAPI;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FlameCorePlugin extends JavaPlugin implements Listener {

    // --- State Management for Tools ---
    private final Set<UUID> blockToolUsers = new HashSet<>();
    private final Map<UUID, BlockWrapper> blockClipboard = new HashMap<>();
    private final Map<UUID, Location> schematicPos1 = new HashMap<>();
    private final Map<UUID, Location> schematicPos2 = new HashMap<>();
    private final Map<UUID, Schematic> schematicClipboard = new HashMap<>();

    @Override
    public void onEnable() {
        // --- Initialize All Core APIs ---
        CommandAPI.init(this);
        ConfigAPI.init(this);
        LangAPI.init(this);
        MenuAPI.init(this);
        BlocksAPI.init(this);
        SchematicAPI.init(this);
        FakeBlocksAPI.init(this);
        BossBarManager.init(this);

        // Register this class as a listener for our tools
        getServer().getPluginManager().registerEvents(this, this);

        // Register the main command
        registerFlameCoreCommand();

        getLogger().info("FlameCore Example Plugin has been enabled!");
    }

    private void registerFlameCoreCommand() {
        Command.create("flamecore")
                .setPermission("flamecore.admin")
                .setAliases("fc")
                .setDescription("Demonstrates the features of the ArkFlame Core API.")
                .setExecutor(ctx -> LangAPI.getMessage("commands.help").send(ctx.getSender()))

                // --- API Demonstrations ---

                .addSubCommand(Command.create("title")
                        .addArgument("title", String.class, "The main title text.")
                        .addArgument("subtitle", String.class, "The subtitle text.")
                        .addOptionalArgument("player", Player.class, "The target player (optional).")
                        .setExecutor(ctx -> {
                            // Use getArgumentOrDefault for the optional argument. If not provided, it
                            // defaults to the command sender.
                            Player target = ctx.getArgumentOrDefault("player", ctx.getPlayer());
                            if (target == null) {
                                LangAPI.getMessage("errors.player-not-found").send(ctx.getSender());
                                return;
                            }

                            // Get required arguments by name.
                            String title = ctx.getArgument("title");
                            String subtitle = ctx.getArgument("subtitle");

                            TitleAPI.create().title(title).subtitle(subtitle).send(target);
                            LangAPI.getMessage("commands.title.sent").with("player", target.getName())
                                    .send(ctx.getSender());
                        }))

                .addSubCommand(Command.create("actionbar")
                        .addArgument("message", String.class, "The message to send.")
                        .addOptionalArgument("player", Player.class, "The target player (optional).")
                        .setExecutor(ctx -> {
                            Player target = ctx.getArgumentOrDefault("player", ctx.getPlayer());
                            if (target == null) {
                                LangAPI.getMessage("errors.player-not-found").send(ctx.getSender());
                                return;
                            }

                            String message = ctx.getArgument("message");

                            ActionBarAPI.create(message).send(target);
                            LangAPI.getMessage("commands.actionbar.sent").with("player", target.getName())
                                    .send(ctx.getSender());
                        }))

                .addSubCommand(Command.create("bossbar")
                        .addArgument("text", String.class, "The text for the boss bar.")
                        .addOptionalArgument("player", Player.class, "The target player (optional).")
                        .setExecutor(ctx -> {
                            Player target = ctx.getArgumentOrDefault("player", ctx.getPlayer());
                            if (target == null) {
                                LangAPI.getMessage("errors.player-not-found").send(ctx.getSender());
                                return;
                            }

                            String text = ctx.getArgument("text");

                            BossBarAPI bar = BossBarAPI.create().text(text).color(BarColor.PURPLE).progress(1.0);
                            bar.addPlayer(target);
                            getServer().getScheduler().runTaskLater(this, bar::destroy, 20 * 10);
                            LangAPI.getMessage("commands.bossbar.sent").with("player", target.getName())
                                    .send(ctx.getSender());
                        }))

                .addSubCommand(Command.create("sendmessage")
                        .addArgument("message", String.class, "A message with ColorAPI formatting.")
                        .setExecutor(ctx -> ColorAPI.colorize(ctx.getArgument("message")).send(ctx.getSender())))

                .addSubCommand(Command.create("menu")
                        .requires(SenderType.PLAYER)
                        .setExecutor(ctx -> openExampleMenu(ctx.getPlayer())))

                .addSubCommand(Command.create("blocksapi")
                        .requires(SenderType.PLAYER)
                        .setExecutor(ctx -> {
                            UUID uuid = ctx.getPlayer().getUniqueId();
                            if (blockToolUsers.contains(uuid)) {
                                blockToolUsers.remove(uuid);
                                LangAPI.getMessage("commands.blocksapi.disabled").send(ctx.getSender());
                            } else {
                                blockToolUsers.add(uuid);
                                LangAPI.getMessage("commands.blocksapi.enabled").send(ctx.getSender());
                            }
                        }))

                .addSubCommand(Command.create("fakeblock")
                        .requires(SenderType.PLAYER)
                        .setExecutor(ctx -> {
                            Player player = ctx.getPlayer();
                            Location targetLoc = player.getTargetBlock(null, 5).getLocation().add(0, 1, 0);
                            FakeBlock.builder(targetLoc, Material.DIAMOND_BLOCK)
                                    .duration(10)
                                    .send(player);
                            LangAPI.getMessage("commands.fakeblock.sent")
                                    .with("x", targetLoc.getBlockX())
                                    .with("y", targetLoc.getBlockY())
                                    .with("z", targetLoc.getBlockZ())
                                    .send(player);
                        }))

                .addSubCommand(Command.create("schematic")
                        .setAliases("schem")
                        .addSubCommand(Command.create("pos1").requires(SenderType.PLAYER).setExecutor(ctx -> {
                            schematicPos1.put(ctx.getPlayer().getUniqueId(), ctx.getPlayer().getLocation());
                            LangAPI.getMessage("commands.schematic.pos1_set").send(ctx.getSender());
                        }))
                        .addSubCommand(Command.create("pos2").requires(SenderType.PLAYER).setExecutor(ctx -> {
                            schematicPos2.put(ctx.getPlayer().getUniqueId(), ctx.getPlayer().getLocation());
                            LangAPI.getMessage("commands.schematic.pos2_set").send(ctx.getSender());
                        }))
                        .addSubCommand(Command.create("copy").requires(SenderType.PLAYER).setExecutor(ctx -> {
                            Player p = ctx.getPlayer();
                            if (!schematicPos1.containsKey(p.getUniqueId())
                                    || !schematicPos2.containsKey(p.getUniqueId())) {
                                LangAPI.getMessage("commands.schematic.no_selection").send(p);
                                return;
                            }
                            LangAPI.getMessage("commands.schematic.copy_start").send(p);
                            SchematicAPI.copy(schematicPos1.get(p.getUniqueId()), schematicPos2.get(p.getUniqueId()))
                                    .thenAccept(schem -> {
                                        schematicClipboard.put(p.getUniqueId(), schem);
                                        LangAPI.getMessage("commands.schematic.copy_success").send(p);
                                    });
                        }))
                        .addSubCommand(Command.create("paste").requires(SenderType.PLAYER).setExecutor(ctx -> {
                            Player p = ctx.getPlayer();
                            if (!schematicClipboard.containsKey(p.getUniqueId())) {
                                LangAPI.getMessage("commands.schematic.no_clipboard").send(p);
                                return;
                            }
                            LangAPI.getMessage("commands.schematic.paste_start").send(p);
                            schematicClipboard.get(p.getUniqueId()).paste(p.getLocation(), success -> {
                                LangAPI.getMessage("commands.schematic.paste_success").send(p);
                            });
                        }))
                        .addSubCommand(Command.create("save")
                                .addArgument("name", String.class, "The name of the schematic file.")
                                .requires(SenderType.PLAYER)
                                .setExecutor(ctx -> {
                                    Player p = ctx.getPlayer();
                                    if (!schematicClipboard.containsKey(p.getUniqueId())) {
                                        LangAPI.getMessage("commands.schematic.no_clipboard").send(p);
                                        return;
                                    }
                                    String name = ctx.getArgument("name");
                                    File schemFolder = new File(getDataFolder(), "schematics");
                                    schemFolder.mkdirs();
                                    File schemFile = new File(schemFolder, name + ".arkschem");
                                    LangAPI.getMessage("commands.schematic.save_start").with("name", name).send(p);
                                    schematicClipboard.get(p.getUniqueId()).save(schemFile).thenRun(() -> {
                                        LangAPI.getMessage("commands.schematic.save_success").with("name", name)
                                                .send(p);
                                    });
                                })))
                .register();
    }

    private void openExampleMenu(Player player) {
        // Construcción del ítem animado con nombre "Beacon" + color alternante
        MenuItem animatedItem = new ItemBuilder(MaterialAPI.getOrAir("BEACON"))
                .animationInterval(2)
                .addNameFrame("<#FFFFFF>&lB")
                .addNameFrame("<#FFFFFF>&lBe")
                .addNameFrame("<#FFFFFF>&lBea")
                .addNameFrame("<#FFFFFF>&lBeac")
                .addNameFrame("<#FFFFFF>&lBeaco")
                .addNameFrame("<#FFFFFF>&lBeacon")
                .addNameFrame("<#FF5733>&lBeacon")
                .addNameFrame("<#FFFFFF>&lBeacon")
                .addNameFrame("<#FF5733>&lBeacon")
                .addLoreFrame(Arrays.asList("&7This item has an", "&7animated name color!"))
                .onClick(e -> LangAPI.getMessage("commands.menu.clicked").send(e.getWhoClicked()))
                .build();

        MenuBuilder menu = new MenuBuilder(27, "<#FFC300>Animated Menu Example")
                .setItem(13, animatedItem); // Centro del menú

        // Posiciones relativas alrededor del centro (posición 13)
        int[] glassSlots = { 4, 12, 14, 10, 16, 0, 8, 22 };
        String[] rainbowHex = {
                "#FF0000", "#FF7F00", "#FFFF00", "#00FF00",
                "#0000FF", "#4B0082", "#8F00FF", "#FF1493"
        };

        // Crear los 8 bloques de cristal que cambian de color
        for (int i = 0; i < glassSlots.length; i++) {
            int slot = glassSlots[i];
            String hex = rainbowHex[i % rainbowHex.length];
            ItemBuilder glassItem = new ItemBuilder(MaterialAPI.getOrAir("WHITE_STAINED_GLASS_PANE", "STAINED_GLASS"))
                    .animationInterval(2)
                    .addNameFrame("<" + hex + ">&l★")
                    .addNameFrame("<#FFFFFF>&l★")
                    .addNameFrame("<" + hex + ">&l★");
            menu.setItem(slot, glassItem.build());
        }

        // Abrir el menú al jugador
        menu.open(player);
    }

    // --- Listener for the Block Tool ---
    @EventHandler
    public void onBlockToolInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!blockToolUsers.contains(player.getUniqueId()) || event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Copy
            LangAPI.getMessage("commands.blocksapi.copying").send(player);
            BlocksAPI.getBlockAsync(event.getClickedBlock().getLocation()).thenAccept(wrapper -> {
                blockClipboard.put(player.getUniqueId(), wrapper);
                LangAPI.getMessage("commands.blocksapi.copied").send(player);
            });
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Paste
            if (!blockClipboard.containsKey(player.getUniqueId())) {
                LangAPI.getMessage("commands.blocksapi.no_clipboard").send(player);
                return;
            }
            Location pasteLocation = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
            BlocksAPI.setBlock(pasteLocation, blockClipboard.get(player.getUniqueId()));
            LangAPI.getMessage("commands.blocksapi.pasted").send(player);
        }
    }
}