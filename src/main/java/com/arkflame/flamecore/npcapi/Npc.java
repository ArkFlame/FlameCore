package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Npc {
    private final NPC citizensNpc;
    private BukkitRunnable behaviorTask;

    // State fields for persistence and behavior
    private Location initialSpawnLocation;
    private int respawnTime = -1; // in seconds, -1 means no respawn
    private boolean persistent = false;
    private int hitDelay = 20; // Default to 20 ticks (1 second)

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
    
    public static Builder builderFromConfig(Config config) {
        return NpcSerializer.deserialize(config);
    }
    
    // --- Internal Setters used by the Builder and Listener ---
    void setRespawnTime(int seconds) { this.respawnTime = seconds; }
    void setInitialSpawnLocation(Location location) { this.initialSpawnLocation = location; }
    void setPersistent(boolean persistent) { this.persistent = persistent; }
    void setHitDelay(int ticks) { this.hitDelay = ticks; } // New setter for the builder
    
    // --- Getters for internal use ---
    public int getRespawnTime() { return respawnTime; }
    public Location getInitialSpawnLocation() { return initialSpawnLocation; }
    NPC getCitizensNpc() { return citizensNpc; }
    NPC getHandle() { return citizensNpc; }
    public int getHitDelay() { return hitDelay; } // New getter for the listener

    // --- Public API Methods ---
    public UUID getUniqueId() { return citizensNpc.getUniqueId(); }
    public String getName() { return citizensNpc.getName(); }
    public Location getLocation() { return isSpawned() ? citizensNpc.getEntity().getLocation() : citizensNpc.getStoredLocation(); }
    public boolean isSpawned() { return citizensNpc.isSpawned(); }
    public void despawn() { citizensNpc.despawn(); }
    public void teleport(Location location) { CitizensCompat.teleport(citizensNpc, location); }

    /**
     * Permanently destroys this NPC. If it was persistent, its data file will be deleted.
     */
    public void destroy() {
        stopBehavior();
        if (this.persistent) {
            File npcFile = new File(NpcAPI.getNpcsFolder(), getUniqueId() + ".yml");
            if (npcFile.exists()) {
                npcFile.delete();
            }
        }
        // The NpcListener will handle unregistering from our map.
        citizensNpc.destroy();
    }
    
    /**
     * Saves the NPC's current state to its data file if it is persistent.
     */
    public void save() {
        if (this.persistent) {
            NpcSerializer.serialize(this);
        }
    }
    
    // --- High-Level Behaviors (Task-based) ---
    private void stopBehavior() {
        if (behaviorTask != null) {
            try { behaviorTask.cancel(); } catch (IllegalStateException ignored) {}
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

                if (currentTarget == null || currentTarget.isDead() || !currentTarget.isValid() || 
                    currentTarget.getLocation().distanceSquared(getLocation()) > radius * radius) {
                    
                    currentTarget = findNearestPlayer(radius);
                    
                    if (currentTarget != null) {
                        attack(currentTarget);
                    }
                }
            }
        };
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

    public void breakBlock(Block block) {
        stopBehavior();
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!isSpawned() || block.getType() == Material.AIR || ticks >= 60) {
                    if (isSpawned() && block.getType() != Material.AIR) {
                        block.breakNaturally();
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

    // --- Builder Class ---
    public static class Builder {
        private final String name;
        private Location location;
        private String skinName;
        private EntityType entityType = EntityType.PLAYER;
        private boolean persistent = false;
        private int respawnTime = -1;
        private int hitDelay = 10; // Default hit delay

        private Builder(String name) { this.name = name; }
        public Builder location(Location location) { this.location = location; return this; }
        public Builder skin(String skinName) { this.skinName = skinName; return this; }
        public Builder type(EntityType entityType) { this.entityType = entityType; return this; }
        public Builder persistent(boolean persistent) { this.persistent = persistent; return this; }
        public Builder respawnTime(int seconds) { this.respawnTime = seconds; return this; }
        public Builder hitDelay(int ticks) {
            this.hitDelay = ticks;
            return this;
        }

        public Npc build() {
            NPCRegistry registry = CitizensCompat.getTemporaryNPCRegistry();
            NPC npc = registry.createNPC(entityType, name);
            
            npc.setProtected(false);
            npc.getOrAddTrait(Owner.class).setOwner(name);
            npc.getOrAddTrait(LookClose.class).lookClose(true);

            if (skinName != null && entityType == EntityType.PLAYER) {
                applySkin(npc, skinName);
            }
            
            Npc wrappedNpc = new Npc(npc);
            wrappedNpc.setPersistent(this.persistent);
            wrappedNpc.setRespawnTime(this.respawnTime);
            wrappedNpc.setHitDelay(this.hitDelay); // Pass the hit delay
            
            if (this.persistent) {
                wrappedNpc.setInitialSpawnLocation(this.location);
                wrappedNpc.save();
            }
            
            return wrappedNpc;
        }

        /**
         * Creates and immediately spawns the NPC at the specified location,
         * applying the configured hit delay upon spawn.
         * @return The spawned Npc object.
         */
        public Npc buildAndSpawn() {
            if (location == null) {
                throw new IllegalStateException("Location must be set before calling buildAndSpawn()");
            }
            Npc npc = build();
            npc.setInitialSpawnLocation(location);
            
            // Spawn the NPC in the world
            npc.citizensNpc.spawn(location);

            Entity npcEntity = npc.citizensNpc.getEntity();
            if (npcEntity instanceof LivingEntity) {
                ((LivingEntity) npcEntity).setMaximumNoDamageTicks(npc.getHitDelay());
            }
            
            return npc;
        }
        
        private void applySkin(NPC npc, String skinName) {
            CompletableFuture.runAsync(() -> CitizensCompat.setSkin(npc, skinName));
        }
    }
}