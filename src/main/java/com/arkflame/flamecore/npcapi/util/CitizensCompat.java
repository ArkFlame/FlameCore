package com.arkflame.flamecore.npcapi.util;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause; // Correct import
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A definitive compatibility wrapper for Citizens API versions (2.0.28 to 2.0.39+).
 * This class uses reflection to safely bridge API differences and provides safe methods.
 */
public final class CitizensCompat {
    // --- Feature Flags ---
    public static final boolean SUPPORTS_ANIMATION_TRAIT;

    // --- Cached Classes ---
    private static Class<? extends Trait> skinTraitClass;
    private static Class<? extends Trait> animationTraitClass;
    private static Class<?> equipmentClass;
    private static Class<?> equipmentSlotClass; // Modern org.bukkit.inventory.EquipmentSlot enum

    // --- Cached Methods ---
    private static Method setSkinNameMethod;
    private static Method playAnimationMethod;
    private static Method setEquipmentMethod; // For modern enum-based setting
    private static Method setEquipmentLegacyMethod; // For legacy int-based setting
    private static Method swingArmNmsMethod;
    private static Method getHandleMethod;
    private static Method sendPacketMethod;
    private static Field playerConnectionField;
    private static Constructor<?> animationPacketConstructor;

    static {
        // --- Detect SkinTrait Location ---
        try {
            skinTraitClass = (Class<? extends Trait>) Class.forName("net.citizensnpcs.trait.SkinTrait");
            setSkinNameMethod = skinTraitClass.getMethod("setSkinName", String.class, boolean.class);
        } catch (Exception e) {
            try {
                skinTraitClass = (Class<? extends Trait>) Class.forName("net.citizensnpcs.api.trait.trait.SkinTrait");
                setSkinNameMethod = skinTraitClass.getMethod("setSkinName", String.class, boolean.class);
            } catch (Exception ex) {
                System.err.println("Could not find any SkinTrait class for Citizens. Skin functionality will be disabled.");
            }
        }

        // --- Detect AnimationTrait ---
        boolean animationSupport = false;
        try {
            animationTraitClass = (Class<? extends Trait>) Class.forName("net.citizensnpcs.trait.AnimationTrait");
            Class<?> animationEnum = Class.forName("net.citizensnpcs.trait.AnimationTrait$Animation");
            playAnimationMethod = animationTraitClass.getMethod("play", animationEnum);
            animationSupport = true;
        } catch (Exception e) { /* AnimationTrait does not exist on this version. */ }
        SUPPORTS_ANIMATION_TRAIT = animationSupport;
        
        // --- THE DEFINITIVE FIX for Equipment ---
        try {
            equipmentClass = (Class<? extends Trait>) Class.forName("net.citizensnpcs.api.trait.trait.Equipment");

            // Try to load the modern (enum-based) method first.
            try {
                equipmentSlotClass = Class.forName("org.bukkit.inventory.EquipmentSlot");
                setEquipmentMethod = equipmentClass.getMethod("set", equipmentSlotClass, ItemStack.class);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // If the modern enum or method isn't found, we're on a legacy version.
                // Fall back to the legacy (int-based) method.
                setEquipmentLegacyMethod = equipmentClass.getMethod("set", int.class, ItemStack.class);
            }
        } catch (Exception e) {
            System.err.println("Could not initialize Equipment trait methods for Citizens.");
            e.printStackTrace();
        }

        // --- Initialize NMS Fallback for Arm Swing (for 1.8) ---
        if (!SUPPORTS_ANIMATION_TRAIT) {
            try {
                String nmsVersion = CitizensAPI.getPlugin().getServer().getClass().getPackage().getName().split("\\.")[3];
                String craftbukkitPackage = "org.bukkit.craftbukkit." + nmsVersion;
                String nmsPackage = "net.minecraft.server." + nmsVersion;

                Class<?> craftPlayerClass = Class.forName(craftbukkitPackage + ".entity.CraftPlayer");
                getHandleMethod = craftPlayerClass.getMethod("getHandle");
                Class<?> entityPlayerClass = Class.forName(nmsPackage + ".EntityPlayer");
                playerConnectionField = entityPlayerClass.getField("playerConnection");
                Class<?> packetClass = Class.forName(nmsPackage + ".Packet");
                Class<?> playerConnectionClass = Class.forName(nmsPackage + ".PlayerConnection");
                sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
                Class<?> packetAnimationClass = Class.forName(nmsPackage + ".PacketPlayOutAnimation");
                Class<?> entityClass = Class.forName(nmsPackage + ".Entity");
                animationPacketConstructor = packetAnimationClass.getConstructor(entityClass, int.class);
            } catch (Exception e) {
                System.err.println("Could not initialize NMS for legacy arm swing. Swing animations may not work on this version.");
            }
        }
    }
    
    public static void setSkin(NPC npc, String skinName) {
        if (skinTraitClass == null || setSkinNameMethod == null) return;
        try {
            Trait skinTrait = npc.getOrAddTrait(skinTraitClass);
            setSkinNameMethod.invoke(skinTrait, skinName, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Correctly sets equipment for an NPC, supporting both modern and legacy Citizens APIs.
     * For now, this helper only sets the main hand.
     * @param npc The NPC to modify.
     * @param item The item to place in the NPC's hand.
     */
    public static void setEquipment(NPC npc, ItemStack item) {
        if (equipmentClass == null) return;
        try {
            Trait equipmentTrait = npc.getOrAddTrait((Class<? extends Trait>) equipmentClass);

            if (setEquipmentMethod != null && equipmentSlotClass != null) {
                // Modern enum-based slot system (1.13+)
                Object handEnum = equipmentSlotClass.getEnumConstants()[4]; // EquipmentSlot.HAND (usually 4th or 5th)
                // A safer way to find it:
                for(Object enumConstant : equipmentSlotClass.getEnumConstants()) {
                    if(enumConstant.toString().equals("HAND")) {
                        handEnum = enumConstant;
                        break;
                    }
                }
                setEquipmentMethod.invoke(equipmentTrait, handEnum, item);
            } else if (setEquipmentLegacyMethod != null) {
                // Legacy integer-based slot system (1.8–1.12)
                setEquipmentLegacyMethod.invoke(equipmentTrait, 0, item); // 0 = hand
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playSwingAnimation(NPC npc) {
        if (npc.getEntity() == null) return;
        if (SUPPORTS_ANIMATION_TRAIT) {
            try {
                Trait animationTrait = npc.getOrAddTrait(animationTraitClass);
                Object swingEnum = animationTraitClass.getDeclaredClasses()[0].getEnumConstants()[0]; // SWING_ARM
                playAnimationMethod.invoke(animationTrait, swingEnum);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Legacy NMS Fallback
            if (animationPacketConstructor == null) return;
            try {
                Object handle = getHandleMethod.invoke(npc.getEntity());
                Object packet = animationPacketConstructor.newInstance(handle, 0); // 0 is the swing animation
                
                for (Player player : npc.getStoredLocation().getWorld().getPlayers()) {
                    if(player.getLocation().distanceSquared(npc.getStoredLocation()) < 2500) { // 50*50 blocks
                        Object playerHandle = getHandleMethod.invoke(player);
                        Object playerConnection = playerConnectionField.get(playerHandle);
                        sendPacketMethod.invoke(playerConnection, packet);
                    }
                }
            } catch (Exception e) {
                // silent fail
            }
        }
    }
    
    public static void faceLocation(NPC npc, Location location) {
        npc.faceLocation(location);
    }

    /**
     * Teleports an NPC using the correct, modern Bukkit TeleportCause.
     * @param npc The NPC to teleport.
     * @param location The target location.
     */
    public static void teleport(NPC npc, Location location) {
        npc.teleport(location, TeleportCause.PLUGIN);
    }
}