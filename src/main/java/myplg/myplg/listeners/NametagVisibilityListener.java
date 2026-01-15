package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NametagVisibilityListener implements Listener {
    private final PvPGame plugin;
    private static final double VISIBILITY_DISTANCE = 20.0;
    private static final double VISIBILITY_DISTANCE_SQUARED = VISIBILITY_DISTANCE * VISIBILITY_DISTANCE;
    private int taskId = -1;

    // 前回の可視状態をキャッシュ（変更があった時のみ更新）
    private final java.util.Map<String, java.util.Set<String>> lastVisibleState = new java.util.HashMap<>();

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

        // キャッシュをクリア
        lastVisibleState.clear();

        // Run task every 40 ticks (2 seconds) - 頻度を下げて負荷軽減
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            updateAllNametagVisibility();
        }, 0L, 40L).getTaskId();
    }

    public void stopVisibilityTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        lastVisibleState.clear();

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
        // プレイヤーリストを一度だけ取得
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        int playerCount = players.length;

        for (int i = 0; i < playerCount; i++) {
            Player viewer = players[i];
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

            String viewerName = viewer.getName();
            java.util.Set<String> currentVisible = lastVisibleState.computeIfAbsent(viewerName, k -> new java.util.HashSet<>());

            // viewerのワールドと位置を一度だけ取得
            org.bukkit.World viewerWorld = viewer.getWorld();
            org.bukkit.Location viewerLoc = viewer.getLocation();

            for (int j = 0; j < playerCount; j++) {
                if (i == j) continue;

                Player target = players[j];
                String targetName = target.getName();

                // 別ワールドなら非表示
                if (!target.getWorld().equals(viewerWorld)) {
                    if (currentVisible.remove(targetName)) {
                        visibleTeam.removeEntry(targetName);
                        hiddenTeam.addEntry(targetName);
                    }
                    continue;
                }

                // 距離の2乗で比較（sqrt回避）
                double distanceSquared = viewerLoc.distanceSquared(target.getLocation());
                boolean shouldBeVisible = distanceSquared <= VISIBILITY_DISTANCE_SQUARED;
                boolean wasVisible = currentVisible.contains(targetName);

                // 状態が変わった時のみ更新
                if (shouldBeVisible != wasVisible) {
                    if (shouldBeVisible) {
                        hiddenTeam.removeEntry(targetName);
                        visibleTeam.addEntry(targetName);
                        currentVisible.add(targetName);
                    } else {
                        visibleTeam.removeEntry(targetName);
                        hiddenTeam.addEntry(targetName);
                        currentVisible.remove(targetName);
                    }
                }
            }
        }
    }

    public void cleanup() {
        stopVisibilityTask();
    }
}
