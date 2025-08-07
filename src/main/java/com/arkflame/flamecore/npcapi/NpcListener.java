package com.arkflame.flamecore.npcapi;

import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcListener implements Listener {
    private final Map<UUID, Boolean> npcProtected = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNpcDamage(NPCDamageByEntityEvent event) {
        Npc npc = NpcAPI.getNpc(event.getNPC()).orElse(null);
        if (npc == null) {
            return;
        }

        UUID npcId = npc.getUniqueId();
        if (npcProtected.containsKey(npcId)) {
            event.setCancelled(true);
            return;
        }

        Entity damager = event.getDamager();
        Player attacker = null;

        if (damager instanceof Arrow && ((Arrow) damager).getShooter() instanceof Player) {
            attacker = (Player) ((Arrow) damager).getShooter();
        } else if (damager instanceof Player) {
            attacker = (Player) damager;
        }

        if (attacker != null) {
            // Apply custom invulnerability period
            npcProtected.put(npcId, true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    npcProtected.remove(npcId);
                }
            }.runTaskLater(NpcAPI.getPlugin(), npc.getNoDamageTicks());
        }
    }

    /**
     * Handles all death-related logic for managed NPCs.
     */
    @EventHandler
    public void onNpcDeath(NPCDeathEvent event) {
        NpcAPI.getNpc(event.getNPC()).ifPresent(npc -> {
            npcProtected.remove(npc.getUniqueId());

            EntityDeathEvent deathEvent = event.getEvent();
            if (deathEvent != null) {
                // Handle custom drops
                deathEvent.getDrops().clear();
                // TODO: Drop custom items

                // Clear the death message for player-type NPCs
                if (deathEvent instanceof PlayerDeathEvent) {
                    ((PlayerDeathEvent) deathEvent).setDeathMessage(null);
                }
            }

            // Handle respawning or destruction
            int respawnTime = npc.getRespawnTime();
            if (respawnTime >= 0) {
                final Location respawnLocation = npc.getInitialSpawnLocation();
                if (respawnLocation == null) return;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (event.getNPC().getOwningRegistry().getById(event.getNPC().getId()) != null
                                && !event.getNPC().isSpawned()) {
                            event.getNPC().spawn(respawnLocation);
                        }
                    }
                }.runTaskLater(NpcAPI.getPlugin(), respawnTime * 20L);
            } else {
                npc.destroy();
            }
        });
    }

    @EventHandler
    public void onNpcRemove(NPCRemoveEvent event) {
        npcProtected.remove(event.getNPC().getUniqueId());
        NpcAPI.unregisterNpc(event.getNPC().getUniqueId());
    }
}