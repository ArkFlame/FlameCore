package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import com.arkflame.flamecore.npcapi.util.DamageUtil;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Npc {
    public enum EquipmentSlot {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS, MAIN_HAND, OFF_HAND
    }
    
    private final NPC citizensNpc;
    private final UUID flameId;
    private BukkitRunnable behaviorTask;
    
    // --- State Fields ---
    private final Set<UUID> allies = new HashSet<>();
    private double customDamage = -1.0;
    
    // --- Configurable Fields ---
    public Location initialSpawnLocation;
    public int respawnTime = -1;
    public boolean persistent = false;
    private int noDamageTicks = 10;
    private int hitFrequency = 10;
    public EntityType entityType = EntityType.PLAYER;
    
    public Behavior behavior = Behavior.IDLE;
    public Entity targetEntity;
    public Location targetLocation;


    /**
     * Public constructor, called exclusively by the static create method.
     */
    public Npc(NPC citizensNpc, UUID flameId) {
        this.citizensNpc = citizensNpc;
        this.flameId = flameId;
        if (citizensNpc.getEntity() != null) {
            this.entityType = citizensNpc.getEntity().getType();
        }
    }
    
    // --- Setters & Getters ---
    public void setInitialSpawnLocation(Location location) { this.initialSpawnLocation = location; }
    public void setEntityType(EntityType type) { this.entityType = type; }
    public void setRespawnTime(int seconds) { this.respawnTime = seconds; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
    public EntityType getEntityType() { return this.entityType; }
    public int getRespawnTime() { return respawnTime; }
    public Location getInitialSpawnLocation() { return initialSpawnLocation; }
    public NPC getHandle() { return this.citizensNpc; }
    public String getName() { return citizensNpc.getName(); }
    public Location getLocation() { return isSpawned() ? citizensNpc.getEntity().getLocation() : citizensNpc.getStoredLocation(); }
    public boolean isSpawned() { return citizensNpc.isSpawned(); }
    public UUID getUniqueId() { return flameId; }
    public UUID getCitizensId() { return citizensNpc.getUniqueId(); }

    public void setNoDamageTicks(int ticks) { this.noDamageTicks = Math.max(0, ticks); }
    public int getNoDamageTicks() { return this.noDamageTicks; }
    public void setHitFrequency(int ticks) { this.hitFrequency = Math.max(1, ticks); }
    public int getHitFrequency() { return this.hitFrequency; }
    public void setCustomDamage(double damage) { this.customDamage = damage; }
    public double getCustomDamage() { return this.customDamage; }

    public double getEffectiveDamage() {
        if (customDamage >= 0) {
            return customDamage;
        }

        ItemStack weapon = null;
        if (isSpawned()) {
            Entity entity = getHandle().getEntity();
            if (entity instanceof LivingEntity) {
                 weapon = ((LivingEntity) entity).getEquipment().getItemInHand();
            }
        }
        
        return DamageUtil.getDamage(weapon);
    }

    // --- Public API Methods ---
    public void spawn() {
        if (initialSpawnLocation == null) {
            NpcAPI.getPlugin().getLogger().warning("Attempted to spawn NPC '" + getName() + "' without an initial location set.");
            return;
        }
        if (!citizensNpc.isSpawned()) {
            citizensNpc.spawn(initialSpawnLocation);
        }
    }
    
    public void despawn() { citizensNpc.despawn(); }
    public void teleport(Location location) { CitizensCompat.teleport(citizensNpc, location); }
    
    public void destroy() {
        stopBehavior();
        if (this.persistent) {
            File npcFile = new File(NpcAPI.getNpcsFolder(), getUniqueId() + ".yml");
            if (npcFile.exists()) {
                npcFile.delete();
            }
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
    
    // --- Ally Management Methods ---
    public void addAlly(LivingEntity entity) { if (entity != null) this.allies.add(entity.getUniqueId()); }
    public void removeAlly(LivingEntity entity) { if (entity != null) this.allies.remove(entity.getUniqueId()); }
    public void clearAllies() { this.allies.clear(); }
    public boolean isAlly(Entity entity) { return allies.contains(entity.getUniqueId()); }

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
        if (!(target instanceof LivingEntity)) return;
        this.behavior = Behavior.ATTACKING;
        this.targetEntity = target;
        this.behaviorTask = new NpcAttackTask(this);
        this.behaviorTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 1L);
    }

    public void attackNearby() { attackNearby(10); }
    
    public void attackNearby(double radius) {
        stopBehavior();
        this.behavior = Behavior.ATTACKING_NEARBY;
        this.behaviorTask = new AttackNearbyTask(this, radius);
        this.behaviorTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 10L); 
    }

    public void moveTo(Location location) {
        stopBehavior();
        citizensNpc.getNavigator().setTarget(location);
    }

    public void follow(Entity target) {
        stopBehavior();
        citizensNpc.getNavigator().setTarget(target, false);
    }

    public void breakBlock(final Block block) {
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

    /**
     * Creates a new NPC with a given name and spawns it at a location.
     * This is the new, simplified entry point for creating NPCs.
     * @param name The name of the NPC.
     * @param spawnLocation The location where the NPC will be created and spawned.
     * @return The newly created Npc instance.
     */
    public static Npc create(String name, Location spawnLocation) {
        if (!NpcAPI.isEnabled()) {
            throw new IllegalStateException("NpcAPI is not enabled because Citizens plugin is not installed.");
        }
        
        NPCRegistry registry = CitizensCompat.getTemporaryNPCRegistry();
        // Default to PLAYER type, can be changed later with setEntityType()
        NPC citizensNpc = registry.createNPC(EntityType.PLAYER, name);

        Npc npc = new Npc(citizensNpc, UUID.randomUUID());
        
        // Configure default traits
        npc.getHandle().setProtected(false);
        npc.getHandle().getOrAddTrait(Owner.class).setOwner(name);
        npc.getHandle().getOrAddTrait(LookClose.class).lookClose(true);

        // Set the initial location
        npc.setSpawnLocation(spawnLocation);
        
        // Register and spawn
        NpcAPI.registerNpc(npc);
        npc.spawn();

        return npc;
    }

    /**
     * Sets the location where the NPC will spawn and respawn.
     * If the NPC is already spawned, it will be teleported to this new location.
     * @param location The new spawn location.
     */
    public void setSpawnLocation(Location location) {
        this.initialSpawnLocation = location;
        if (isSpawned()) {
            teleport(location);
        }
    }
    
    /**
     * Changes the skin of the NPC. This operation is performed asynchronously.
     * This only works for NPCs of EntityType.PLAYER.
     * @param skinName The name of the player whose skin should be used.
     */
    public void setSkin(final String skinName) {
        if (getEntityType() != EntityType.PLAYER) {
            NpcAPI.getPlugin().getLogger().warning("Attempted to set skin for a non-player NPC '" + getName() + "'.");
            return;
        }
        // Use Bukkit's scheduler for 1.8 compatibility
        new BukkitRunnable() {
            @Override
            public void run() {
                CitizensCompat.setSkin(getHandle(), skinName);
            }
        }.runTaskAsynchronously(NpcAPI.getPlugin());
    }

/**
     * Sets a piece of equipment for the NPC.
     * For 1.8 compatibility, this uses direct equipment setting.
     * The OFF_HAND slot is handled safely using reflection to ensure it only
     * runs on servers that support it (1.9+).
     * @param slot The equipment slot to modify.
     * @param item The item to place in the slot.
     */
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (!isSpawned() || getHandle().getEntity() == null) {
            NpcAPI.getPlugin().getLogger().warning("Attempted to set equipment on a despawned NPC '" + getName() + "'.");
            return;
        }

        LivingEntity entity = (LivingEntity) getHandle().getEntity();
        
        switch (slot) {
            case HELMET:
                entity.getEquipment().setHelmet(item);
                break;
            case CHESTPLATE:
                entity.getEquipment().setChestplate(item);
                break;
            case LEGGINGS:
                entity.getEquipment().setLeggings(item);
                break;
            case BOOTS:
                entity.getEquipment().setBoots(item);
                break;
            case MAIN_HAND:
                entity.getEquipment().setItemInHand(item);
                break;
            case OFF_HAND:
                try {
                    // This is the direct feature detection. Try to get the method.
                    Method setOffHand = entity.getEquipment().getClass().getMethod("setItemInOffHand", ItemStack.class);
                    // If we get here, the method exists. Now, invoke it.
                    setOffHand.invoke(entity.getEquipment(), item);
                } catch (NoSuchMethodException e) {
                    // This exception is expected on servers before 1.9. It's not an error.
                    NpcAPI.getPlugin().getLogger().warning("Cannot set off-hand item for NPC '" + getName() + "'. This server version does not support it.");
                } catch (Exception e) {
                    // Any other exception is a real error during invocation.
                    NpcAPI.getPlugin().getLogger().severe("Failed to set off-hand item for NPC '" + getName() + "' via reflection.");
                    e.printStackTrace();
                }
                break;
        }
    }
}