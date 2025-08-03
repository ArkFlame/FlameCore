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
 * It intelligently uses modern Spigot APIs first if available.
 */
class ActionBarPacketSender {
    // Caches for reflection
    private static Method getHandleMethod;
    private static Method sendPacketMethod;
    private static Field playerConnectionField;
    private static Constructor<?> packetPlayOutChatConstructor;
    private static Method chatSerializerMethod;

    private static boolean useSpigotAPI = true;

    static {
        // We assume the Spigot API exists. If it throws an error, we switch to reflection.
        try {
            ChatMessageType.ACTION_BAR.getClass(); // This will throw NoClassDefFoundError on old versions
        } catch (Throwable e) {
            useSpigotAPI = false;
            // Spigot API not found, fall back to NMS reflection.
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
                
                // Constructor for 1.8.8+
                try {
                    packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class);
                } catch (NoSuchMethodException ex) {
                    // Constructor for 1.16+
                    packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, ChatMessageType.class);
                }
                
                chatSerializerMethod = iChatBaseComponentClass.getDeclaredClasses()[0].getMethod("a", String.class);
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    static void send(Player player, BaseComponent[] message) {
        if (useSpigotAPI) {
            // Use the modern, safe, and efficient Spigot API.
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
            return;
        }

        // Fallback to NMS reflection for older versions (pre 1.10)
        try {
            Object chatComponent = chatSerializerMethod.invoke(null, ComponentSerializer.toString(message));
            
            // The constructor signature changed over time, we handle both common cases.
            Object packet;
            if (packetPlayOutChatConstructor.getParameterCount() == 2 && packetPlayOutChatConstructor.getParameterTypes()[1] == byte.class) {
                packet = packetPlayOutChatConstructor.newInstance(chatComponent, (byte) 2);
            } else {
                // This path is for newer reflection logic if needed, but spigot API should cover it.
                // For safety, we just don't send if the legacy constructor isn't found.
                return;
            }
            
            sendPacket(player, packet);
        } catch (Exception e) {
            // Reflection failed.
        }
    }
    
    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = getHandleMethod.invoke(player);
        Object playerConnection = playerConnectionField.get(handle);
        sendPacketMethod.invoke(playerConnection, packet);
    }
}