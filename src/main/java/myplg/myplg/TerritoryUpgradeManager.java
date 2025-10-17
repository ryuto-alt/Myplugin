package myplg.myplg;

import java.util.HashMap;
import java.util.Map;

public class TerritoryUpgradeManager {
    private final PvPGame plugin;
    private final Map<String, Integer> teamUpgradeLevels; // Team name -> upgrade level (0-3)

    public TerritoryUpgradeManager(PvPGame plugin) {
        this.plugin = plugin;
        this.teamUpgradeLevels = new HashMap<>();
    }

    /**
     * Get the current upgrade level for a team
     * @param teamName The team name
     * @return Upgrade level (0 = none, 1 = heal, 2 = speed, 3 = evolution)
     */
    public int getUpgradeLevel(String teamName) {
        return teamUpgradeLevels.getOrDefault(teamName, 0);
    }

    /**
     * Upgrade a team's territory to the specified level
     * @param teamName The team name
     * @param level The new level
     * @return true if upgrade was successful
     */
    public boolean upgradeTerritory(String teamName, int level) {
        if (level < 1 || level > 3) {
            return false;
        }

        int currentLevel = getUpgradeLevel(teamName);
        if (currentLevel >= level) {
            return false; // Already at this level or higher
        }

        teamUpgradeLevels.put(teamName, level);
        plugin.getLogger().info("Team " + teamName + " upgraded territory to level " + level);
        return true;
    }

    /**
     * Check if a team has a specific upgrade
     * @param teamName The team name
     * @param level The level to check
     * @return true if team has this upgrade or higher
     */
    public boolean hasUpgrade(String teamName, int level) {
        return getUpgradeLevel(teamName) >= level;
    }

    /**
     * Reset all upgrades (called when game starts/ends)
     */
    public void resetAll() {
        teamUpgradeLevels.clear();
        plugin.getLogger().info("All territory upgrades have been reset");
    }

    /**
     * Reset upgrades for a specific team
     * @param teamName The team name
     */
    public void resetTeam(String teamName) {
        teamUpgradeLevels.remove(teamName);
        plugin.getLogger().info("Territory upgrades reset for team: " + teamName);
    }
}
