package myplg.myplg;

import java.util.HashMap;
import java.util.Map;

public class WeaponUpgradeManager {
    private final PvPGame plugin;
    private final Map<String, Integer> teamWeaponLevels; // Team name -> weapon level (0 or 1)

    public WeaponUpgradeManager(PvPGame plugin) {
        this.plugin = plugin;
        this.teamWeaponLevels = new HashMap<>();
    }

    /**
     * Get the current weapon upgrade level for a team
     * @param teamName The team name
     * @return Weapon level (0 = none, 1 = sharpness I)
     */
    public int getWeaponLevel(String teamName) {
        return teamWeaponLevels.getOrDefault(teamName, 0);
    }

    /**
     * Upgrade a team's weapon
     * @param teamName The team name
     * @return true if upgrade was successful
     */
    public boolean upgradeWeapon(String teamName) {
        int currentLevel = getWeaponLevel(teamName);
        if (currentLevel >= 1) {
            return false; // Already upgraded
        }

        teamWeaponLevels.put(teamName, 1);
        plugin.getLogger().info("Team " + teamName + " upgraded weapons to Sharpness I");
        return true;
    }

    /**
     * Check if a team has weapon upgrade
     * @param teamName The team name
     * @return true if team has weapon upgrade
     */
    public boolean hasWeaponUpgrade(String teamName) {
        return getWeaponLevel(teamName) >= 1;
    }

    /**
     * Reset all weapon upgrades (called when game starts/ends)
     */
    public void resetAll() {
        teamWeaponLevels.clear();
        plugin.getLogger().info("All weapon upgrades have been reset");
    }

    /**
     * Reset weapon upgrades for a specific team
     * @param teamName The team name
     */
    public void resetTeam(String teamName) {
        teamWeaponLevels.remove(teamName);
        plugin.getLogger().info("Weapon upgrades reset for team: " + teamName);
    }
}
