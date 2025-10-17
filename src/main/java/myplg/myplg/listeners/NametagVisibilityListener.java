package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Controls nametag visibility - only shows nametags to players within 20 blocks
 * Uses scoreboard teams to hide nametags when players are too far
 */
public class NametagVisibilityListener implements Listener {
    private final PvPGame plugin;
    private static final double VISIBILITY_DISTANCE = 20.0;
    private int taskId = -1;

    public NametagVisibilityListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Wait a bit for scoreboard to be set up
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setupPlayerNametagTeam(player);
        }, 20L);
    }

    public void startVisibilityTask() {
        // Cancel existing task if running
        stopVisibilityTask();

        // Run task every 10 ticks (0.5 seconds) to update nametag visibility
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            updateAllNametagVisibility();
        }, 0L, 10L).getTaskId();
    }

    public void stopVisibilityTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        // Reset all nametags to visible when task stops
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupPlayerNametagTeam(player);
        }
    }

    private void setupPlayerNametagTeam(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        // Create visible and hidden teams for each player's view
        Team visibleTeam = scoreboard.getTeam("visible_tags");
        Team hiddenTeam = scoreboard.getTeam("hidden_tags");

        if (visibleTeam == null) {
            visibleTeam = scoreboard.registerNewTeam("visible_tags");
            visibleTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }

        if (hiddenTeam == null) {
            hiddenTeam = scoreboard.registerNewTeam("hidden_tags");
            hiddenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
    }

    private void updateAllNametagVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();
            if (scoreboard == null) {
                continue;
            }

            Team visibleTeam = scoreboard.getTeam("visible_tags");
            Team hiddenTeam = scoreboard.getTeam("hidden_tags");

            if (visibleTeam == null || hiddenTeam == null) {
                setupPlayerNametagTeam(viewer);
                continue;
            }

            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(target)) {
                    continue;
                }

                String targetName = target.getName();
                double distance = viewer.getLocation().distance(target.getLocation());

                // Remove from both teams first
                visibleTeam.removeEntry(targetName);
                hiddenTeam.removeEntry(targetName);

                // Add to appropriate team based on distance
                if (distance <= VISIBILITY_DISTANCE) {
                    visibleTeam.addEntry(targetName);
                } else {
                    hiddenTeam.addEntry(targetName);
                }
            }
        }
    }

    public void cleanup() {
        stopVisibilityTask();
    }
}
