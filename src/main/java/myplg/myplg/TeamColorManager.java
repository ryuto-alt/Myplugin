package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

/**
 * Manages team-colored player names during game
 */
public class TeamColorManager {
    private final PvPGame plugin;

    public TeamColorManager(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * Apply team colors to all players
     */
    public void applyTeamColors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            if (teamName != null) {
                applyTeamColor(player, teamName);
            }
        }
    }

    /**
     * Apply team color to a specific player
     */
    public void applyTeamColor(Player player, String teamName) {
        ChatColor color = getTeamChatColor(teamName);

        // Get player's scoreboard (or create new one)
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        // Create or get team for this color
        org.bukkit.scoreboard.Team scoreboardTeam = scoreboard.getTeam(teamName);
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.registerNewTeam(teamName);
        }

        // Set team color
        scoreboardTeam.setColor(color);
        scoreboardTeam.setPrefix(color.toString());

        // Add player to team
        if (!scoreboardTeam.hasEntry(player.getName())) {
            scoreboardTeam.addEntry(player.getName());
        }

        // Apply to all other players' scoreboards so they see the color
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (otherPlayer.equals(player)) continue;

            Scoreboard otherScoreboard = otherPlayer.getScoreboard();
            if (otherScoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
                continue; // Skip if using main scoreboard
            }

            org.bukkit.scoreboard.Team otherTeam = otherScoreboard.getTeam(teamName);
            if (otherTeam == null) {
                otherTeam = otherScoreboard.registerNewTeam(teamName);
            }

            otherTeam.setColor(color);
            otherTeam.setPrefix(color.toString());

            if (!otherTeam.hasEntry(player.getName())) {
                otherTeam.addEntry(player.getName());
            }
        }

        plugin.getLogger().info("Applied color " + color + " to player " + player.getName() + " (team: " + teamName + ")");
    }

    /**
     * Remove team colors from all players
     */
    public void removeTeamColors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = player.getScoreboard();

            // Unregister all teams
            for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
                team.unregister();
            }
        }
    }

    /**
     * Get ChatColor for a team name
     */
    private ChatColor getTeamChatColor(String teamName) {
        switch (teamName) {
            case "レッド":
                return ChatColor.RED;
            case "ブルー":
                return ChatColor.BLUE;
            case "グリーン":
                return ChatColor.GREEN;
            case "イエロー":
                return ChatColor.YELLOW;
            case "アクア":
                return ChatColor.AQUA;
            case "ホワイト":
                return ChatColor.WHITE;
            case "ピンク":
                return ChatColor.LIGHT_PURPLE;
            case "グレー":
                return ChatColor.GRAY;
            default:
                return ChatColor.WHITE;
        }
    }
}
