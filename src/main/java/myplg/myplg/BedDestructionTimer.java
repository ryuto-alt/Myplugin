package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Manages the 35-minute bed destruction timer
 * Destroys all beds after 35 minutes to prevent stalemates
 */
public class BedDestructionTimer {
    private final PvPGame plugin;
    private static final int BED_DESTRUCTION_TIME = 35 * 60; // 35 minutes in seconds
    private static final int COUNTDOWN_START = 5 * 60; // Start countdown at 5 minutes
    private int remainingSeconds = BED_DESTRUCTION_TIME;
    private Integer taskId = null;

    public BedDestructionTimer(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the bed destruction timer
     */
    public void startTimer() {
        // Cancel existing timer if any
        stopTimer();

        remainingSeconds = BED_DESTRUCTION_TIME;

        // Run task every second (20 ticks)
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remainingSeconds--;

            // Update scoreboard every second during countdown
            if (remainingSeconds <= COUNTDOWN_START) {
                plugin.getScoreboardManager().setCountdown(remainingSeconds);
            }

            // Announce at specific intervals
            if (remainingSeconds == 30 * 60) { // 30 minutes
                Bukkit.broadcastMessage("§c§l[警告] ベッド破壊まで残り30分！");
                playWarningSound();
            } else if (remainingSeconds == 20 * 60) { // 20 minutes
                Bukkit.broadcastMessage("§c§l[警告] ベッド破壊まで残り20分！");
                playWarningSound();
            } else if (remainingSeconds == 10 * 60) { // 10 minutes
                Bukkit.broadcastMessage("§c§l[警告] ベッド破壊まで残り10分！");
                playWarningSound();
            } else if (remainingSeconds == 5 * 60) { // 5 minutes - start countdown
                Bukkit.broadcastMessage("§4§l[緊急警告] ベッド破壊まで残り5分！カウントダウン開始！");
                playWarningSound();
            } else if (remainingSeconds == 3 * 60) { // 3 minutes
                Bukkit.broadcastMessage("§4§l[緊急警告] ベッド破壊まで残り3分！");
                playWarningSound();
            } else if (remainingSeconds == 60) { // 1 minute
                Bukkit.broadcastMessage("§4§l[最終警告] ベッド破壊まで残り1分！");
                playWarningSound();
            } else if (remainingSeconds == 30) { // 30 seconds
                Bukkit.broadcastMessage("§4§l[最終警告] ベッド破壊まで残り30秒！");
                playWarningSound();
            } else if (remainingSeconds == 10) { // 10 seconds
                Bukkit.broadcastMessage("§4§l10");
                playCountdownSound();
            } else if (remainingSeconds <= 5 && remainingSeconds > 0) {
                Bukkit.broadcastMessage("§4§l" + remainingSeconds);
                playCountdownSound();
            } else if (remainingSeconds == 0) {
                // Destroy all beds
                destroyAllBeds();
                stopTimer();
            }
        }, 0L, 20L).getTaskId(); // Run every second
    }

    /**
     * Stop the timer
     */
    public void stopTimer() {
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = null;
        }
        remainingSeconds = BED_DESTRUCTION_TIME;
        plugin.getScoreboardManager().setCountdown(-1); // Hide countdown
    }

    /**
     * Get remaining seconds
     */
    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * Destroy all remaining beds
     */
    private void destroyAllBeds() {
        Bukkit.broadcastMessage("§4§l§n=== ベッド破壊タイム ===");
        Bukkit.broadcastMessage("§c全てのベッドが破壊されました！");
        Bukkit.broadcastMessage("§c残りプレイヤーのみで戦ってください！");

        int destroyedCount = 0;

        for (Team team : plugin.getGameManager().getTeams().values()) {
            // Check if bed is still alive
            if (plugin.getScoreboardManager().isBedAlive(team.getName())) {
                // Destroy bed block
                if (team.getBedBlock() != null) {
                    team.getBedBlock().setType(Material.AIR);
                    destroyedCount++;
                    plugin.getLogger().info("Time limit: Destroyed bed for team " + team.getName());
                }

                // Update scoreboard
                plugin.getScoreboardManager().setBedStatus(team.getName(), false);

                // Notify team
                for (java.util.UUID memberUUID : team.getMembers()) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.sendMessage("§c§l[警告] あなたのチームのベッドが時間切れで破壊されました！");
                        member.playSound(member.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }
                }
            }
        }

        plugin.getLogger().info("Time limit bed destruction: Destroyed " + destroyedCount + " beds");

        // Play dramatic sound for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
    }

    /**
     * Play warning sound for all players
     */
    private void playWarningSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }
    }

    /**
     * Play countdown sound for all players
     */
    private void playCountdownSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }
    }
}
