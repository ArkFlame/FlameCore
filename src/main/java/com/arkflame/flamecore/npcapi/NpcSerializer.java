package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.configapi.ConfigAPI;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/**
 * A utility to serialize and deserialize Npc objects to/from our custom Config files.
 */
class NpcSerializer {

    /**
     * Saves an Npc's state to its corresponding YAML file.
     */
    public static void serialize(Npc npc) {
        // Get a Config object for this NPC's unique file.
        Config config = ConfigAPI.getConfig("npcs/" + npc.getUniqueId() + ".yml");
        
        config.set("name", npc.getName());
        config.set("entity-type", npc.getCitizensNpc().getEntity().getType().name());
        
        // SkinTrait doesn't exist on 1.8, so we need a safe way to get the skin name.
        // For simplicity, we'll assume it's the same as the name if not explicitly set.
        // A more complex implementation could reflect to get the SkinTrait's name.
        config.set("skin", npc.getName()); 
        
        config.set("location", npc.getInitialSpawnLocation());
        config.set("respawn-time", npc.getRespawnTime());
        
        config.save();
    }
    
    /**
     * Reads a Config object and creates an Npc.Builder from its data.
     */
    public static Npc.Builder deserialize(Config config) {
        String name = config.getString("name");
        
        Npc.Builder builder = Npc.builder(name)
                .persistent(true); // If we're loading from a file, it must be persistent.
        
        if (config.contains("entity-type")) {
            builder.type(EntityType.valueOf(config.getString("entity-type")));
        }
        if (config.contains("skin")) {
            builder.skin(config.getString("skin"));
        }
        if (config.contains("location")) {
            builder.location(config.getLocation("location"));
        }
        if (config.contains("respawn-time")) {
            builder.respawnTime(config.getInt("respawn-time"));
        }
        
        return builder;
    }
}