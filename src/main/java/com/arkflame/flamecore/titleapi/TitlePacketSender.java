package com.arkflame.flamecore.titleapi;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Internal helper to send title packets.
 * This class handles all NMS/Reflection logic to maintain compatibility.
 * It intelligently uses modern Spigot APIs first if available.
 */
class TitlePacketSender {
    // Caches for reflection
    private static Method getHandleMethod;
    private static Method sendPacketMethod;
    private static Field playerConnectionField;
    private static Constructor<?> packetPlayOutTitleConstructor;
    private static Object enumTitleAction;
    private static Object enumSubtitleAction;
    private static Object enumTimesAction;
    private static Method chatSerializerMethod;

    // Cache for modern API (1.11+)
    private static Method modernSendTitleMethod;

    static {
        try {
            // First, try to get the modern, non-reflection method.
            modernSendTitleMethod = Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
        } catch (NoSuchMethodException e) {
            // Modern method not found, fall back to NMS reflection.
            try {
                String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
                getHandleMethod = craftPlayerClass.getMethod("getHandle");

                Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + nmsVersion + ".EntityPlayer");
                playerConnectionField = entityPlayerClass.getField("playerConnection");

                Class<?> packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".Packet");
                Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + nmsVersion + ".PlayerConnection");
                sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
                
                Class<?> packetPlayOutTitleClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutTitle");
                Class<?> enumTitleActionClass = packetPlayOutTitleClass.getDeclaredClasses()[0]; // EnumTitleAction
                Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
                
                packetPlayOutTitleConstructor = packetPlayOutTitleClass.getConstructor(enumTitleActionClass, iChatBaseComponentClass, int.class, int.class, int.class);
                
                enumTitleAction = enumTitleActionClass.getField("TITLE").get(null);
                enumSubtitleAction = enumTitleActionClass.getField("SUBTITLE").get(null);
                enumTimesAction = enumTitleActionClass.getField("TIMES").get(null);

                chatSerializerMethod = iChatBaseComponentClass.getDeclaredClasses()[0].getMethod("a", String.class); // ChatSerializer.a(json)

            } catch (Exception ex) {
                // Could not initialize reflection, API will be disabled for this version.
                ex.printStackTrace();
            }
        }
    }

    static void send(Player player, BaseComponent[] title, BaseComponent[] subtitle, int fadeIn, int stay, int fadeOut) {
        if (modernSendTitleMethod != null) {
            // Use the modern, safe, and efficient Spigot API.
            try {
                String legacyTitle = BaseComponent.toLegacyText(title);
                String legacySubtitle = BaseComponent.toLegacyText(subtitle);
                modernSendTitleMethod.invoke(player, legacyTitle, legacySubtitle, fadeIn, stay, fadeOut);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Fallback to NMS reflection for older versions (pre 1.11)
        try {
            Object timesPacket = packetPlayOutTitleConstructor.newInstance(enumTimesAction, null, fadeIn, stay, fadeOut);
            sendPacket(player, timesPacket);

            Object titleComponent = chatSerializerMethod.invoke(null, ComponentSerializer.toString(title));
            Object titlePacket = packetPlayOutTitleConstructor.newInstance(enumTitleAction, titleComponent, fadeIn, stay, fadeOut);
            sendPacket(player, titlePacket);
            
            Object subtitleComponent = chatSerializerMethod.invoke(null, ComponentSerializer.toString(subtitle));
            Object subtitlePacket = packetPlayOutTitleConstructor.newInstance(enumSubtitleAction, subtitleComponent, fadeIn, stay, fadeOut);
            sendPacket(player, subtitlePacket);
        } catch (Exception e) {
            // Reflection failed for this player/packet.
        }
    }

    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = getHandleMethod.invoke(player);
        Object playerConnection = playerConnectionField.get(handle);
        sendPacketMethod.invoke(playerConnection, packet);
    }
}