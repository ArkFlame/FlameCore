package com.arkflame.core.colorapi;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

class InternalTextComponent {
    private final String text;
    private final ChatColor color;
    private final boolean bold, italic, underlined, strikethrough, magic;

    private HoverEvent hoverEvent;
    private ClickEvent clickEvent;
    
    InternalTextComponent(String text, ChatColor color, boolean bold, boolean italic, boolean underlined, boolean strikethrough, boolean magic) {
        this.text = text;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.magic = magic;
    }

    String getText() { return text; }
    HoverEvent getHoverEvent() { return hoverEvent; }
    ClickEvent getClickEvent() { return clickEvent; }
    
    void setHoverEvent(HoverEvent hoverEvent) { this.hoverEvent = hoverEvent; }
    void setClickEvent(ClickEvent clickEvent) { this.clickEvent = clickEvent; }

    BaseComponent toBungee() {
        TextComponent component = new TextComponent(text);
        if (color != null) {
            component.setColor(color);
        }
        component.setBold(bold);
        component.setItalic(italic);
        component.setUnderlined(underlined);
        component.setStrikethrough(strikethrough);
        component.setObfuscated(magic);
        
        component.setHoverEvent(hoverEvent);
        component.setClickEvent(clickEvent);
        return component;
    }

    String toLegacyText() {
        StringBuilder sb = new StringBuilder();
        if (color != null) {
            sb.append(color);
        }
        if (bold) sb.append(ChatColor.BOLD);
        if (italic) sb.append(ChatColor.ITALIC);
        if (underlined) sb.append(ChatColor.UNDERLINE);
        if (strikethrough) sb.append(ChatColor.STRIKETHROUGH);
        if (magic) sb.append(ChatColor.MAGIC);
        
        sb.append(text);
        return sb.toString();
    }
}