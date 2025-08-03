package com.arkflame.flamecore.blocksapi;

import org.bukkit.Material;

import com.arkflame.flamecore.materialapi.MaterialAPI;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A version-agnostic snapshot of a block's state, including material, data, and tile entity information.
 * This object can be serialized to a string for persistent storage.
 */
public class BlockWrapper {
    private final String materialName;
    private final byte legacyData;
    private final Map<String, String> extraData;

    public BlockWrapper(String materialName, byte legacyData, Map<String, String> extraData) {
        this.materialName = materialName;
        this.legacyData = legacyData;
        this.extraData = extraData != null ? extraData : new HashMap<>();
    }
    
    public Material getMaterial() {
        // Use our MaterialAPI for safe, version-independent lookups
        return MaterialAPI.getOrAir(materialName);
    }

    public String getMaterialName() { return materialName; }
    public byte getLegacyData() { return legacyData; }
    public Map<String, String> getExtraData() { return extraData; }

    /**
     * Serializes this BlockWrapper into a compact, storable string.
     * Format: material=MATERIAL_NAME|data=LEGACY_DATA|extraKey=base64(extraValue)|...
     * @return A serialized string representation.
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("material=").append(materialName);
        sb.append("|data=").append(legacyData);

        for (Map.Entry<String, String> entry : extraData.entrySet()) {
            sb.append("|").append(entry.getKey()).append("=")
              .append(Base64.getEncoder().encodeToString(entry.getValue().getBytes()));
        }
        return sb.toString();
    }

    /**
     * Deserializes a string back into a BlockWrapper object.
     * @param serializedData The string to deserialize.
     * @return A new BlockWrapper, or null if the string is invalid.
     */
    public static BlockWrapper deserialize(String serializedData) {
        if (serializedData == null || serializedData.isEmpty()) {
            return null;
        }

        String materialName = "AIR";
        byte legacyData = 0;
        Map<String, String> extraData = new HashMap<>();

        String[] parts = serializedData.split("\\|");
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length != 2) continue;

            String key = keyValue[0];
            String value = keyValue[1];

            switch (key) {
                case "material":
                    materialName = value;
                    break;
                case "data":
                    legacyData = Byte.parseByte(value);
                    break;
                default: // All other keys are assumed to be Base64 encoded extra data
                    extraData.put(key, new String(Base64.getDecoder().decode(value)));
                    break;
            }
        }
        return new BlockWrapper(materialName, legacyData, extraData);
    }
}