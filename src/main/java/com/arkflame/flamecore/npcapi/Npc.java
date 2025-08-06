package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.CitizensAPI;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Npc {
    private final NPC citizensNpc;
    private final UUID flameId;
    private BukkitRunnable behaviorTask;
    
    // State fields
    public Location initialSpawnLocation;
    public int respawnTime = -1;
    public boolean persistent = false;
    public int hitDelay = 10;
    public EntityType entityType = EntityType.PLAYER;
    
    public Behavior behavior = Behavior.IDLE;
    public Entity targetEntity;
    public Location targetLocation;

    /**
     * The ONLY constructor. It is now package-private and called exclusively by the builder.
     */
    Npc(NPC citizensNpc, UUID flameId) {
        this.citizensNpc = citizensNpc;
        this.flameId = flameId; // Use the provided FlameCore UUID
        if (citizensNpc.getEntity() != null) {
            this.entityType = citizensNpc.getEntity().getType();
        }
    }

    public static Builder builder(String name) {
        if (!NpcAPI.isEnabled()) {
            throw new IllegalStateException("NpcAPI is not enabled because Citizens plugin is not installed.");
        }
        return new Builder(name);
    }
    
    // --- Setters & Getters ---
    public void setInitialSpawnLocation(Location location) { this.initialSpawnLocation = location; }
    public void setEntityType(EntityType type) { this.entityType = type; }
    public void setRespawnTime(int seconds) { this.respawnTime = seconds; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
    public void setHitDelay(int ticks) { this.hitDelay = ticks; }
    public EntityType getEntityType() { return this.entityType; }
    public int getRespawnTime() { return respawnTime; }
    public Location getInitialSpawnLocation() { return initialSpawnLocation; }
    public int getHitDelay() { return hitDelay; }
    public NPC getHandle() { return this.citizensNpc; }
    public String getName() { return citizensNpc.getName(); }
    public Location getLocation() { return isSpawned() ? citizensNpc.getEntity().getLocation() : citizensNpc.getStoredLocation(); }
    public boolean isSpawned() { return citizensNpc.isSpawned(); }
    public UUID getUniqueId() { return flameId; } // This now returns OUR UUID.
    public UUID getCitizensId() { return citizensNpc.getUniqueId(); } // Getter for the Citizens UUID

    // --- Public API Methods ---
    public void spawn() {
        if (initialSpawnLocation == null) {
            NpcAPI.getPlugin().getLogger().warning("Attempted to spawn NPC '" + getName() + "' without an initial location set.");
            return;
        }
        citizensNpc.spawn(initialSpawnLocation);
        Entity npcEntity = citizensNpc.getEntity();
        if (npcEntity instanceof LivingEntity) {
            ((LivingEntity) npcEntity).setMaximumNoDamageTicks(getHitDelay());
        }
    }
    
    public void despawn() { citizensNpc.despawn(); }
    public void teleport(Location location) { CitizensCompat.teleport(citizensNpc, location); }
    
    public void destroy() {
        stopBehavior();
        if (this.persistent) {
            File npcFile = new File(NpcAPI.getNpcsFolder(), getUniqueId() + ".yml");
            if (npcFile.exists()) npcFile.delete();
        }
        citizensNpc.destroy();
    }
    
    public void save() {
        if (!this.persistent) return;
        if (this.initialSpawnLocation == null) {
            NpcAPI.getPlugin().getLogger().warning("Attempted to save persistent NPC '" + getName() + "' without an initial spawn location. Aborting save.");
            return;
        }
        NpcSerializer.serialize(this);
    }
    

    // --- Behavior Methods ---
    public void stopBehavior() {
        if (behaviorTask != null) {
            try { behaviorTask.cancel(); } catch (IllegalStateException ignored) {}
            behaviorTask = null;
        }
        if (isSpawned()) {
            citizensNpc.getNavigator().cancelNavigation();
        }
        this.behavior = Behavior.IDLE;
        this.targetEntity = null;
        this.targetLocation = null;
    }
    
    public void attack(Entity target) {
        stopBehavior();
        this.behavior = Behavior.ATTACKING;
        this.targetEntity = target;
        // Start the task for the new behavior
        this.behaviorTask = new NpcAttackTask(this);
        this.behaviorTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 5L);
    }
    
    public void attackNearby() {
        attackNearby(10);
    }

    public void attackNearby(double radius) {
        stopBehavior();
        this.behavior = Behavior.ATTACKING_NEARBY;
        // Start the new controller task
        this.behaviorTask = new NpcGuardTask(this, radius);
        this.behaviorTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 20L);
    }

    public void moveTo(Location location) {
        stopBehavior();
        citizensNpc.getNavigator().setTarget(location);
    }

    public void follow(Entity target) {
        stopBehavior();
        citizensNpc.getNavigator().setTarget(target, false);
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
        private int hitDelay = 10;

        private Builder(String name) { this.name = name; }
        public Builder location(Location location) { this.location = location; return this; }
        public Builder skin(String skinName) { this.skinName = skinName; return this; }
        public Builder type(EntityType entityType) { this.entityType = entityType; return this; }
        public Builder persistent(boolean persistent) { this.persistent = persistent; return this; }
        public Builder respawnTime(int seconds) { this.respawnTime = seconds; return this; }
        public Builder hitDelay(int ticks) { this.hitDelay = Math.max(0, ticks); return this; }
        public Location getLocation() { return location; }

        /**
         * The definitive build method. It now constructs the Npc object atomically.
         */
        public Npc build() {
            // Step 1: Create the Citizens NPC in the temporary registry.
            NPCRegistry registry = CitizensCompat.getTemporaryNPCRegistry();
            NPC npc = registry.createNPC(this.entityType, this.name);
            
            // Step 2: Create our authoritative wrapper with its own unique FlameCore ID.
            Npc wrappedNpc = new Npc(npc, UUID.randomUUID());
            
            // Register it in our map
            NpcAPI.registerNpc(wrappedNpc);
            
            // Step 3: Configure the wrapper with all builder properties.
            wrappedNpc.setPersistent(this.persistent);
            wrappedNpc.setRespawnTime(this.respawnTime);
            wrappedNpc.setHitDelay(this.hitDelay);
            wrappedNpc.setEntityType(this.entityType);
            wrappedNpc.setInitialSpawnLocation(this.location);
            
            // Step 4: Configure the Citizens NPC with traits.
            npc.setProtected(false);
            npc.getOrAddTrait(Owner.class).setOwner(name);
            npc.getOrAddTrait(LookClose.class).lookClose(true);

            if (skinName != null && entityType == EntityType.PLAYER) {
                applySkin(npc, skinName);
            }
            
            // Step 5: Register the complete, configured wrapper in our API.
            NpcAPI.registerNpc(wrappedNpc);
            
            // Step 6: If persistent, save it to a file named after OUR UUID.
            if (this.persistent) {
                wrappedNpc.save();
            }
            
            return wrappedNpc;
        }

        public Npc buildAndSpawn() {
            if (location == null) {
                NpcAPI.getPlugin().getLogger().warning("Attempted to call buildAndSpawn() on an NPC builder ('" + this.name + "') without a location set. Aborting spawn.");
                return null;
            }
            Npc npc = build();
            if (npc != null) {
                npc.spawn();
            }
            return npc;
        }
        
        private void applySkin(NPC npc, String skinName) {
            CompletableFuture.runAsync(() -> CitizensCompat.setSkin(npc, skinName));
        }
    }
}