package com.arkflame.flamecore.npcapi;

/**
 * Represents the current high-level behavior of a custom NPC.
 * This state is now persistent and saved to the NPC's config file.
 */
public enum Behavior {
    /**
     * The NPC is idle and has no active target or movement goal.
     */
    IDLE,
    /**
     * The NPC is moving to a fixed location.
     */
    MOVING,
    /**
     * The NPC is following a specific entity.
     */
    FOLLOWING,
    /**
     * The NPC is actively attacking a specific entity.
     */
    ATTACKING,
    /**
     * The NPC is actively attacking the nearest player within a radius.
     */
    ATTACKING_NEARBY,
    /**
     * The NPC is in the process of breaking a block.
     */
    BREAKING_BLOCK
}