package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.api.trait.trait.Owner;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Npc {
    private final NPC citizensNpc;
    
    // Each NPC now manages its own behavior task. This is robust and simple.
    private BukkitRunnable behaviorTask;

    public Npc(NPC citizensNpc) {
        this.citizensNpc = citizensNpc;
        NpcAPI.registerNpc(this);
    }

    public static Builder builder(String name) {
        if (!NpcAPI.isEnabled()) {
            throw new IllegalStateException("NpcAPI is not enabled because Citizens plugin is not installed.");
        }
        return new Builder(name);
    }
    
    // --- Basic Getters & Methods ---
    public UUID getUniqueId() { return citizensNpc.getUniqueId(); }
    public String getName() { return citizensNpc.getName(); }
    public Location getLocation() { return isSpawned() ? citizensNpc.getEntity().getLocation() : citizensNpc.getStoredLocation(); }
    public boolean isSpawned() { return citizensNpc.isSpawned(); }
    public void despawn() { citizensNpc.despawn(); }
    public void teleport(Location location) { CitizensCompat.teleport(citizensNpc, location); }

    public void destroy() {
        stopBehavior();
        // The listener will call NpcAPI.unregisterNpc
        citizensNpc.destroy();
    }
    
    // --- High-Level Behaviors (Task-based) ---
    private void stopBehavior() {
        if (behaviorTask != null) {
            behaviorTask.cancel();
            behaviorTask = null;
        }
        if (isSpawned()) {
            citizensNpc.getNavigator().cancelNavigation();
        }
    }

    public void moveTo(Location location) {
        stopBehavior();
        citizensNpc.getNavigator().setTarget(location);
    }

    public void follow(Entity target) {
        stopBehavior();
        citizensNpc.getNavigator().setTarget(target, false);
    }
    
    public void attack(Entity target) {
        stopBehavior();
        behaviorTask = new BukkitRunnable() {
            private long lastAttackTime = 0;
            private final long attackInterval = 500;

            @Override
            public void run() {
                if (!isSpawned() || target == null || target.isDead() || !target.isValid()) {
                    stopBehavior();
                    cancel();
                    return;
                }
                
                citizensNpc.getNavigator().setTarget(target, false);
                
                if (getLocation().distanceSquared(target.getLocation()) < 9) { // Attack range
                    citizensNpc.getNavigator().cancelNavigation();
                    if (System.currentTimeMillis() - lastAttackTime > attackInterval) {
                        CitizensCompat.faceLocation(citizensNpc, target.getLocation());
                        CitizensCompat.playSwingAnimation(citizensNpc);
                        if (target instanceof LivingEntity) {
                            ((LivingEntity) target).damage(1.0, citizensNpc.getEntity());
                        }
                        lastAttackTime = System.currentTimeMillis();
                    }
                }
            }
        };
        behaviorTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 5L);
    }

    public void breakBlock(Block block) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!isSpawned() || block.getType() == Material.AIR || ticks >= 60) { // Timeout after 3 seconds
                    if (isSpawned() && block.getType() != Material.AIR) {
                        block.breakNaturally(); // Final break
                    }
                    cancel();
                    return;
                }
                CitizensCompat.faceLocation(citizensNpc, block.getLocation());
                CitizensCompat.playSwingAnimation(citizensNpc);
                ticks += 10;
            }
        }.runTaskTimer(NpcAPI.getPlugin(), 0L, 10L);
    }

        /**
     * NEW: Puts the NPC in "guard mode," causing it to attack the nearest player within a 10-block radius.
     */
    public void attackNearby() {
        attackNearby(10.0);
    }

    /**
     * NEW: Puts the NPC in "guard mode," causing it to attack the nearest player within the specified radius.
     * This method reuses the core .attack() logic.
     * @param radius The radius in blocks to search for players.
     */
    public void attackNearby(double radius) {
        stopBehavior();
        
        behaviorTask = new BukkitRunnable() {
            private Entity currentTarget;

            @Override
            public void run() {
                if (!isSpawned()) {
                    stopBehavior();
                    return;
                }

                // If the current target is invalid or has moved out of range, find a new one.
                if (currentTarget == null || currentTarget.isDead() || !currentTarget.isValid() || 
                    currentTarget.getLocation().distanceSquared(getLocation()) > radius * radius) {
                    
                    // Find the nearest valid player within the radius.
                    currentTarget = findNearestPlayer(radius);
                    
                    if (currentTarget != null) {
                        // Found a new target, tell the NPC to attack it.
                        // This will cancel this controller task and start a new, dedicated attack task.
                        attack(currentTarget);
                    }
                }
                // If the current target is still valid, the dedicated attack task is already running, so do nothing.
            }
        };
        // This controller task runs every second to check for new targets.
        behaviorTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 20L);
    }
    
    private Player findNearestPlayer(double radius) {
        if (!isSpawned()) return null;
        double radiusSquared = radius * radius;
        
        return getLocation().getWorld().getPlayers().stream()
            .filter(player -> !player.getUniqueId().equals(getUniqueId()))
            .filter(player -> player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR)
            .filter(player -> getLocation().distanceSquared(player.getLocation()) <= radiusSquared)
            .min(Comparator.comparingDouble(player -> getLocation().distanceSquared(player.getLocation())))
            .orElse(null);
    }

    // --- Builder Class ---
    public static class Builder {
        private final String name;
        private Location location;
        private String skinName;
        private EntityType entityType = EntityType.PLAYER;
        private boolean persistent = false; // NPCs are NOT persistent by default

        private Builder(String name) {
            this.name = name;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder skin(String skinName) {
            this.skinName = skinName;
            return this;
        }

        public Builder type(EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder persistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

    public Npc build() {
        NPCRegistry registry = persistent 
            ? CitizensAPI.getNPCRegistry() 
            : CitizensCompat.getTemporaryNPCRegistry();

        NPC npc = registry.createNPC(entityType, name);
            npc.setProtected(true);
            npc.getOrAddTrait(Owner.class).setOwner(name);
            LookClose lookClose = npc.getOrAddTrait(LookClose.class);
            lookClose.lookClose(true);

            if (skinName != null && entityType == EntityType.PLAYER) {
                applySkin(npc, skinName);
            }
            // No registration in our API needed anymore.
            return new Npc(npc);
        }

        public Npc buildAndSpawn() {
            if (location == null) {
                throw new IllegalStateException("Location must be set before calling buildAndSpawn()");
            }
            Npc npc = build();
            npc.citizensNpc.spawn(location);
            return npc;
        }

        private void applySkin(NPC npc, String skinName) {
            CompletableFuture.runAsync(() -> CitizensCompat.setSkin(npc, skinName));
        }
    }
}