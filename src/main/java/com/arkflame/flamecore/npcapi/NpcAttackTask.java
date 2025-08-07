package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.npcapi.util.CitizensCompat;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A self-contained task that manages an NPC actively attacking a single target.
 * This task runs every tick to provide fast and responsive combat.
 */
class NpcAttackTask extends BukkitRunnable {
    /**
     * Streamlines the set of checks to determine if an entity is a valid target.
     * This is used for filtering new targets and validating the current target.
     * An entity is valid if it's alive, not an ally, within range, and not in an
     * invalid gamemode.
     * This method is package-private so NpcAttackTask can access it.
     *
     * @param entity The entity to check.
     * @return {@code true} if the entity is a valid target, {@code false}
     *         otherwise.
     */
    static boolean isValidTarget(Npc npc, LivingEntity entity, double radiusSquared) {
        // Target must exist, be alive, and valid in the world.
        if (entity == null || entity.isDead() || !entity.isValid()) {
            return false;
        }

        // Target must not be the NPC itself.
        if (entity.getUniqueId().equals(npc.getHandle().getEntity().getUniqueId())) {
            return false;
        }

        // If the target is a player, check their gamemode.
        if (entity instanceof Player) {
            Player player = (Player) entity;
            GameMode gameMode = player.getGameMode();
            if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
                return false;
            }
        }

        // Target must not be an ally.
        if (npc.isAlly(entity)) {
            return false;
        }

        // Target must be within the defined radius.
        if (entity.getLocation().distanceSquared(npc.getLocation()) > radiusSquared) {
            return false;
        }

        return true; // All checks passed.
    }

    private final Npc npc;
    private final LivingEntity target;
    private int attackCooldownTicks;

    public NpcAttackTask(Npc npc) {
        this.npc = npc;
        // We can safely cast here because the logic in AttackNearbyTask ensures it's a
        // LivingEntity.
        this.target = (LivingEntity) npc.targetEntity;
        this.attackCooldownTicks = 0;
    }

    @Override
    public void run() {
        // Use the manager's centralized method to check for target validity.
        // If the NPC is despawned or the target is no longer valid for any reason...
        if (!npc.isSpawned() || !isValidTarget(npc, target, 50 * 50)) {
            if (npc.getBehavior() == Behavior.ATTACKING) {
                npc.stopBehavior();
            } else {
                this.cancel();
            }
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