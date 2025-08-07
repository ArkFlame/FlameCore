package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A self-contained task that manages an NPC actively attacking a single target.
 * This task runs every tick to provide fast and responsive combat.
 */
class NpcAttackTask extends BukkitRunnable {
    private final Npc npc;
    private final LivingEntity target;
    private int attackCooldownTicks;

    public NpcAttackTask(Npc npc) {
        this.npc = npc;
        // We can safely cast here because the Npc.attack() method checks the type.
        this.target = (LivingEntity) npc.targetEntity;
        this.attackCooldownTicks = 0;
    }
    
    @Override
    public void run() {
        // Stop the task if the NPC or target is no longer valid.
        if (!npc.isSpawned() || target == null || target.isDead() || !target.isValid()) {
            npc.stopBehavior(); // This also cancels the task.
            return;
        }
        
        // Decrement cooldown timer
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }

        // Make the NPC always face its target
        CitizensCompat.faceLocation(npc.getHandle(), target.getEyeLocation());
        
        // Check if the NPC is within attack range (e.g., 3 blocks, squared to 9).
        if (npc.getLocation().distanceSquared(target.getLocation()) < 9) {
            // In range, stop navigating to engage.
            npc.getHandle().getNavigator().cancelNavigation();

            // Check if the attack is off cooldown.
            if (attackCooldownTicks <= 0) {
                CitizensCompat.playSwingAnimation(npc.getHandle());
                target.damage(npc.getEffectiveDamage(), npc.getHandle().getEntity());
                
                // Use the OFFENSIVE frequency for the cooldown
                attackCooldownTicks = npc.getHitFrequency();
            }
        } else {
            // Out of range, navigate towards the target.
            // The 'true' parameter means it will use pathfinding to get close.
            npc.getHandle().getNavigator().setTarget(target, true);
        }
    }
}