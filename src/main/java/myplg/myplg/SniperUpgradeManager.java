package myplg.myplg;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages sniper damage upgrades for individual players
 * Level 0: No upgrade (1.0x damage)
 * Level 1: 1.2x damage (cost: 5 diamonds)
 * Level 2: 1.35x damage (cost: 7 diamonds)
 */
public class SniperUpgradeManager {
    private final PvPGame plugin;
    private final Map<UUID, Integer> playerUpgradeLevels; // Player UUID -> Upgrade level (0, 1, 2)

    public SniperUpgradeManager(PvPGame plugin) {
        this.plugin = plugin;
        this.playerUpgradeLevels = new HashMap<>();
    }

    /**
     * Get the upgrade level for a player
     * @param playerUUID The player's UUID
     * @return Upgrade level (0, 1, or 2)
     */
    public int getUpgradeLevel(UUID playerUUID) {
        return playerUpgradeLevels.getOrDefault(playerUUID, 0);
    }

    /**
     * Get the damage multiplier for a player
     * @param playerUUID The player's UUID
     * @return Damage multiplier (1.0, 1.2, or 1.35)
     */
    public double getDamageMultiplier(UUID playerUUID) {
        int level = getUpgradeLevel(playerUUID);
        switch (level) {
            case 1:
                return 1.2;
            case 2:
                return 1.35;
            default:
                return 1.0;
        }
    }

    /**
     * Upgrade a player's sniper damage
     * @param playerUUID The player's UUID
     * @param newLevel The new level (1 or 2)
     * @return true if upgrade was successful
     */
    public boolean upgradeSniper(UUID playerUUID, int newLevel) {
        if (newLevel < 1 || newLevel > 2) {
            return false;
        }

        int currentLevel = getUpgradeLevel(playerUUID);
        if (currentLevel >= newLevel) {
            return false; // Already at this level or higher
        }

        // Level 2 requires level 1 first
        if (newLevel == 2 && currentLevel < 1) {
            return false;
        }

        playerUpgradeLevels.put(playerUUID, newLevel);
        plugin.getLogger().info("Player " + playerUUID + " upgraded sniper to level " + newLevel);
        return true;
    }

    /**
     * Reset all upgrades (called when game ends)
     */
    public void resetAll() {
        playerUpgradeLevels.clear();
        plugin.getLogger().info("All sniper upgrades have been reset");
    }

    /**
     * Reset upgrade for a specific player
     * @param playerUUID The player's UUID
     */
    public void resetPlayer(UUID playerUUID) {
        playerUpgradeLevels.remove(playerUUID);
    }
}
