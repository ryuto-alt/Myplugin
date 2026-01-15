package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ENDモードマネージャー
 * 全ベッド破壊後3分でENDモード突入
 * エンダードラゴン2体出現、10秒ごとにワールドボーダー縮小
 */
public class EndModeManager {
    private final PvPGame plugin;

    // ENDモード設定
    private static final int END_MODE_DELAY_SECONDS = 3 * 60; // 3分
    private static final int BORDER_SHRINK_INTERVAL_SECONDS = 10; // 10秒ごと
    private static final double BORDER_SHRINK_AMOUNT = 5.0; // 各回で縮小する量
    private static final double MIN_BORDER_SIZE = 10.0; // 最小ボーダーサイズ
    private static final double END_MODE_INITIAL_BORDER_SIZE = 234.0; // ENDモード開始時のボーダーサイズ (234x234)
    private static final double BORDER_CENTER_X = 0.0; // ボーダー中心X座標
    private static final double BORDER_CENTER_Z = 0.0; // ボーダー中心Z座標

    private boolean allBedsDestroyed = false;
    private boolean endModeActive = false;
    private int countdownSeconds = END_MODE_DELAY_SECONDS;
    private BukkitTask countdownTask = null;
    private BukkitTask borderShrinkTask = null;
    private List<UUID> spawnedDragons = new ArrayList<>();
    private double initialBorderSize = 0;
    private Location borderCenter = null;

    public EndModeManager(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * 全ベッドが破壊されたかチェックし、ENDモードカウントダウンを開始
     */
    public void checkAllBedsDestroyed() {
        if (allBedsDestroyed || endModeActive) {
            return; // 既に処理済み
        }

        // 全チームのベッド状態を確認
        boolean anyBedAlive = false;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            if (plugin.getScoreboardManager().isBedAlive(team.getName())) {
                anyBedAlive = true;
                break;
            }
        }

        if (!anyBedAlive) {
            allBedsDestroyed = true;
            startEndModeCountdown();
        }
    }

    /**
     * ENDモードカウントダウンを開始
     */
    private void startEndModeCountdown() {
        plugin.getLogger().info("全ベッドが破壊されました！ENDモードまで" + END_MODE_DELAY_SECONDS + "秒...");

        Bukkit.broadcastMessage("§4§l═══════════════════════════");
        Bukkit.broadcastMessage("§c§l⚠ 全てのベッドが破壊されました！ ⚠");
        Bukkit.broadcastMessage("§e§l3分後にENDモードが発動します！");
        Bukkit.broadcastMessage("§4§l═══════════════════════════");

        // 全プレイヤーに警告音
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }

        countdownSeconds = END_MODE_DELAY_SECONDS;

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            countdownSeconds--;

            // 警告アナウンス
            if (countdownSeconds == 2 * 60) { // 2分
                Bukkit.broadcastMessage("§c§l[ENDモード] 発動まで残り2分！");
                playWarningSound();
            } else if (countdownSeconds == 60) { // 1分
                Bukkit.broadcastMessage("§4§l[ENDモード] 発動まで残り1分！");
                playWarningSound();
            } else if (countdownSeconds == 30) { // 30秒
                Bukkit.broadcastMessage("§4§l[ENDモード] 発動まで残り30秒！");
                playWarningSound();
            } else if (countdownSeconds <= 10 && countdownSeconds > 0) {
                Bukkit.broadcastMessage("§4§l" + countdownSeconds);
                playCountdownSound();
            } else if (countdownSeconds == 0) {
                // ENDモード発動
                activateEndMode();
                countdownTask.cancel();
                countdownTask = null;
            }
        }, 0L, 20L); // 毎秒実行
    }

    /**
     * ENDモードを発動
     */
    private void activateEndMode() {
        endModeActive = true;
        plugin.getLogger().info("ENDモード発動！");

        // 派手なアナウンス
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§5§l╔═══════════════════════════════╗");
        Bukkit.broadcastMessage("§5§l║   §d§l⚡ END MODE ACTIVATED ⚡   §5§l║");
        Bukkit.broadcastMessage("§5§l╚═══════════════════════════════╝");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§lエンダードラゴンが出現しました！");
        Bukkit.broadcastMessage("§c§lステージが崩壊し始めます！");
        Bukkit.broadcastMessage("");

        // 全プレイヤーにタイトル表示
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§5§lEND MODE", "§cステージが崩壊し始めます！", 20, 60, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }

        // エンダードラゴン2体を出現させる
        spawnDragons();

        // ワールドボーダー縮小を開始
        startBorderShrinking();
    }

    /**
     * エンダードラゴン2体を出現させる（演出用、攻撃しない）
     */
    private void spawnDragons() {
        World gameWorld = Bukkit.getWorld("world");
        if (gameWorld == null) {
            plugin.getLogger().warning("ゲームワールドが見つかりません！");
            return;
        }

        // ボーダーの中心を取得
        WorldBorder border = gameWorld.getWorldBorder();
        Location center = border.getCenter();

        // ドラゴン1: 中心から少し離れた位置に
        Location dragon1Loc = center.clone().add(30, 60, 30);
        EnderDragon dragon1 = (EnderDragon) gameWorld.spawnEntity(dragon1Loc, EntityType.ENDER_DRAGON);
        configureDragon(dragon1, "§5§lENDドラゴン I");
        spawnedDragons.add(dragon1.getUniqueId());

        // ドラゴン2: 反対側に
        Location dragon2Loc = center.clone().add(-30, 60, -30);
        EnderDragon dragon2 = (EnderDragon) gameWorld.spawnEntity(dragon2Loc, EntityType.ENDER_DRAGON);
        configureDragon(dragon2, "§5§lENDドラゴン II");
        spawnedDragons.add(dragon2.getUniqueId());

        plugin.getLogger().info("エンダードラゴン2体をスポーンしました");

        // ドラゴンの行動を制御するタスク（プレイヤーを攻撃しないように）
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID dragonUUID : spawnedDragons) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(dragonUUID);
                if (entity instanceof EnderDragon) {
                    EnderDragon dragon = (EnderDragon) entity;
                    // ドラゴンのフェーズを変更してプレイヤーを攻撃しないようにする
                    dragon.setPhase(EnderDragon.Phase.CIRCLING);
                }
            }
        }, 0L, 100L); // 5秒ごとにフェーズをリセット
    }

    /**
     * ドラゴンの設定
     */
    private void configureDragon(EnderDragon dragon, String name) {
        dragon.setCustomName(name);
        dragon.setCustomNameVisible(true);
        dragon.setPhase(EnderDragon.Phase.CIRCLING); // 旋回モード（攻撃しない）

        // AIを無効化してプレイヤーを攻撃しないようにする
        dragon.setAI(false);

        // 一定時間後にAIを有効にして旋回させる
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!dragon.isDead()) {
                dragon.setAI(true);
                dragon.setPhase(EnderDragon.Phase.CIRCLING);
            }
        }, 40L); // 2秒後
    }

    /**
     * ワールドボーダー縮小を開始
     */
    private void startBorderShrinking() {
        World gameWorld = Bukkit.getWorld("world");
        if (gameWorld == null) {
            plugin.getLogger().warning("ゲームワールドが見つかりません！");
            return;
        }

        WorldBorder border = gameWorld.getWorldBorder();

        // 元のボーダー設定を保存（リセット用）
        initialBorderSize = border.getSize();
        borderCenter = border.getCenter();

        // ENDモード用にボーダーを設定
        border.setCenter(BORDER_CENTER_X, BORDER_CENTER_Z);
        border.setSize(END_MODE_INITIAL_BORDER_SIZE);
        border.setDamageAmount(1.0); // ボーダー外でのダメージ
        border.setDamageBuffer(5.0); // ダメージ開始までの距離
        border.setWarningDistance(10); // 警告距離

        plugin.getLogger().info("ワールドボーダー設定: 中心=(" + BORDER_CENTER_X + ", " + BORDER_CENTER_Z +
            "), サイズ=" + END_MODE_INITIAL_BORDER_SIZE);

        borderShrinkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            WorldBorder currentBorder = gameWorld.getWorldBorder();
            double currentSize = currentBorder.getSize();

            if (currentSize <= MIN_BORDER_SIZE) {
                plugin.getLogger().info("ワールドボーダーが最小サイズに到達しました");
                // 最小サイズに達したら停止しない（ゲームは続行）
                return;
            }

            // ボーダーを縮小
            double newSize = Math.max(currentSize - BORDER_SHRINK_AMOUNT, MIN_BORDER_SIZE);
            currentBorder.setSize(newSize, BORDER_SHRINK_INTERVAL_SECONDS); // 10秒かけて縮小

            // 警告音
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }

            // パーティクル効果（ボーダー付近）
            spawnBorderParticles(gameWorld, currentBorder);

        }, 0L, BORDER_SHRINK_INTERVAL_SECONDS * 20L); // 10秒ごと
    }

    /**
     * ボーダー付近にパーティクル効果を表示
     */
    private void spawnBorderParticles(World world, WorldBorder border) {
        Location center = border.getCenter();
        double size = border.getSize() / 2;

        // ボーダーの4辺にパーティクルを表示
        for (int i = 0; i < 50; i++) {
            double offset = (Math.random() - 0.5) * size * 2;
            double y = 64 + Math.random() * 20; // Y座標をランダムに

            // 北側
            world.spawnParticle(org.bukkit.Particle.DRAGON_BREATH,
                center.getX() + offset, y, center.getZ() - size, 1, 0, 0, 0, 0);
            // 南側
            world.spawnParticle(org.bukkit.Particle.DRAGON_BREATH,
                center.getX() + offset, y, center.getZ() + size, 1, 0, 0, 0, 0);
            // 東側
            world.spawnParticle(org.bukkit.Particle.DRAGON_BREATH,
                center.getX() + size, y, center.getZ() + offset, 1, 0, 0, 0, 0);
            // 西側
            world.spawnParticle(org.bukkit.Particle.DRAGON_BREATH,
                center.getX() - size, y, center.getZ() + offset, 1, 0, 0, 0, 0);
        }
    }

    /**
     * 警告音を再生
     */
    private void playWarningSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }
    }

    /**
     * カウントダウン音を再生
     */
    private void playCountdownSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }
    }

    /**
     * ENDモードが有効かどうか
     */
    public boolean isEndModeActive() {
        return endModeActive;
    }

    /**
     * リセット（ゲーム終了時）
     */
    public void reset() {
        // タスクをキャンセル
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (borderShrinkTask != null) {
            borderShrinkTask.cancel();
            borderShrinkTask = null;
        }

        // ドラゴンを削除
        for (UUID dragonUUID : spawnedDragons) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(dragonUUID);
            if (entity != null) {
                entity.remove();
            }
        }
        spawnedDragons.clear();

        // ワールドボーダーをリセット
        World gameWorld = Bukkit.getWorld("world");
        if (gameWorld != null && initialBorderSize > 0) {
            WorldBorder border = gameWorld.getWorldBorder();
            border.setSize(initialBorderSize);
            if (borderCenter != null) {
                border.setCenter(borderCenter);
            }
        }

        // 状態をリセット
        allBedsDestroyed = false;
        endModeActive = false;
        countdownSeconds = END_MODE_DELAY_SECONDS;
        initialBorderSize = 0;
        borderCenter = null;

        plugin.getLogger().info("ENDモードマネージャーをリセットしました");
    }

    /**
     * 停止（プラグイン無効化時）
     */
    public void stop() {
        reset();
    }
}
