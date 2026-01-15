package myplg.myplg;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * デバッグモード中のサーバー情報表示
 * アクションバーにTPS, MSPT, メモリ, Pingを表示
 */
public class DebugInfoDisplay {

    private final PvPGame plugin;
    private BukkitTask displayTask;
    private BukkitTask tpsTask;

    // TPS計算用
    private long lastTickTime;
    private double[] tpsHistory = new double[20]; // 直近20秒分
    private int tpsIndex = 0;
    private double currentTps = 20.0;
    private double currentMspt = 0.0;

    // tick計測用
    private long lastPollTime;
    private int tickCount;

    public DebugInfoDisplay(PvPGame plugin) {
        this.plugin = plugin;
        this.lastTickTime = System.nanoTime();
        this.lastPollTime = System.currentTimeMillis();
        this.tickCount = 0;
    }

    /**
     * 表示を開始
     */
    public void start() {
        if (displayTask != null) {
            return; // 既に実行中
        }

        // TPS計測タスク（毎tick実行）
        tpsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.nanoTime();
            long diff = now - lastTickTime;
            lastTickTime = now;

            // MSPT計算（ナノ秒→ミリ秒）
            currentMspt = diff / 1_000_000.0;

            tickCount++;
        }, 1L, 1L);

        // 表示更新タスク（1秒ごと）
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // TPS計算
            long now = System.currentTimeMillis();
            long elapsed = now - lastPollTime;
            
            if (elapsed > 0) {
                // 実際のTPS = (tickCount / 経過秒数)
                double measuredTps = (tickCount * 1000.0) / elapsed;
                if (measuredTps > 20.0) measuredTps = 20.0; // 上限20
                
                tpsHistory[tpsIndex] = measuredTps;
                tpsIndex = (tpsIndex + 1) % tpsHistory.length;
                
                // 平均TPS計算
                double sum = 0;
                int count = 0;
                for (double tps : tpsHistory) {
                    if (tps > 0) {
                        sum += tps;
                        count++;
                    }
                }
                currentTps = count > 0 ? sum / count : 20.0;
            }
            
            lastPollTime = now;
            tickCount = 0;

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
        if (tpsTask != null) {
            tpsTask.cancel();
            tpsTask = null;
        }
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

    /**
     * TPS値に応じた色を取得
     */
    private String getTpsColor(double tps) {
        if (tps >= 19.0) return "§a"; // 緑: 良好
        if (tps >= 15.0) return "§e"; // 黄: 注意
        return "§c"; // 赤: 問題
    }

    /**
     * MSPT値に応じた色を取得
     */
    private String getMsptColor(double mspt) {
        if (mspt < 30) return "§a";  // 緑: 良好
        if (mspt < 50) return "§e";  // 黄: 注意
        return "§c"; // 赤: 問題（50ms超えるとラグ）
    }

    /**
     * メモリ使用率に応じた色を取得
     */
    private String getMemoryColor(int percent) {
        if (percent < 70) return "§a";  // 緑: 余裕あり
        if (percent < 85) return "§e";  // 黄: やや多い
        return "§c"; // 赤: 危険
    }

    /**
     * Ping値に応じた色を取得
     */
    private String getPingColor(int ping) {
        if (ping < 50) return "§a";   // 緑: 良好
        if (ping < 100) return "§e";  // 黄: 普通
        if (ping < 200) return "§6";  // オレンジ: やや遅い
        return "§c"; // 赤: 遅い
    }

    /**
     * 現在のTPSを取得
     */
    public double getCurrentTps() {
        return currentTps;
    }

    /**
     * 現在のMSPTを取得
     */
    public double getCurrentMspt() {
        return currentMspt;
    }
}
