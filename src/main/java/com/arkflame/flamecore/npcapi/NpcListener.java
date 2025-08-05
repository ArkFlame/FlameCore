package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.DamageUtil;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class NpcListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNpcDamage(EntityDamageByEntityEvent event) {
        // Check if the damaged entity is one of our managed NPCs.
        NpcAPI.getNpc(event.getEntity()).ifPresent(npc -> {
            Entity damager = event.getDamager();
            Player attacker = null;

            // Handle projectile damage (like arrows)
            if (damager instanceof Arrow) {
                Arrow arrow = (Arrow) damager;
                if (arrow.getShooter() instanceof Player) {
                    attacker = (Player) arrow.getShooter();
                }
            } else if (damager instanceof Player) {
                attacker = (Player) damager;
            }

            if (attacker != null) {
                // If an attacker is found, calculate and set the damage based on their weapon.
                ItemStack weapon = attacker.getItemInHand();
                double damage = DamageUtil.getDamage(weapon);
                event.setDamage(damage);
            }
        });
    }

    @EventHandler
    public void onNpcDeath(NPCDeathEvent event) {
        NpcAPI.getNpc(event.getNPC()).ifPresent(npc -> {
            int respawnTime = npc.getRespawnTime();
            
            if (respawnTime > 0) {
                // This NPC should respawn.
                Location respawnLocation = npc.getInitialSpawnLocation();
                if (respawnLocation == null) {
                    System.err.println("NPC " + npc.getName() + " has a respawn time but no initial spawn location was set!");
                    return;
                }
                
                // Schedule the respawn task.
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Check if the NPC object still exists in case it was manually destroyed
                        // during the respawn timer.
                        if (NpcAPI.getNpc(event.getNPC()).isPresent()) {
                            event.getNPC().spawn(respawnLocation);
                        }
                    }
                }.runTaskLater(NpcAPI.getPlugin(), respawnTime * 20L);
                
            } else {
                // No respawn time, destroy it permanently.
                npc.destroy();
            }
        });
    }
    
    @EventHandler
    public void onNpcRemove(NPCRemoveEvent event) {
        NpcAPI.unregisterNpc(event.getNPC().getUniqueId());
    }
}