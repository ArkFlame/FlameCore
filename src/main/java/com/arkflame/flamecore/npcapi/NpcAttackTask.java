package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A self-contained task that manages the behavior of an NPC actively attacking a single target.
 */
class NpcAttackTask extends BukkitRunnable {
    private final Npc npc;
    private final Entity target;
    private long lastAttackTime = 0;
    private final long attackInterval = 500; // 10 ticks

    public NpcAttackTask(Npc npc) {
        this.npc = npc;
        this.target = npc.targetEntity;
    }
    
    @Override
    public void run() {
        // Stop the task if the NPC or target is no longer valid.
        if (!npc.isSpawned() || target == null || target.isDead() || !target.isValid()) {
            npc.stopBehavior(); // This also cancels the task.
            return;
        }
        
        // Continuously navigate towards the target.
        npc.getHandle().getNavigator().setTarget(target, false);
        
        // Check if the NPC is within attack range (3 blocks).
        if (npc.getLocation().distanceSquared(target.getLocation()) < 9) {
            // Stop moving to engage in combat.
            npc.getHandle().getNavigator().cancelNavigation();

            // Check if the attack cooldown has passed.
            if (System.currentTimeMillis() - lastAttackTime > attackInterval) {
                // Face the target, swing, and deal damage.
                CitizensCompat.faceLocation(npc.getHandle(), target.getLocation());
                CitizensCompat.playSwingAnimation(npc.getHandle());
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).damage(1.0, npc.getHandle().getEntity());
                }
                lastAttackTime = System.currentTimeMillis();
            }
        }
    }
}