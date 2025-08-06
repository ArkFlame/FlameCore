package com.arkflame.flamecore.npcapi;

import com.arkflame.flamecore.configapi.Config;
import com.arkflame.flamecore.configapi.Config.ConfigLocation;
import com.arkflame.flamecore.configapi.ConfigAPI;
import com.arkflame.flamecore.npcapi.util.CitizensCompat;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.entity.EntityType;

import java.util.UUID;

/**
 * The definitive serializer for Npc objects. It handles saving our custom data
 * and creating fresh Citizens NPCs from that data on load.
 */
class NpcSerializer {
    
    public static void serialize(Npc npc) {
        // The file is named after our FlameCore UUID.
        Config config = ConfigAPI.getConfig("npcs/" + npc.getUniqueId() + ".yml");
        
        // We no longer need to save the Citizens UUID. It's truly temporary.
        config.set("name", npc.getName());
        config.set("entity-type", npc.getEntityType().name());
        
        if (npc.getHandle().hasTrait(SkinTrait.class)) {
            config.set("skin", npc.getHandle().getTrait(SkinTrait.class).getSkinName());
        }
        
        if (npc.getInitialSpawnLocation() != null) {
            config.setLocation("location", npc.getInitialSpawnLocation());
        }
        
        config.set("respawn-time", npc.getRespawnTime());
        config.set("hit-delay", npc.getHitDelay());
        
        config.save();
    }
    
    public static Npc deserialize(Config config, UUID flameId) {
        System.out.println("[NPC DEBUG] Attempting to deserialize NPC with FlameID: " + flameId);
        
        String name = config.getString("name");
        if (name == null) {
            System.out.println("[NPC DEBUG] Deserialization FAILED: 'name' is missing in " + flameId + ".yml");
            return null;
        }
            
        EntityType type = EntityType.PLAYER;
        if (config.getRaw().contains("entity-type")) {
            try { type = EntityType.valueOf(config.getRaw().getString("entity-type")); } catch (Exception ignored) {}
        }

        NPCRegistry registry = CitizensCompat.getTemporaryNPCRegistry();
        NPC citizensNpc = registry.createNPC(type, name);

        // --- THE DEFINITIVE FIX ---
        // 1. Create the wrapper.
        Npc npc = new Npc(citizensNpc, flameId);
        
        // 2. IMMEDIATELY register it with the API's central map.
        // This was the missing, critical step.
        NpcAPI.registerNpc(npc);
        System.out.println("[NPC DEBUG] NPC Wrapper CREATED and REGISTERED in NpcAPI map. Name: " + name + ", FlameID: " + flameId);
        
        // --- Apply all our custom data ---
        npc.setPersistent(true);
        if (config.getRaw().contains("skin")) {
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
        
        npc.getHandle().setProtected(false);
        npc.getHandle().getOrAddTrait(Owner.class).setOwner(npc.getName());
        npc.getHandle().getOrAddTrait(LookClose.class).lookClose(true);

        return npc;
    }
}