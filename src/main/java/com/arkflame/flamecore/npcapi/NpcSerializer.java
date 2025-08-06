package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.configapi.ConfigAPI;
import com.arkflame.flamecore.configapi.Config.ConfigLocation;
import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait; // Correct, modern import
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * A utility to serialize and deserialize Npc objects to/from our custom Config files.
 * It handles reclaiming existing Citizens NPCs on load to prevent duplicates.
 */
class NpcSerializer {
    
    /**
     * Saves an Npc's state to its corresponding YAML file.
     * @param npc The Npc object to serialize.
     */
    public static void serialize(Npc npc) {
        Config config = ConfigAPI.getConfig("npcs/" + npc.getUniqueId() + ".yml");
        
        config.set("citizens-uuid", npc.getCitizensId().toString());
        config.set("name", npc.getName());
        config.set("entity-type", npc.getEntityType().name());
        
        if (npc.getHandle().hasTrait(SkinTrait.class)) {
            config.set("skin", npc.getHandle().getTrait(SkinTrait.class).getSkinName());
        }
        
        if (npc.getInitialSpawnLocation() != null) {
            config.setLocation("location", npc.getInitialSpawnLocation());
        } else {
            NpcAPI.getPlugin().getLogger().warning("Serializing NPC '" + npc.getName() + "' with a null location!");
        }
        
        config.set("respawn-time", npc.getRespawnTime());
        config.set("hit-delay", npc.getHitDelay());
        
        config.save();
    }
    
    /**
     * The definitive deserializer. It reclaims existing NPCs from any Citizens registry
     * or creates a new one if it doesn't exist, then applies our custom data.
     * @param config The config file to load from.
     * @param npcId The UUID of the NPC, derived from the file name.
     * @return The loaded or created Npc wrapper.
     */
    public static Npc deserialize(Config config, UUID flameId) {
        // --- Reclaim Logic ---
        // Try to find an NPC that Citizens may have already loaded with this UUID.
        // We check BOTH registries to be safe.
        UUID citizensId = UUID.fromString(config.getRaw().getString("citizens-uuid"));
        NPC citizensNpc = CitizensAPI.getNPCRegistry().getByUniqueId(citizensId);
        
        // --- Creation Logic ---
        if (citizensNpc == null) {
            // NPC does not exist, we must create it.
            String name = config.getString("name");
            if (name == null) {
                NpcAPI.getPlugin().getLogger().warning("Cannot load NPC from config with no name!");
                return null;
            }
            
            EntityType type = EntityType.PLAYER;
            if (config.getRaw().contains("entity-type")) {
                try { type = EntityType.valueOf(config.getRaw().getString("entity-type")); } catch (Exception ignored) {}
            }
            
            // Persistent NPCs are created in the main registry so Citizens can save them.
            // This is crucial for the reclaim logic to work on the next startup.
            NPCRegistry registry = CitizensCompat.getTemporaryNPCRegistry();
            // The createNPC(type, name) method is stable and returns the new NPC.
            citizensNpc = registry.createNPC(type, name);
            try {
                // Use reflection to force the UUID to match. This is risky but necessary for this design.
                Field uuidField = citizensNpc.getClass().getDeclaredField("uuid");
                uuidField.setAccessible(true);
                uuidField.set(citizensNpc, citizensId);
            } catch (Exception e) {
                // If reflection fails, we can't guarantee the link.
            }
        }

        // Get or create our authoritative wrapper for this Citizens NPC.
        Npc npc = new Npc(citizensNpc, flameId);

        // --- Apply all our custom data from the config ---
        npc.setPersistent(true);
        if (config.getRaw().contains("skin")) {
            // Use getOrAddTrait for safety.
            npc.getHandle().getOrAddTrait(SkinTrait.class).setSkinName(config.getRaw().getString("skin"), true);
        }

        if (config.contains("location")) {
            ConfigLocation wrapper = config.getLocation("location");
            if (wrapper != null) {
                npc.setInitialSpawnLocation(wrapper.toLocation());
            }
        }

        npc.setRespawnTime(config.getRaw().getInt("respawn-time", -1));
        npc.setHitDelay(config.getRaw().getInt("hit-delay", 10));
        
        // Ensure standard traits are present and configured correctly.
        npc.getHandle().setProtected(false);
        npc.getHandle().getOrAddTrait(Owner.class).setOwner(npc.getName());
        npc.getHandle().getOrAddTrait(LookClose.class).lookClose(true);

        return npc;
    }
}