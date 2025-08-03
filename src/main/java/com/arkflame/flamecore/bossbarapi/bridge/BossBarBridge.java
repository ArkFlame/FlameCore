package com.arkflame.flamecore.bossbarapi.bridge;

import org.bukkit.entity.Player;

import com.arkflame.flamecore.bossbarapi.enums.BarColor;
import com.arkflame.flamecore.bossbarapi.enums.BarStyle;

public interface BossBarBridge {
    void setText(String text);
    void setProgress(double progress);
    void setColor(BarColor color);
    void setStyle(BarStyle style);
    void setVisible(boolean visible);
    void addPlayer(Player player);
    void removePlayer(Player player);
    void destroy();
}