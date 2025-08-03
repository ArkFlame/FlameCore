package com.arkflame.core.actionbarapi;

import com.arkflame.core.colorapi.ColorAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * A modern, fluent API for sending action bars to players.
 * Integrates fully with ColorAPI for gradient and hex support.
 *
 * Example:
 * <pre>{@code
 * ActionBarAPI.create("<#FF5733>Mana: &b100 / 100")
 *             .send(player);
 * }</pre>
 */
public class ActionBarAPI {
    private final BaseComponent[] message;

    private ActionBarAPI(String text) {
        this.message = ColorAPI.colorize(text).toBungeeComponents();
    }

    /**
     * Creates a new ActionBarAPI builder with the specified message.
     * @param text The message to display, processed by ColorAPI.
     * @return A new ActionBarAPI instance.
     */
    public static ActionBarAPI create(String text) {
        return new ActionBarAPI(text);
    }

    /**
     * Sends the configured action bar to a specific player.
     */
    public void send(Player player) {
        if (player != null && player.isOnline()) {
            ActionBarPacketSender.send(player, message);
        }
    }

    /**
     * Sends the configured action bar to a collection of players.
     */
    public void send(Collection<? extends Player> players) {
        for (Player player : players) {
            send(player);
        }
    }

    /**
     * Sends the configured action bar to all online players.
     */
    public void sendToAll() {
        send(Bukkit.getOnlinePlayers());
    }
}