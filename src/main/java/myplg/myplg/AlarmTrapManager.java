package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Manages alarm traps for teams
 * Alerts team when enemies enter 20m radius of bed
 * Applies debuffs to enemies
 */
public class AlarmTrapManager {
    private final PvPGame plugin;
    private final Map<String, Integer> teamAlarmLevel; // Team name -> Alarm level (0, 1, 2)
    private final Map<String, Set<UUID>> recentlyTriggered; // Team name -> Recently triggered players (cooldown)
    private final int ALARM_RADIUS = 20; // 20 blocks
    private final int COOLDOWN_SECONDS = 5; // 5 second cooldown per player
    private Integer taskId = null;

    public AlarmTrapManager(PvPGame plugin) {
        this.plugin = plugin;
        this.teamAlarmLevel = new HashMap<>();
        this.recentlyTriggered = new HashMap<>();
    }

    /**
     * Start the alarm trap detection task
     */
    public void startAlarmTask() {
        if (taskId != null) {
            stopAlarmTask();
        }

        // Check every 10 ticks (0.5 seconds) for enemies near beds
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            checkAllTraps();
        }, 0L, 10L).getTaskId();
    }

    /**
     * Stop the alarm trap detection task
     */
    public void stopAlarmTask() {
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = null;
        }
    }

    /**
     * Purchase/upgrade alarm trap for a team
     */
    public boolean upgradeAlarm(String teamName, int newLevel) {
        int currentLevel = teamAlarmLevel.getOrDefault(teamName, 0);

        if (newLevel <= currentLevel) {
            return false; // Already at this level or higher
        }

        if (newLevel > 2) {
            return false; // Max level is 2
        }

        teamAlarmLevel.put(teamName, newLevel);
        plugin.getLogger().info("Team " + teamName + " upgraded alarm to level " + newLevel);
        return true;
    }

    /**
     * Get alarm level for a team
     */
    public int getAlarmLevel(String teamName) {
        return teamAlarmLevel.getOrDefault(teamName, 0);
    }

    /**
     * Reset all alarms
     */
    public void reset() {
        teamAlarmLevel.clear();
        recentlyTriggered.clear();
    }

    /**
     * Check all alarm traps
     */
    private void checkAllTraps() {
        for (Team team : plugin.getGameManager().getTeams().values()) {
            String teamName = team.getName();
            int alarmLevel = getAlarmLevel(teamName);

            if (alarmLevel == 0) {
                continue; // No alarm trap
            }

            // Check if bed still exists
            if (team.getBedBlock() == null || !plugin.getScoreboardManager().isBedAlive(teamName)) {
                continue; // No bed, no trap
            }

            Location bedLocation = team.getBedBlock().getLocation();

            // Check for enemy players within radius
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

                // Skip if same team or no team
                if (playerTeam == null || playerTeam.equals(teamName)) {
                    continue;
                }

                // Check if in same world
                if (!player.getWorld().equals(bedLocation.getWorld())) {
                    continue;
                }

                // Check distance
                double distance = player.getLocation().distance(bedLocation);
                if (distance <= ALARM_RADIUS) {
                    // Enemy detected! Trigger alarm
                    triggerAlarm(teamName, player, alarmLevel);
                }
            }
        }
    }

    /**
     * Trigger alarm for a team
     */
    private void triggerAlarm(String teamName, Player enemy, int alarmLevel) {
        // Check cooldown
        Set<UUID> triggered = recentlyTriggered.computeIfAbsent(teamName, k -> new HashSet<>());
        if (triggered.contains(enemy.getUniqueId())) {
            return; // Still in cooldown
        }

        // Add to cooldown
        triggered.add(enemy.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            triggered.remove(enemy.getUniqueId());
        }, COOLDOWN_SECONDS * 20L);

        // Get team
        Team team = plugin.getGameManager().getTeam(teamName);
        if (team == null) return;

        // Notify team members
        for (UUID memberUUID : team.getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage("§c§l[アラーム] §e敵が陣地に侵入しました！");
                member.sendMessage("§7侵入者: §f" + enemy.getName());
                member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

                // Play additional alarm sound
                for (int i = 0; i < 3; i++) {
                    final int delay = i * 5;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }, delay);
                }
            }
        }

        // Apply effects to enemy based on alarm level
        if (alarmLevel >= 2) {
            // Level 2: Apply debuffs for 3 seconds
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // 3 seconds
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1)); // 3 seconds, level 2
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0)); // 3 seconds

            enemy.sendMessage("§c§lアラームが発動しました！");
            enemy.playSound(enemy.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
        }

        plugin.getLogger().info("Alarm triggered for team " + teamName + " by player " + enemy.getName() + " (Level " + alarmLevel + ")");
    }
}
