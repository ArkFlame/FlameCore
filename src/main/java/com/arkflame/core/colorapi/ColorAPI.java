package com.arkflame.core.colorapi;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A self-contained, fluent API for creating and sending rich text messages.
 * This API supports legacy codes (&c), hex codes (&#RRGGBB), and gradients (<#start>text</#end>).
 * It produces BungeeCord/Spigot chat components without external dependencies like Adventure.
 *
 * Example:
 * <pre>{@code
 * ColorAPI.colorize("<#55C1FF>Welcome to <#FFAA00>My Server&r&l! &#FFA500Check out our store.")
 *         .onHover("&eClick to visit our website!")
 *         .onClick(ClickAction.OPEN_URL, "https://store.example.com")
 *         .send(player);
 * }</pre>
 */
public class ColorAPI {
    // Combined pattern for all special formats we handle.
    // Group 1: Gradient Start | Group 2: Gradient End | Group 3: Single Hex | Group 4: Legacy
    private static final Pattern FORMATTING_PATTERN = Pattern.compile(
            "<#([A-Fa-f0-9]{6})>|" +   // Gradient Start (Group 1)
            "</#([A-Fa-f0-9]{6})>|" +  // Gradient End (Group 2)
            "&#([A-Fa-f0-9]{6})|" +    // Single Hex (Group 3)
            "&[0-9A-FK-ORa-fk-or]"      // Legacy Code (Group 4)
    );

    private final List<InternalTextComponent> components = new ArrayList<>();

    private ColorAPI() {}

    public static ColorAPI create() {
        return new ColorAPI();
    }

    public static ColorAPI colorize(String text) {
        return create().append(text);
    }

    public ColorAPI append(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        this.components.addAll(parse(text));
        return this;
    }

    public ColorAPI append(ColorAPI other) {
        this.components.addAll(other.components);
        return this;
    }

    public ColorAPI onHover(HoverAction action, ColorAPI content) {
        if (!components.isEmpty()) {
            // Apply to all components created in the last append call
            getLastAppendedGroup().forEach(c -> c.setHoverEvent(new HoverEvent(
                action.toBungee(),
                content.toBungeeComponents()
            )));
        }
        return this;
    }

    public ColorAPI onHover(String hoverText) {
        return onHover(HoverAction.SHOW_TEXT, ColorAPI.colorize(hoverText));
    }

    public ColorAPI onClick(ClickAction action, String value) {
        if (!components.isEmpty()) {
            getLastAppendedGroup().forEach(c -> c.setClickEvent(new ClickEvent(
                action.toBungee(),
                value
            )));
        }
        return this;
    }
    
    /**
     * Finds all components that were part of the last logical block (word or formatted segment).
     * This makes hover/click events apply to whole colored words, not just the last letter.
     */
    private List<InternalTextComponent> getLastAppendedGroup() {
        if (components.isEmpty()) {
            return new ArrayList<>();
        }
        // A "group" is a sequence of components without spaces that share the same hover/click events.
        int lastIndex = components.size() - 1;
        InternalTextComponent last = components.get(lastIndex);
        HoverEvent h = last.getHoverEvent();
        ClickEvent c = last.getClickEvent();
        
        List<InternalTextComponent> group = new ArrayList<>();
        for (int i = lastIndex; i >= 0; i--) {
            InternalTextComponent current = components.get(i);
            if (current.getHoverEvent() == h && current.getClickEvent() == c) {
                group.add(current);
                if (current.getText().contains(" ")) { // Stop at spaces
                    break;
                }
            } else {
                break;
            }
        }
        return group;
    }

    public void send(CommandSender sender) {
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(toBungeeComponents());
        } else {
            sender.sendMessage(toLegacyText());
        }
    }

    public String toLegacyText() {
        return components.stream().map(InternalTextComponent::toLegacyText).collect(Collectors.joining());
    }

    public BaseComponent[] toBungeeComponents() {
        return components.stream().map(InternalTextComponent::toBungee).toArray(BaseComponent[]::new);
    }

    // --- The New Parsing Engine ---

    private static List<InternalTextComponent> parse(String text) {
        List<InternalTextComponent> components = new ArrayList<>();
        Matcher matcher = FORMATTING_PATTERN.matcher(text);

        TextColorizer state = new TextColorizer();
        int lastMatchEnd = 0;

        while (matcher.find()) {
            int start = matcher.start();
            // 1. Append any plain text before this match
            if (start > lastMatchEnd) {
                state.appendText(text.substring(lastMatchEnd, start), components);
            }

            String match = matcher.group();

            // 2. Process the matched code
            if (match.startsWith("<#") && !match.startsWith("</#")) { // Gradient Start
                state.beginGradient(ChatColor.of("#" + matcher.group(1)));
            } else if (match.startsWith("</#")) { // Gradient End
                state.endGradient(ChatColor.of("#" + matcher.group(2)));
            } else if (match.startsWith("&#")) { // Single Hex
                state.setColor(ChatColor.of("#" + matcher.group(3)));
            } else { // Legacy Code
                state.applyLegacyCode(ChatColor.getByChar(match.charAt(1)));
            }

            lastMatchEnd = matcher.end();
        }

        // 3. Append any remaining text after the last match
        if (lastMatchEnd < text.length()) {
            state.appendText(text.substring(lastMatchEnd), components);
        }

        return components;
    }

    /**
     * Interpolates between two colors.
     * @param factor A value from 0.0 to 1.0.
     */
    private static Color interpolate(Color color1, Color color2, float factor) {
        int r = (int) (color1.getRed() + factor * (color2.getRed() - color1.getRed()));
        int g = (int) (color1.getGreen() + factor * (color2.getGreen() - color1.getGreen()));
        int b = (int) (color1.getBlue() + factor * (color2.getBlue() - color1.getBlue()));
        return new Color(r, g, b);
    }

    /**
     * Inner class to manage the state of the text parser.
     */
    private static class TextColorizer {
        private ChatColor color = ChatColor.WHITE;
        private boolean bold, italic, underlined, strikethrough, magic;

        private ChatColor gradientStart, gradientEnd;
        private boolean inGradient = false;

        void setColor(ChatColor color) {
            if (!inGradient) {
                this.color = color;
                resetFormatting();
            }
        }
        
        private char getChatCode(ChatColor color) {
            return color.toString().charAt(1);
        }

        void applyLegacyCode(ChatColor code) {
            if (code == null) {
                return;
            }

            char codeChar = getChatCode(code);

            if ((codeChar >= '0' && codeChar <= '9') || (codeChar >= 'a' && codeChar <= 'f')) {
                setColor(code);
            } else {
                if (code == ChatColor.BOLD) {
                    bold = true;
                } else if (code == ChatColor.ITALIC) {
                    italic = true;
                } else if (code == ChatColor.UNDERLINE) {
                    underlined = true;
                } else if (code == ChatColor.STRIKETHROUGH) {
                    strikethrough = true;
                } else if (code == ChatColor.MAGIC) {
                    magic = true;
                } else if (code == ChatColor.RESET) {
                    this.color = null;
                    resetFormatting();
                    this.inGradient = false;
                }
            }
        }

        void beginGradient(ChatColor start) {
            this.inGradient = true;
            this.gradientStart = start;
        }

        void endGradient(ChatColor end) {
            this.gradientEnd = end;
        }

        void appendText(String text, List<InternalTextComponent> components) {
            if (inGradient) {
                // If the end tag wasn't found yet, assume it's the start color
                ChatColor finalEndColor = (gradientEnd != null) ? gradientEnd : gradientStart;

                Color start = gradientStart.getColor();
                Color end = finalEndColor.getColor();
                String cleanText = ChatColor.stripColor(text);
                int len = cleanText.length();
                
                for (int i = 0; i < len; i++) {
                    float factor = (len > 1) ? (float) i / (len - 1) : 0;
                    Color interpolated = interpolate(start, end, factor);
                    ChatColor charColor = ChatColor.of(interpolated);
                    components.add(new InternalTextComponent(
                            String.valueOf(cleanText.charAt(i)), charColor, bold, italic, underlined, strikethrough, magic));
                }
                
                // Reset gradient state after processing
                inGradient = false;
                gradientStart = null;
                gradientEnd = null;
            } else {
                components.add(new InternalTextComponent(
                        text, color, bold, italic, underlined, strikethrough, magic));
            }
        }

        private void resetFormatting() {
            bold = italic = underlined = strikethrough = magic = false;
        }
    }
}