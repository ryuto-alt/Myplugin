package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ScoreboardManager {
    private final PvPGame plugin;
    private final Map<String, Boolean> bedStatus; // Team name -> Bed alive status
    private final Map<String, Boolean> teamEliminated; // Team name -> Eliminated status
    private int rainbowIndex = 0; // For rainbow animation
    private final String[] RAINBOW_COLORS = {"§c", "§6", "§e", "§a", "§b", "§9", "§d"}; // Red, Gold, Yellow, Green, Aqua, Blue, Pink
    private Integer updateTaskId = null; // Track the update task ID
    private Integer bonusEmeraldCountdownTaskId = null; // Bonus emerald countdown task ID
    private int countdownSeconds = -1; // Countdown timer (-1 = disabled)
    private int bonusEmeraldCountdownSeconds = -1; // Bonus emerald countdown (-1 = disabled)
    private int bonusEmeraldLevel = 1; // 1 = I, 2 = II, 3 = III
    private int gameElapsedSeconds = 0; // Track game elapsed time
    private final Random random = new Random();

    // Time threshold for bonus emerald II/III (10-15 minutes = 600-900 seconds, using 12 min = 720)
    private static final int BONUS_EMERALD_UPGRADE_TIME = 720;

    public ScoreboardManager(PvPGame plugin) {
        this.plugin = plugin;
        this.bedStatus = new HashMap<>();
        this.teamEliminated = new HashMap<>();
    }

    /**
     * Start the scoreboard update task (called when game starts)
     */
    public void startUpdateTask() {
        // Cancel existing task if any
        stopUpdateTask();

        // Start rainbow animation task (updates every 20 ticks = 1 second) - 負荷軽減
        updateTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            rainbowIndex = (rainbowIndex + 1) % RAINBOW_COLORS.length;
            updateAllScoreboards();
        }, 0L, 20L).getTaskId();

        // Reset game elapsed time
        gameElapsedSeconds = 0;

        // Initialize bonus emerald with random level and time
        selectNextBonusEmerald();

        // Start bonus emerald countdown task (updates every 20 ticks = 1 second)
        bonusEmeraldCountdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Track game elapsed time
            gameElapsedSeconds++;

            if (bonusEmeraldCountdownSeconds > 0) {
                bonusEmeraldCountdownSeconds--;
            } else {
                // Spawn bonus emeralds
                spawnBonusEmeralds();
                // Select next bonus emerald (random level and time)
                selectNextBonusEmerald();
            }
        }, 20L, 20L).getTaskId();
    }

    /**
     * Select next bonus emerald level and countdown time randomly
     */
    private void selectNextBonusEmerald() {
        // Check if game is still in early phase (first 12 minutes)
        if (gameElapsedSeconds < BONUS_EMERALD_UPGRADE_TIME) {
            // Early game: only level I (1 emerald)
            bonusEmeraldLevel = 1;
            // I: 2-3 minutes (120-180 seconds)
            bonusEmeraldCountdownSeconds = 120 + random.nextInt(61); // 120 to 180
        } else {
            // After 12 minutes: randomly select II or III
            bonusEmeraldLevel = random.nextBoolean() ? 2 : 3;

            // Set countdown based on level
            if (bonusEmeraldLevel == 2) {
                // II: 2-3 minutes (120-180 seconds)
                bonusEmeraldCountdownSeconds = 120 + random.nextInt(61); // 120 to 180
            } else {
                // III: 4-5 minutes (240-300 seconds)
                bonusEmeraldCountdownSeconds = 240 + random.nextInt(61); // 240 to 300
            }
        }
    }

    /**
     * Spawn bonus emeralds at all emerald generator locations
     */
    private void spawnBonusEmeralds() {
        int amount = bonusEmeraldLevel; // II = 2, III = 3

        // Find all emerald generators and spawn bonus emeralds
        for (Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
            if (generator.getMaterial() == Material.EMERALD) {
                Location loc = generator.getCorner1().clone().add(0.5, 1.0, 0.5);
                for (int i = 0; i < amount; i++) {
                    org.bukkit.entity.Item item = loc.getWorld().dropItem(loc, new ItemStack(Material.EMERALD, 1));
                    item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                }
            }
        }

        // Broadcast message
        String levelText = bonusEmeraldLevel == 1 ? "I" : (bonusEmeraldLevel == 2 ? "II" : "III");
        Bukkit.broadcastMessage("§2§lエメラルド" + levelText + "§aが出現しました！");
    }

    /**
     * Stop the scoreboard update task (called when game ends or plugin disables)
     */
    public void stopUpdateTask() {
        if (updateTaskId != null) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = null;
        }
        if (bonusEmeraldCountdownTaskId != null) {
            Bukkit.getScheduler().cancelTask(bonusEmeraldCountdownTaskId);
            bonusEmeraldCountdownTaskId = null;
        }
        bonusEmeraldCountdownSeconds = -1;
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

        // Bonus emerald countdown display (at the top)
        if (bonusEmeraldCountdownSeconds >= 0) {
            int minutes = bonusEmeraldCountdownSeconds / 60;
            int seconds = bonusEmeraldCountdownSeconds % 60;
            String levelText = bonusEmeraldLevel == 1 ? "I" : (bonusEmeraldLevel == 2 ? "II" : "III");
            objective.getScore("§2§lエメラルド" + levelText + " §7- §a" + String.format("%d:%02d", minutes, seconds)).setScore(score--);
            objective.getScore("   ").setScore(score--);
        }

        // Teams section
        objective.getScore("§l§nチーム状況:").setScore(score--);

        // Display each team's bed status and player count
        for (Team team : plugin.getGameManager().getTeams().values()) {
            String teamName = team.getName();
            boolean bedAlive = bedStatus.getOrDefault(teamName, true);
            boolean eliminated = teamEliminated.getOrDefault(teamName, false);

            String prefix = getTeamPrefix(teamName);

            // Count alive players in team (exclude eliminated players)
            int aliveCount = 0;
            for (UUID memberUUID : team.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    // Check if player is eliminated
                    if (plugin.getPlayerDeathListener() != null &&
                        !plugin.getPlayerDeathListener().isPlayerEliminated(memberUUID)) {
                        aliveCount++;
                    }
                }
            }

            String status;
            if (eliminated) {
                // Team is completely eliminated - show X mark
                status = "§c✗";
            } else if (bedAlive) {
                // Bed is alive - show checkmark (even if no members)
                status = "§a✓";
            } else if (aliveCount == 0) {
                // Bed destroyed and no players - show X mark
                status = "§c✗";
            } else {
                // Bed destroyed but team has players - show player count
                status = "§c" + aliveCount;
            }

            // Build the line with YOU indicator on the right side
            String youIndicator = teamName.equals(playerTeamName) ? " §7YOU" : "";
            String teamDisplay = teamName.equals(playerTeamName) ? "§l" + teamName : teamName;

            String line = prefix + " " + teamDisplay + " " + status + youIndicator;
            objective.getScore(line).setScore(score--);
        }

        // Countdown timer (if active)
        if (countdownSeconds >= 0) {
            objective.getScore("    ").setScore(score--);
            int minutes = countdownSeconds / 60;
            int seconds = countdownSeconds % 60;
            String countdownColor = countdownSeconds <= 60 ? "§4§l" : (countdownSeconds <= 180 ? "§c§l" : "§e§l");
            objective.getScore(countdownColor + "ベッド破壊まで:").setScore(score--);
            objective.getScore(countdownColor + String.format("%d:%02d", minutes, seconds)).setScore(score--);
        }

        // Bottom spacing
        objective.getScore("  ").setScore(score--);

        // Rainbow UnoPixel.net
        String rainbowText = getRainbowText("www.UnoPixel.net");
        objective.getScore(rainbowText).setScore(score--);
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
     * Set team as eliminated
     */
    public void setTeamEliminated(String teamName) {
        teamEliminated.put(teamName, true);
        updateAllScoreboards();
    }

    /**
     * Initialize all teams' beds as alive
     */
    public void initializeAllBeds() {
        bedStatus.clear();
        teamEliminated.clear();
        for (Team team : plugin.getGameManager().getTeams().values()) {
            bedStatus.put(team.getName(), true);
            teamEliminated.put(team.getName(), false);
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
     * Set countdown timer (seconds remaining until bed destruction)
     * Set to -1 to disable countdown
     */
    public void setCountdown(int seconds) {
        this.countdownSeconds = seconds;
    }

    /**
     * Get rainbow text with cycling colors
     */
    private String getRainbowText(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int colorIndex = (rainbowIndex + i) % RAINBOW_COLORS.length;
            result.append(RAINBOW_COLORS[colorIndex]).append(text.charAt(i));
        }
        return result.toString();
    }

    /**
     * Get team color prefix
     */
    public String getTeamPrefix(String teamName) {
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


    /**
     * Get team color code
     */
    public String getTeamColorCode(String teamName) {
        switch (teamName) {
            case "レッド": return "§c";
            case "ブルー": return "§9";
            case "グリーン": return "§a";
            case "イエロー": return "§e";
            case "アクア": return "§b";
            case "ホワイト": return "§f";
            case "ピンク": return "§d";
            case "グレー": return "§7";
            default: return "§f";
        }
    }

    /**
     * Update player's tab list name with team color
     */
    public void updatePlayerTabListName(Player player) {
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        if (teamName != null) {
            String colorCode = getTeamColorCode(teamName);
            player.setPlayerListName(colorCode + player.getName());
        } else {
            player.setPlayerListName(player.getName());
        }
    }

    /**
     * Update all players' tab list names with team colors
     */
    public void updateAllPlayerTabListNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTabListName(player);
        }
    }

    /**
     * Reset player's tab list name to default
     */
    public void resetPlayerTabListName(Player player) {
        player.setPlayerListName(player.getName());
    }

    /**
     * Reset all players' tab list names to default
     */
    public void resetAllPlayerTabListNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayerTabListName(player);
        }
    }
}
