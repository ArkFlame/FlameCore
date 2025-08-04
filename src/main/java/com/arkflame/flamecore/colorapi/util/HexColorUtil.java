package com.arkflame.flamecore.colorapi.util;

import net.md_5.bungee.api.ChatColor;
import java.awt.Color;
import java.lang.reflect.Method;

public final class HexColorUtil {
    private static Method ofMethod;
    private static boolean isModern;

    static {
        try {
            ofMethod = ChatColor.class.getMethod("of", String.class);
            isModern = true;
        } catch (NoSuchMethodException e) {
            isModern = false;
        }
    }

    public static boolean isModern() {
        return isModern;
    }

    public static ChatColor of(Color color) {
        if (!isModern) {
            // On legacy, we don't create hex ChatColor objects directly.
            // We find the closest legacy color instead.
            return LegacyColorMatcher.getClosest(color);
        }
        try {
            return (ChatColor) ofMethod.invoke(null, String.format("#%06X", (0xFFFFFF & color.getRGB())));
        } catch (Exception e) {
            return ChatColor.WHITE;
        }
    }

    public static String toLegacyFormat(Color color) {
        if (isModern) {
            return ChatColor.of(color).toString();
        } else {
            // For legacy text, just return the closest legacy color code.
            return LegacyColorMatcher.getClosest(color).toString();
        }
    }
}