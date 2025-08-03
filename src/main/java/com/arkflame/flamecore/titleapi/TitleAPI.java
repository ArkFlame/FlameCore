package com.arkflame.flamecore.titleapi;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.arkflame.flamecore.colorapi.ColorAPI;

import java.util.Collection;

/**
 * A modern, fluent API for sending titles to players.
 * Integrates fully with ColorAPI for gradient and hex support.
 *
 * Example:
 * <pre>{@code
 * TitleAPI.create()
 *         .title("<#00BFFF>Level Up!</#FFFFFF>")
 *         .subtitle("&eYou are now level 10!")
 *         .fadeIn(10)
 *         .stay(40)
 *         .fadeOut(10)
 *         .send(player);
 * }</pre>
 */
public class TitleAPI {
    private BaseComponent[] title;
    private BaseComponent[] subtitle;
    private int fadeIn;
    private int stay;
    private int fadeOut;

    private TitleAPI() {
        // Default timings from Minecraft
        this.title = ColorAPI.colorize("").toBungeeComponents();
        this.subtitle = ColorAPI.colorize("").toBungeeComponents();
        this.fadeIn = 10;
        this.stay = 70;
        this.fadeOut = 20;
    }

    /**
     * Creates a new TitleAPI builder instance.
     * @return A new TitleAPI builder.
     */
    public static TitleAPI create() {
        return new TitleAPI();
    }

    /**
     * Sets the main title text. Processed by ColorAPI.
     */
    public TitleAPI title(String title) {
        this.title = ColorAPI.colorize(title).toBungeeComponents();
        return this;
    }

    /**
     * Sets the subtitle text. Processed by ColorAPI.
     */
    public TitleAPI subtitle(String subtitle) {
        this.subtitle = ColorAPI.colorize(subtitle).toBungeeComponents();
        return this;
    }

    /**
     * Sets the time in ticks for the title to fade in.
     */
    public TitleAPI fadeIn(int ticks) {
        this.fadeIn = ticks;
        return this;
    }

    /**
     * Sets the time in ticks for the title to stay on screen.
     */
    public TitleAPI stay(int ticks) {
        this.stay = ticks;
        return this;
    }

    /**
     * Sets the time in ticks for the title to fade out.
     */
    public TitleAPI fadeOut(int ticks) {
        this.fadeOut = ticks;
        return this;
    }

    /**
     * Sends the configured title to a specific player.
     */
    public void send(Player player) {
        if (player != null && player.isOnline()) {
            TitlePacketSender.send(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Sends the configured title to a collection of players.
     */
    public void send(Collection<? extends Player> players) {
        for (Player player : players) {
            send(player);
        }
    }

    /**
     * Sends the configured title to all online players.
     */
    public void sendToAll() {
        send(Bukkit.getOnlinePlayers());
    }
}