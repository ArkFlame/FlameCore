package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.api.trait.trait.Owner;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Npc {
    private final NPC citizensNpc;
    private BukkitRunnable attackTask;

    private Npc(NPC citizensNpc) {
        this.citizensNpc = citizensNpc;
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
    public Location getLocation() { return citizensNpc.getStoredLocation(); }
    public boolean isSpawned() { return citizensNpc.isSpawned(); }
    public void despawn() { citizensNpc.despawn(); }
    public void teleport(Location location) { CitizensCompat.teleport(citizensNpc, location); }

    public void destroy() {
        NpcAPI.unregisterNpc(this);
        citizensNpc.destroy();
    }
    
    // --- High-Level Behaviors ---
    public void moveTo(Location location) {
        citizensNpc.getNavigator().setTarget(location);
    }

    public void follow(Entity target) {
        citizensNpc.getNavigator().setTarget(target, false);
    }
    
    public void stopNavigation() {
        citizensNpc.getNavigator().cancelNavigation();
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

    public void attack(Entity target) {
        stopAttacking(); // Stop any previous attack task
        attackTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSpawned() || target == null || target.isDead() || !target.isValid()) {
                    stopAttacking();
                    return;
                }
                
                citizensNpc.getNavigator().setTarget(target, false);
                double distanceSquared = getLocation().distanceSquared(target.getLocation());
                
                if (distanceSquared < 9) { // 3 blocks
                    CitizensCompat.playSwingAnimation(citizensNpc);
                }
            }
        };
        attackTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 20L); // Attack logic runs every second
    }
    
    public void stopAttacking() {
        if (attackTask != null && !attackTask.isCancelled()) {
            attackTask.cancel();
            attackTask = null;
        }
        stopNavigation();
    }

    // --- Builder Class ---
    public static class Builder {
        private final String name;
        private Location location;
        private String skinName;
        private EntityType entityType = EntityType.PLAYER;

        private Builder(String name) { this.name = name; }
        public Builder location(Location location) { this.location = location; return this; }
        public Builder skin(String skinName) { this.skinName = skinName; return this; }
        public Builder type(EntityType entityType) { this.entityType = entityType; return this; }

        public Npc build() {
            NPC npc = CitizensAPI.getNPCRegistry().createNPC(entityType, name);
            npc.getOrAddTrait(Owner.class).setOwner(name);
            LookClose lookClose = npc.getOrAddTrait(LookClose.class);
            lookClose.lookClose(true);

            if (skinName != null && entityType == EntityType.PLAYER) {
                applySkin(npc, skinName);
            }
            Npc wrappedNpc = new Npc(npc);
            NpcAPI.registerNpc(wrappedNpc);
            return wrappedNpc;
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