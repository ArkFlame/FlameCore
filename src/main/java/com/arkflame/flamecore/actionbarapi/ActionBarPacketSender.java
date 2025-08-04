package com.arkflame.flamecore.actionbarapi;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Internal helper to send action bar packets.
 * This class handles all NMS/Reflection logic to maintain compatibility.
 * It intelligently detects the server version to use the best available method.
 */
class ActionBarPacketSender {

    // --- Caches ---
    private static boolean useModernApi;

    // Reflection caches for NMS (Legacy)
    private static Method getHandleMethod;
    private static Method sendPacketMethod;
    private static Field playerConnectionField;
    private static Constructor<?> packetPlayOutChatConstructor;
    private static Method chatSerializerMethod;

    static {
        // This static block runs once to determine the best method to send action bars.
        try {
            // Attempt to find the modern Spigot API method.
            // This is the most reliable check for 1.10+ servers.
            Player.Spigot.class.getMethod("sendMessage", ChatMessageType.class, BaseComponent[].class);
            useModernApi = true;
        } catch (Throwable e) {
            // A NoClassDefFoundError or NoSuchMethodError means we're on an older version.
            useModernApi = false;
            // Now, we MUST initialize the NMS reflection as a fallback.
            try {
                String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
                getHandleMethod = craftPlayerClass.getMethod("getHandle");

                Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + nmsVersion + ".EntityPlayer");
                playerConnectionField = entityPlayerClass.getField("playerConnection");

                Class<?> packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".Packet");
                Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + nmsVersion + ".PlayerConnection");
                sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);

                Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
                Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");

                // This is the key constructor for 1.8 action bars.
                packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class);
                
                chatSerializerMethod = iChatBaseComponentClass.getDeclaredClasses()[0].getMethod("a", String.class);
                
            } catch (Exception ex) {
                System.err.println("Could not initialize NMS reflection for ActionBarAPI. Action bars may not work on this version.");
                ex.printStackTrace();
            }
        }
    }

    static void send(Player player, BaseComponent[] message) {
        if (useModernApi) {
            // Use the modern, safe, and efficient Spigot API.
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
        } else {
            // Fallback to NMS reflection for older versions (1.8 - 1.9).
            if (packetPlayOutChatConstructor == null) {
                // The reflection setup failed, so we can't send.
                return;
            }
            try {
                String jsonMessage = ComponentSerializer.toString(message);
                Object chatComponent = chatSerializerMethod.invoke(null, jsonMessage);
                
                // The '2' here tells the client this is an action bar message.
                Object packet = packetPlayOutChatConstructor.newInstance(chatComponent, (byte) 2);
                
                sendPacket(player, packet);
            } catch (Exception e) {
                // An error occurred during NMS sending.
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