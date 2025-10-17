package myplg.myplg;

import java.util.HashMap;
import java.util.Map;

public class ArmorUpgradeManager {
    private final PvPGame plugin;
    private final Map<String, Integer> teamArmorLevels; // Team name -> armor level (0-3)

    public ArmorUpgradeManager(PvPGame plugin) {
        this.plugin = plugin;
        this.teamArmorLevels = new HashMap<>();
    }

    /**
     * Get the current armor upgrade level for a team
     * @param teamName The team name
     * @return Armor level (0 = none, 1-3 = Protection I-III)
     */
    public int getArmorLevel(String teamName) {
        return teamArmorLevels.getOrDefault(teamName, 0);
    }

    /**
     * Upgrade a team's armor to the specified level
     * @param teamName The team name
     * @param level The new level (1-3)
     * @return true if upgrade was successful
     */
    public boolean upgradeArmor(String teamName, int level) {
        if (level < 1 || level > 3) {
            return false;
        }

        int currentLevel = getArmorLevel(teamName);
        if (currentLevel >= level) {
            return false; // Already at this level or higher
        }

        teamArmorLevels.put(teamName, level);
        plugin.getLogger().info("Team " + teamName + " upgraded armor to Protection " + level);
        return true;
    }

    /**
     * Check if a team has a specific armor upgrade level
     * @param teamName The team name
     * @param level The level to check
     * @return true if team has this upgrade or higher
     */
    public boolean hasArmorUpgrade(String teamName, int level) {
        return getArmorLevel(teamName) >= level;
    }

    /**
     * Reset all armor upgrades (called when game starts/ends)
     */
    public void resetAll() {
        teamArmorLevels.clear();
        plugin.getLogger().info("All armor upgrades have been reset");
    }

    /**
     * Reset armor upgrades for a specific team
     * @param teamName The team name
     */
    public void resetTeam(String teamName) {
        teamArmorLevels.remove(teamName);
        plugin.getLogger().info("Armor upgrades reset for team: " + teamName);
    }
}
