package com.arkflame.core.bossbarapi.bridge;

import com.arkflame.core.bossbarapi.enums.BarColor;
import com.arkflame.core.bossbarapi.enums.BarStyle;
import org.bukkit.entity.Player;

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