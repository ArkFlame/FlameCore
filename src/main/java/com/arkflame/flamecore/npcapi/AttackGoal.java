package com.arkflame.flamecore.npcapi;

import net.citizensnpcs.api.ai.Goal;
import net.citizensnpcs.api.ai.GoalSelector;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * A custom Citizens AI Goal to make an NPC pathfind to, look at, and attack a target.
 */
class AttackGoal implements Goal {
    private final NPC npc;
    private Entity target;
    private long lastAttackTime = 0;
    private final long attackInterval = 1000; // 1 second between attacks

    public AttackGoal(NPC npc) {
        this.npc = npc;
    }

    public void setTarget(Entity target) {
        this.target = target;
    }

    @Override
    public void reset() {
        npc.getNavigator().cancelNavigation();
    }

    @Override
    public void run(GoalSelector goalSelector) {
        if (target == null || target.isDead() || !target.isValid()) {
            goalSelector.finish();
            return;
        }
        
        double distance = npc.getStoredLocation().distanceSquared(target.getLocation());
        
        // If too far away, navigate closer
        if (distance > 4) { // 2 blocks, squared
            npc.getNavigator().setTarget(target, false);
        } else {
            // If close enough, stop moving and attack
            npc.getNavigator().cancelNavigation();
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime > attackInterval) {
                if (npc.getEntity() instanceof LivingEntity) {
                    ((LivingEntity) npc.getEntity()).attack(target);
                }
                lastAttackTime = currentTime;
            }
        }
    }

    @Override
    public boolean shouldExecute(GoalSelector goalSelector) {
        return target != null && !target.isDead() && target.isValid();
    }
}