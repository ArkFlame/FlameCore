package com.arkflame.flamecore.npcapi;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Comparator;

/**
 * A controller task that scans for targets and delegates the actual combat
 * to a sub-task (NpcAttackTask). This avoids code duplication and separates concerns.
 * It also includes logic to prevent the NPC from getting stuck on an unreachable target.
 */
class AttackNearbyTask extends BukkitRunnable {
    private final Npc npc;
    private final double radius;
    private final double radiusSquared;
    private NpcAttackTask currentAttackSubTask;

    // State variables for stuck detection
    private int stuckAttempts = 0;
    private Location lastLocation;

    public AttackNearbyTask(Npc npc, double radius) {
        this.npc = npc;
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    @Override
    public void run() { // Runs each 10 ticks
        if (!npc.isSpawned() || npc.behavior != Behavior.ATTACKING_NEARBY) {
            this.cancel(); // Stop if the NPC is despawned or behavior changed.
            return;
        }

        if (npc.targetEntity != null) {
            LivingEntity currentTarget = (LivingEntity) npc.targetEntity;
            
            // Check if the current target is still valid using our centralized method.
            if (NpcAttackTask.isValidTarget(npc, currentTarget, radiusSquared)) {
                // The target is valid. Now, check for the "stuck" condition before returning.

                Location currentLocation = npc.getLocation();
                
                // Check if NPC has moved more than 1 block (distance squared > 1)
                boolean hasMoved = lastLocation != null && currentLocation.distanceSquared(lastLocation) > 1.0;
                // Check if target is within hittable range (distance < 3, so distance squared < 9)
                boolean canHitTarget = currentLocation.distanceSquared(currentTarget.getLocation()) < 9.0;
                
                if (!hasMoved && !canHitTarget) {
                    stuckAttempts++;
                } else {
                    // Reset the counter if the NPC moved or the target is in range.
                    stuckAttempts = 0;
                }
                
                // Update last location for the next check. Use clone to avoid reference issues.
                this.lastLocation = currentLocation.clone();

                // If stuck for 4 attempts (40 ticks), force a target switch.
                if (stuckAttempts >= 4) {
                    // Try to find a new target, excluding the current one.
                    LivingEntity newTarget = findNearestTarget(currentTarget);
                    
                    // Clean up the old attack sub-task regardless of the outcome.
                    if (this.currentAttackSubTask != null) {
                        try { this.currentAttackSubTask.cancel(); } catch (IllegalStateException ignored) {}
                        this.currentAttackSubTask = null;
                    }
                    
                    if (newTarget != null) {
                        // A new target was found, switch to it.
                        npc.targetEntity = newTarget;
                    } else {
                        // FALLBACK: No other target found. Keep the current one and try again.
                        // The entity is already the target, so no change is needed.
                    }
                    
                    // Reset the stuck counter and create a new attack task for the determined target (new or fallback).
                    stuckAttempts = 0;
                    this.currentAttackSubTask = new NpcAttackTask(npc);
                    this.currentAttackSubTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 1L);
                    return; // We're done for this tick.
                }

                // If target is valid and we're not stuck, let the sub-task handle it.
                return;
            }
        }

        // If we reach here, the current target is invalid (dead, too far, etc.) or was null.
        
        // 1. Clean up the old sub-task if it exists.
        if (this.currentAttackSubTask != null) {
            try {
                this.currentAttackSubTask.cancel();
            } catch (IllegalStateException ignored) {
                // The sub-task may have already cancelled itself, which is fine.
            }
            this.currentAttackSubTask = null;
        }
        
        // 2. Clear the invalid target and reset stuck detection state.
        npc.targetEntity = null;
        stuckAttempts = 0;
        lastLocation = null;

        // 3. Find the nearest new valid target.
        LivingEntity newTarget = findNearestTarget();

        // 4. If a new target was found, assign it and start a new attack sub-task.
        if (newTarget != null) {
            npc.targetEntity = newTarget;
            this.currentAttackSubTask = new NpcAttackTask(npc);
            this.currentAttackSubTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 1L);
        }
    }
    
    /**
     * Finds the nearest valid target for the NPC, with an option to exclude an entity.
     * This version is optimized to only search for entities near the NPC.
     *
     * @param excludeEntity The entity to ignore during the search (can be null).
     * @return The closest valid LivingEntity, or null if none are found.
     */
    private LivingEntity findNearestTarget(LivingEntity excludeEntity) {
        if (!npc.isSpawned()) return null;
        
        // Use getNearbyEntities for much better performance than iterating all world entities.
        return npc.getLocation().getWorld()
                .getNearbyEntities(npc.getLocation(), radius, radius, radius).stream()
                .filter(entity -> entity instanceof LivingEntity) // Ensure we only consider living entities
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> !entity.equals(excludeEntity)) // Exclude the specified entity
                .filter(entity -> NpcAttackTask.isValidTarget(npc, entity, radiusSquared)) // Use the streamlined method to filter for valid targets.
                .min(Comparator.comparingDouble(entity -> npc.getLocation().distanceSquared(entity.getLocation())))
                .orElse(null);
    }

    /**
     * Finds the nearest valid target for the NPC.
     * This version is for convenience and does not exclude any entity.
     *
     * @return The closest valid LivingEntity, or null if none are found.
     */
    private LivingEntity findNearestTarget() {
        return findNearestTarget(null);
    }
    
    /**
     * Overridden to ensure the sub-task is also cancelled, preventing it from running loose.
     */
    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        if (currentAttackSubTask != null) {
            try {
                currentAttackSubTask.cancel();
            } catch (IllegalStateException ignored) {
                // Ignore, task is already stopped.
            }
            currentAttackSubTask = null;
        }
    }
}