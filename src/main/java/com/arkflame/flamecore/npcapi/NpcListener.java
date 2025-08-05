package com.arkflame.flamecore.npcapi;

import net.citizensnpcs.api.event.NPCRemoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NpcListener implements Listener {
    
    @EventHandler
    public void onNpcRemove(NPCRemoveEvent event) {
        // When any Citizens NPC is destroyed, we unregister it from our API's map.
        // This keeps our managed list perfectly in sync with Citizens.
        NpcAPI.unregisterNpc(event.getNPC().getUniqueId());
    }
}