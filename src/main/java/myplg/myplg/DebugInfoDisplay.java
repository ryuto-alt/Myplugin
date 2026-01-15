package myplg.myplg;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class DebugInfoDisplay {

    private final PvPGame plugin;
    private BukkitTask displayTask;

    // TPS計算用（システム時間ベース - 毎tick実行不要）
    private long lastPollTime;
    private long lastTickCount;
    private double currentTps = 20.0;
    private double currentMspt = 50.0;

    public DebugInfoDisplay(PvPGame plugin) {
        this.plugin = plugin;
        this.lastPollTime = System.currentTimeMillis();
        this.lastTickCount = 0;
    }

    /**
     * 表示を開始
     */
    public void start() {
        if (displayTask != null) {
            return; // 既に実行中
        }

        lastPollTime = System.currentTimeMillis();
        lastTickCount = Bukkit.getCurrentTick();

        // 表示更新タスク（1秒ごと）- TPS計算もここで行う
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // TPS計算（システム時間ベース）
            long now = System.currentTimeMillis();
            long currentTickCount = Bukkit.getCurrentTick();
            long elapsed = now - lastPollTime;
            long ticksElapsed = currentTickCount - lastTickCount;

            if (elapsed > 0 && ticksElapsed > 0) {
                // 実際のTPS = (経過tick / 経過秒数)
                double measuredTps = (ticksElapsed * 1000.0) / elapsed;
                if (measuredTps > 20.0) measuredTps = 20.0;
                currentTps = measuredTps;

                // MSPT = 経過時間 / 経過tick
                currentMspt = (double) elapsed / ticksElapsed;
            }

            lastPollTime = now;
            lastTickCount = currentTickCount;

            // デバッグモード中の全プレイヤーに表示
            if (plugin.getGameManager().isDebugMode()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendDebugInfo(player);
                }
            }
        }, 20L, 20L); // 1秒ごと

        plugin.getLogger().info("[DebugInfoDisplay] 開始しました");
    }

    /**
     * 表示を停止
     */
    public void stop() {
        if (displayTask != null) {
            displayTask.cancel();
            displayTask = null;
        }

        // アクションバーをクリア
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(Component.empty());
        }

        plugin.getLogger().info("[DebugInfoDisplay] 停止しました");
    }

    /**
     * プレイヤーにデバッグ情報を送信
     */
    private void sendDebugInfo(Player player) {
        StringBuilder sb = new StringBuilder();

        // TPS（色分け: 緑=良好, 黄=注意, 赤=問題）
        String tpsColor = getTpsColor(currentTps);
        sb.append(tpsColor).append("TPS: ").append(String.format("%.1f", currentTps));

        sb.append(" §7| ");

        // MSPT（色分け: 緑=<30ms, 黄=30-50ms, 赤=>50ms）
        String msptColor = getMsptColor(currentMspt);
        sb.append(msptColor).append("MSPT: ").append(String.format("%.1f", currentMspt)).append("ms");

        sb.append(" §7| ");

        // メモリ使用量
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMB = heapUsage.getUsed() / (1024 * 1024);
        long maxMB = heapUsage.getMax() / (1024 * 1024);
        int memPercent = (int) ((usedMB * 100) / maxMB);
        String memColor = getMemoryColor(memPercent);
        sb.append(memColor).append("RAM: ").append(usedMB).append("/").append(maxMB).append("MB");

        sb.append(" §7| ");

        // プレイヤーPing
        int ping = player.getPing();
        String pingColor = getPingColor(ping);
        sb.append(pingColor).append("Ping: ").append(ping).append("ms");

        // アクションバーに送信
        player.sendActionBar(Component.text(sb.toString()));
    }

    private String getTpsColor(double tps) {
        if (tps >= 19.0) return "§a";
        if (tps >= 15.0) return "§e";
        return "§c";
    }

    private String getMsptColor(double mspt) {
        if (mspt < 30) return "§a";
        if (mspt < 50) return "§e";
        return "§c";
    }

    private String getMemoryColor(int percent) {
        if (percent < 70) return "§a";
        if (percent < 85) return "§e";
        return "§c";
    }

    private String getPingColor(int ping) {
        if (ping < 50) return "§a";
        if (ping < 100) return "§e";
        if (ping < 200) return "§6";
        return "§c";
    }

    public double getCurrentTps() {
        return currentTps;
    }

    public double getCurrentMspt() {
        return currentMspt;
    }
}
