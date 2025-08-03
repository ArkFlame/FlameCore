package com.arkflame.core.bossbarapi;

import com.arkflame.core.bossbarapi.bridge.BossBarBridge;
import com.arkflame.core.bossbarapi.enums.BarColor;
import com.arkflame.core.bossbarapi.enums.BarStyle;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A modern, fluent API for creating and managing Boss Bars.
 * This API is version-agnostic, working from 1.8 to 1.21+.
 * It automatically uses the native BossBar API on 1.9+ and falls back to a Wither-based
 * implementation on 1.8, hiding all complexity from the user.
 *
 * Example:
 * <pre>{@code
 * BossBarAPI myBar = BossBarAPI.create()
 *          .text("<#FF5733>Server TPS: &a19.9")
 *          .progress(0.99)
 *          .color(BarColor.GREEN);
 *
 * myBar.addPlayer(player);
 * }</pre>
 */
public class BossBarAPI {
    private final BossBarBridge bridge;
    private final Set<UUID> players = new HashSet<>();
    private boolean visible = true;

    BossBarAPI() {
        this.bridge = BossBarManager.createBridge();
    }

    /**
     * Creates a new BossBar.
     * @return A new BossBarAPI instance.
     */
    public static BossBarAPI create() {
        return new BossBarAPI();
    }

    public BossBarAPI text(String text) {
        bridge.setText(text);
        return this;
    }

    public BossBarAPI progress(double progress) {
        bridge.setProgress(progress);
        return this;
    }

    public BossBarAPI color(BarColor color) {
        bridge.setColor(color);
        return this;
    }

    public BossBarAPI style(BarStyle style) {
        bridge.setStyle(style);
        return this;
    }

    public BossBarAPI addPlayer(Player player) {
        if (players.add(player.getUniqueId())) {
            bridge.addPlayer(player);
        }
        return this;
    }

    public BossBarAPI removePlayer(Player player) {
        if (players.remove(player.getUniqueId())) {
            bridge.removePlayer(player);
        }
        return this;
    }

    public BossBarAPI addPlayers(Collection<Player> players) {
        players.forEach(this::addPlayer);
        return this;
    }

    public BossBarAPI removeAll() {
        new HashSet<>(players).forEach(uuid -> {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) {
                removePlayer(p);
            }
        });
        players.clear();
        return this;
    }

    public BossBarAPI setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            bridge.setVisible(visible);
        }
        return this;
    }

    /**
     * Permanently destroys this boss bar, removing it for all players
     * and cleaning up any associated resources (like Wither entities on 1.8).
     */
    public void destroy() {
        removeAll();
        bridge.destroy();
        BossBarManager.removeBar(this);
    }
    
    // Internal methods for the manager
    Set<UUID> getPlayers() { return players; }
    BossBarBridge getBridge() { return bridge; }
}