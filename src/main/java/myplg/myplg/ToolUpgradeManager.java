package myplg.myplg;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToolUpgradeManager {
    private final PvPGame plugin;

    // Player UUID -> Axe Level (0-4)
    private final Map<UUID, Integer> axeLevels;

    // Player UUID -> Pickaxe Level (0-4)
    private final Map<UUID, Integer> pickaxeLevels;

    public ToolUpgradeManager(PvPGame plugin) {
        this.plugin = plugin;
        this.axeLevels = new HashMap<>();
        this.pickaxeLevels = new HashMap<>();
    }

    /**
     * Get the current axe level for a player
     * @param playerUUID Player's UUID
     * @return Axe level (0-4)
     */
    public int getAxeLevel(UUID playerUUID) {
        return axeLevels.getOrDefault(playerUUID, 0);
    }

    /**
     * Get the current pickaxe level for a player
     * @param playerUUID Player's UUID
     * @return Pickaxe level (0-4)
     */
    public int getPickaxeLevel(UUID playerUUID) {
        return pickaxeLevels.getOrDefault(playerUUID, 0);
    }

    /**
     * Upgrade the player's axe to the next level
     * @param playerUUID Player's UUID
     * @param targetLevel Target level (1-4)
     * @return true if upgraded successfully, false if already at or above that level
     */
    public boolean upgradeAxe(UUID playerUUID, int targetLevel) {
        int currentLevel = getAxeLevel(playerUUID);

        if (targetLevel <= currentLevel) {
            return false; // Already at or above this level
        }

        if (targetLevel < 1 || targetLevel > 4) {
            return false; // Invalid level
        }

        axeLevels.put(playerUUID, targetLevel);
        return true;
    }

    /**
     * Upgrade the player's pickaxe to the next level
     * @param playerUUID Player's UUID
     * @param targetLevel Target level (1-4)
     * @return true if upgraded successfully, false if already at or above that level
     */
    public boolean upgradePickaxe(UUID playerUUID, int targetLevel) {
        int currentLevel = getPickaxeLevel(playerUUID);

        if (targetLevel <= currentLevel) {
            return false; // Already at or above this level
        }

        if (targetLevel < 1 || targetLevel > 4) {
            return false; // Invalid level
        }

        pickaxeLevels.put(playerUUID, targetLevel);
        return true;
    }

    /**
     * Get the material for the axe at the specified level
     * @param level Axe level (0-4)
     * @return Material for the axe
     */
    public Material getAxeMaterial(int level) {
        switch (level) {
            case 1: return Material.WOODEN_AXE;
            case 2: return Material.STONE_AXE;
            case 3: return Material.IRON_AXE;
            case 4: return Material.DIAMOND_AXE;
            default: return null;
        }
    }

    /**
     * Get the material for the pickaxe at the specified level
     * @param level Pickaxe level (0-4)
     * @return Material for the pickaxe
     */
    public Material getPickaxeMaterial(int level) {
        switch (level) {
            case 1: return Material.WOODEN_PICKAXE;
            case 2: return Material.STONE_PICKAXE;
            case 3: return Material.IRON_PICKAXE;
            case 4: return Material.DIAMOND_PICKAXE;
            default: return null;
        }
    }

    /**
     * Reset player's tool upgrades (called on death or game end)
     * @param playerUUID Player's UUID
     */
    public void resetPlayer(UUID playerUUID) {
        axeLevels.remove(playerUUID);
        pickaxeLevels.remove(playerUUID);
    }

    /**
     * Reset all players' tool upgrades
     */
    public void resetAll() {
        axeLevels.clear();
        pickaxeLevels.clear();
    }
}
