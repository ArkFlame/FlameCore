package com.arkflame.flamecore.actionbarapi;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Internal helper to send action bar packets.
 * This version uses the correct serialization method to ensure color support on all versions (1.8-1.21+).
 */
class ActionBarPacketSender {

    private static boolean useModernApi;

    // NMS Reflection Caches
    private static Method getHandleMethod;
    private static Method sendPacketMethod;
    private static Field playerConnectionField;
    private static Constructor<?> packetPlayOutChatConstructor;
    private static Method chatSerializerMethodA;

    static {
        try {
            // Check for the modern Spigot API method. This is the most reliable check.
            Player.Spigot.class.getMethod("sendMessage", ChatMessageType.class, BaseComponent[].class);
            useModernApi = true;
        } catch (Throwable e) {
            // If the modern method doesn't exist, we are on a legacy version.
            useModernApi = false;
            try {
                String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                String craftbukkitPackage = "org.bukkit.craftbukkit." + nmsVersion;
                String nmsPackage = "net.minecraft.server." + nmsVersion;

                Class<?> craftPlayerClass = Class.forName(craftbukkitPackage + ".entity.CraftPlayer");
                getHandleMethod = craftPlayerClass.getMethod("getHandle");

                Class<?> entityPlayerClass = Class.forName(nmsPackage + ".EntityPlayer");
                playerConnectionField = entityPlayerClass.getField("playerConnection");

                Class<?> packetClass = Class.forName(nmsPackage + ".Packet");
                Class<?> playerConnectionClass = Class.forName(nmsPackage + ".PlayerConnection");
                sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);

                Class<?> iChatBaseComponentClass = Class.forName(nmsPackage + ".IChatBaseComponent");
                Class<?> packetPlayOutChatClass = Class.forName(nmsPackage + ".PacketPlayOutChat");

                packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class);
                
                // This is the static method IChatBaseComponent.ChatSerializer.a("...json...")
                chatSerializerMethodA = iChatBaseComponentClass.getDeclaredClasses()[0].getMethod("a", String.class);

            } catch (Exception ex) {
                System.err.println("Critical error initializing NMS for legacy ActionBarAPI. Action bars will not work on this version.");
                ex.printStackTrace();
            }
        }
    }

    static void send(Player player, BaseComponent[] message) {
        if (useModernApi) {
            // --- Modern Path (1.12+): Use the reliable Spigot API ---
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
        } else {
            // --- Legacy Path (1.8 - 1.11): Use NMS with the corrected JSON format ---
            if (packetPlayOutChatConstructor == null || chatSerializerMethodA == null) {
                return; // NMS initialization failed.
            }
            try {
                // 1. Convert our rich components to a string with legacy § codes.
                // Example: "§6§lTest"
                String legacyText = BaseComponent.toLegacyText(message);

                // 2. Escape any quotes in the text itself to prevent breaking the JSON.
                String escapedText = legacyText.replace("\"", "\\\"");

                // 3. Create the simple JSON format that legacy versions understand.
                // Example: {"text":"§6§lTest"}
                String json = "{\"text\":\"" + escapedText + "\"}";

                // 4. Use the chat serializer to create an NMS IChatBaseComponent from our simple JSON.
                Object chatComponent = chatSerializerMethodA.invoke(null, json);
                
                // 5. Create the packet with the component and the action bar position byte (2).
                Object packet = packetPlayOutChatConstructor.newInstance(chatComponent, (byte) 2);
                
                // 6. Send the packet.
                sendPacket(player, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void sendPacket(Player player, Object packet) {
        if (getHandleMethod == null) return;
        try {
            Object handle = getHandleMethod.invoke(player);
            Object playerConnection = playerConnectionField.get(handle);
            sendPacketMethod.invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}