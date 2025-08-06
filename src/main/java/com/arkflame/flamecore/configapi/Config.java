package com.arkflame.flamecore.configapi;

import com.arkflame.flamecore.colorapi.ColorAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper around a single YAML configuration file, providing easy and color-aware access to its values.
 * This class should be obtained via {@link ConfigAPI#getConfig(String)}.
 */
public class Config {
    private final File file;
    private FileConfiguration config;

    Config(String path, JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(path, false);
            } catch (Exception ex) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    // --- Standard Getters (String, int, etc.) ---
    public String getString(String path) {
        String raw = config.getString(path);
        return raw != null ? ColorAPI.colorize(raw).toLegacyText() : null;
    }
    public String getString(String path, String def) {
        String raw = config.getString(path, def);
        return raw != null ? ColorAPI.colorize(raw).toLegacyText() : def;
    }
    public int getInt(String path) { return config.getInt(path); }
    public int getInt(String path, int def) { return config.getInt(path, def); }
    public double getDouble(String path) { return config.getDouble(path); }
    public double getDouble(String path, double def) { return config.getDouble(path, def); }
    public boolean getBoolean(String path) { return config.getBoolean(path); }
    public boolean getBoolean(String path, boolean def) { return config.getBoolean(path, def); }
    public List<String> getStringList(String path) {
        return config.getStringList(path).stream()
                .map(line -> ColorAPI.colorize(line).toLegacyText())
                .collect(Collectors.toList());
    }
    public ItemStack[] getItems(String path) {
        List<?> list = config.getList(path);
        if (list == null) return new ItemStack[0];
        return list.toArray(new ItemStack[0]);
    }

    // --- Custom Location Serialization System ---

    /**
     * Gets a LocationWrapper from the config at the specified path.
     * @param path The path to the location object in the YAML.
     * @return A LocationWrapper, or null if the path or required values are not found.
     */
    public ConfigLocation getLocation(String path) {
        if (!config.isConfigurationSection(path)) {
            return null;
        }
        ConfigurationSection section = config.getConfigurationSection(path);
        String worldName = section.getString("world");
        if (worldName == null) return null;

        return new ConfigLocation(
            worldName,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw", 0.0),
            (float) section.getDouble("pitch", 0.0)
        );
    }

    /**
     * Sets a Bukkit Location at a specified path by serializing it into a YAML object.
     * Remember to call {@link #save()} to persist changes.
     * @param path The path to set the location object at.
     * @param location The Bukkit Location to save.
     */
    public void setLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            config.set(path, null);
            return;
        }
        ConfigurationSection section = config.isConfigurationSection(path) 
            ? config.getConfigurationSection(path) 
            : config.createSection(path);
            
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }
    
    // --- Other Methods ---
    public void set(String path, Object value) { config.set(path, value); }
    public boolean contains(String path) { return config.contains(path); }
    public ConfigurationSection getSection(String path) { return config.getConfigurationSection(path); }
    public void save() {
        try { config.save(file); } 
        catch (IOException e) { e.printStackTrace(); }
    }
    public void reload() { this.config = YamlConfiguration.loadConfiguration(file); }
    public FileConfiguration getRaw() { return config; }

    /**
     * A serializable, Bukkit-independent representation of a location.
     */
    public static class ConfigLocation {
        private final String worldName;
        private final double x, y, z;
        private final float yaw, pitch;

        public ConfigLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Location toLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, x, y, z, yaw, pitch);
        }
        
        public String getWorldName() { return worldName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }
}