package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    private final PvPGame plugin;
    private final Map<String, Boolean> bedStatus; // Team name -> Bed alive status

    public ScoreboardManager(PvPGame plugin) {
        this.plugin = plugin;
        this.bedStatus = new HashMap<>();
    }

    /**
     * Create and display scoreboard for a player
     */
    public void createScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("bedwars", "dummy", "§6§lBED WARS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        updateScoreboard(player, scoreboard, objective);
        player.setScoreboard(scoreboard);
    }

    /**
     * Update scoreboard content
     */
    public void updateScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int score = 15;

        // Get player's team
        String playerTeamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

        // Title spacing
        objective.getScore(" ").setScore(score--);

        // Display player's own team info
        if (playerTeamName != null) {
            Team playerTeam = plugin.getGameManager().getTeam(playerTeamName);
            if (playerTeam != null) {
                String prefix = getTeamPrefix(playerTeamName);
                objective.getScore("§l§n自分のチーム:").setScore(score--);
                objective.getScore(prefix + " §l" + playerTeamName).setScore(score--);
                objective.getScore("   ").setScore(score--);
            }
        }

        // Teams section
        objective.getScore("§l§nチーム状況:").setScore(score--);

        // Display each team's bed status and player count
        for (Team team : plugin.getGameManager().getTeams().values()) {
            String teamName = team.getName();
            boolean bedAlive = bedStatus.getOrDefault(teamName, true);

            String prefix = getTeamPrefix(teamName);

            // Count alive players in team
            int aliveCount = 0;
            for (UUID memberUUID : team.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    aliveCount++;
                }
            }

            String status;
            if (bedAlive) {
                status = "§a✓";
            } else {
                // Show player count if bed is destroyed
                status = "§c" + aliveCount;
            }

            // Highlight player's own team
            String teamDisplay = teamName;
            if (teamName.equals(playerTeamName)) {
                teamDisplay = "§l" + teamName + " §7(YOU)";
            }

            String line = prefix + " " + teamDisplay + " " + status;
            objective.getScore(line).setScore(score--);
        }

        // Bottom spacing
        objective.getScore("  ").setScore(score--);
        objective.getScore("§ewww.hypixel.net").setScore(score--);
    }

    /**
     * Update all players' scoreboards
     */
    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = player.getScoreboard();
            if (scoreboard != null) {
                Objective objective = scoreboard.getObjective("bedwars");
                if (objective != null) {
                    updateScoreboard(player, scoreboard, objective);
                }
            }
        }
    }

    /**
     * Set bed status for a team
     */
    public void setBedStatus(String teamName, boolean alive) {
        bedStatus.put(teamName, alive);
        updateAllScoreboards();
    }

    /**
     * Get bed status for a team
     */
    public boolean isBedAlive(String teamName) {
        return bedStatus.getOrDefault(teamName, true);
    }

    /**
     * Initialize all teams' beds as alive
     */
    public void initializeAllBeds() {
        bedStatus.clear();
        for (Team team : plugin.getGameManager().getTeams().values()) {
            bedStatus.put(team.getName(), true);
        }
    }

    /**
     * Remove all scoreboards
     */
    public void removeAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    /**
     * Get team color prefix
     */
    private String getTeamPrefix(String teamName) {
        switch (teamName) {
            case "レッド": return "§cR";
            case "ブルー": return "§9B";
            case "グリーン": return "§aG";
            case "イエロー": return "§eY";
            case "アクア": return "§bA";
            case "ホワイト": return "§fW";
            case "ピンク": return "§dP";
            case "グレー": return "§7G";
            default: return "§f?";
        }
    }
}
