package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.DamageUtil;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcListener implements Listener {
    
    private final Map<UUID, Boolean> npcInvulnerability = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNpcDamage(NPCDamageByEntityEvent event) {
        // Use the new, more reliable get_npc_by_NPC_object method.
        NpcAPI.getNpc(event.getNPC()).ifPresent(npc -> {
            UUID npcId = npc.getUniqueId();

            if (npcInvulnerability.containsKey(npcId)) {
                event.setCancelled(true);
                return;
            }
            
            Entity damager = event.getDamager();
            Player attacker = null;

            if (damager instanceof Arrow) {
                if (((Arrow) damager).getShooter() instanceof Player) {
                    attacker = (Player) ((Arrow) damager).getShooter();
                }
            } else if (damager instanceof Player) {
                attacker = (Player) damager;
            }

            if (attacker != null) {
                ItemStack weapon = attacker.getItemInHand();
                double damage = DamageUtil.getDamage(weapon);
                event.setDamage(damage);

                npc.getHandle().setProtected(true);
                npcInvulnerability.put(npcId, true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // The correct, safe check for existence before modification.
                        if (npc.getHandle() != null && npc.getHandle().isSpawned()) {
                            npc.getHandle().setProtected(false);
                        }
                        npcInvulnerability.remove(npcId);
                    }
                }.runTaskLater(NpcAPI.getPlugin(), npc.getHitDelay());
            }
        });
    }

    @EventHandler
    public void onNpcDeath(NPCDeathEvent event) {
        npcInvulnerability.remove(event.getNPC().getUniqueId());
        
        NpcAPI.getNpc(event.getNPC()).ifPresent(npc -> {
            int respawnTime = npc.getRespawnTime();
            
            if (respawnTime >= 0) {
                Location respawnLocation = npc.getInitialSpawnLocation();
                if (respawnLocation == null) return;
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // The correct, safe check using the NPC's owning registry.
                        if (event.getNPC().getOwningRegistry().getById(event.getNPC().getId()) != null && !event.getNPC().isSpawned()) {
                            event.getNPC().spawn(respawnLocation);
                            event.getNPC().setProtected(false);
                            Entity npcEntity = event.getNPC().getEntity();
                            if (npcEntity instanceof LivingEntity) {
                                ((LivingEntity) npcEntity).setMaximumNoDamageTicks(npc.getHitDelay());
                            }
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
        npcInvulnerability.remove(event.getNPC().getUniqueId());
        NpcAPI.unregisterNpc(event.getNPC().getUniqueId());
    }
}