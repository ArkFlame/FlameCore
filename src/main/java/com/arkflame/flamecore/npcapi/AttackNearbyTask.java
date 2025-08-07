package com.arkflame.flamecore.npcapi;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Comparator;

/**
 * A controller task that scans for targets and delegates the actual combat
 * to a sub-task (NpcAttackTask). This avoids code duplication and separates concerns.
 */
class AttackNearbyTask extends BukkitRunnable {
    private final Npc npc;
    private final double radiusSquared;
    private NpcAttackTask currentAttackSubTask;

    public AttackNearbyTask(Npc npc, double radius) {
        this.npc = npc;
        this.radiusSquared = radius * radius;
    }

    @Override
    public void run() {
        if (!npc.isSpawned() || npc.behavior != Behavior.ATTACKING_NEARBY) {
            this.cancel(); // Stop if the NPC is despawned or behavior changed.
            return;
        }

        // Check if the sub-task is active and its target is still valid.
        if (isSubTaskValid()) {
            return; // The NpcAttackTask is handling combat, so we do nothing.
        }

        // If the sub-task is not valid, the NPC is idle. Find a new target.
        LivingEntity newTarget = findNearestTarget();

        if (newTarget != null) {
            // A new valid target was found. Start a new attack sub-task.
            // We do this manually to avoid the stopBehavior() call in npc.attack().
            npc.targetEntity = newTarget;
            this.currentAttackSubTask = new NpcAttackTask(npc);
            this.currentAttackSubTask.runTaskTimer(NpcAPI.getPlugin(), 0L, 1L);
        }
    }

    /**
     * Checks if the current attack sub-task exists and is still valid.
     * A task is invalid if it's null, cancelled, or its target is no longer valid.
     */
    private boolean isSubTaskValid() {
        if (currentAttackSubTask == null) {
            return false;
        }
        
        LivingEntity target = (LivingEntity) npc.targetEntity;
        
        return target != null && !target.isDead() && target.isValid() &&
               target.getLocation().distanceSquared(npc.getLocation()) <= radiusSquared &&
               !npc.isAlly(target);
    }

    private LivingEntity findNearestTarget() {
        if (!npc.isSpawned()) return null;
        
        // This stream logic is for Java 8 compatibility.
        // It can be replaced with a simple for-loop for even broader compatibility if needed.
        return npc.getLocation().getWorld().getEntitiesByClass(LivingEntity.class).stream()
                .filter(entity -> !entity.getUniqueId().equals(npc.getHandle().getEntity().getUniqueId()))
                .filter(entity -> !npc.isAlly(entity))
                .filter(entity -> !entity.isDead() && entity.isValid())
                .filter(entity -> !(entity instanceof Player) || (((Player) entity).getGameMode() != GameMode.CREATIVE && ((Player) entity).getGameMode() != GameMode.SPECTATOR))
                .filter(entity -> npc.getLocation().distanceSquared(entity.getLocation()) <= radiusSquared)
                .min(Comparator.comparingDouble(entity -> npc.getLocation().distanceSquared(entity.getLocation())))
                .orElse(null);
    }
    
    /**
     * Overridden to ensure the sub-task is also cancelled, preventing it from running loose.
     */
    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        if (currentAttackSubTask != null) {
            currentAttackSubTask.cancel();
            currentAttackSubTask = null;
        }
    }
}