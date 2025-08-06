package com.arkflame.flamecore.npcapi;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Comparator;

/**
 * A self-contained "controller" task that puts an NPC in guard mode.
 * It periodically scans for nearby players and delegates the actual combat
 * to the NPC's .attack() method.
 */
class NpcGuardTask extends BukkitRunnable {
    private final Npc npc;
    private final double radius;
    private final double radiusSquared;
    private Entity currentTarget;
    
    public NpcGuardTask(Npc npc, double radius) {
        this.npc = npc;
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    @Override
    public void run() {
        if (!npc.isSpawned()) {
            npc.stopBehavior(); // This also cancels this task.
            return;
        }

        // Check if the current target is still valid and within range.
        if (currentTarget != null && !currentTarget.isDead() && currentTarget.isValid() &&
            currentTarget.getLocation().distanceSquared(npc.getLocation()) <= radiusSquared) {
            // The current target is still valid, so do nothing and let the attack task continue.
            return;
        }
        
        // Find a new target.
        currentTarget = findNearestPlayer(radius);
        
        if (currentTarget != null) {
            // Found a new valid target.
            // Delegate the actual combat to the NPC's attack method.
            // This will cancel this guard task and start a new NpcAttackTask.
            npc.attack(currentTarget);
        }
    }

    private Player findNearestPlayer(double radius) {
        if (!npc.isSpawned()) return null;
        
        return npc.getLocation().getWorld().getPlayers().stream()
            .filter(player -> !player.getUniqueId().equals(npc.getUniqueId()))
            .filter(player -> player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR)
            .filter(player -> npc.getLocation().distanceSquared(player.getLocation()) <= radiusSquared)
            .min(Comparator.comparingDouble(player -> npc.getLocation().distanceSquared(player.getLocation())))
            .orElse(null);
    }
}