package myplg.myplg.data;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;

/**
 * ワールドスワップ方式によるワールド管理
 *
 * 仕組み:
 * - world: ゲームで使用するメインワールド
 * - world_backup: 常時ロードされたバックアップワールド（プレイヤーは入れない）
 * - world_master: 初期バックアップの永続コピー（plugins/myplg/world_master）
 *
 * ゲーム終了時:
 * 1. プレイヤーをlobbyへ移動
 * 2. worldをアンロード → フォルダ削除
 * 3. world_backupをアンロード → worldにリネーム
 * 4. worldをロード（瞬時に復元完了）
 * 5. 非同期でworld_masterからworld_backupを再作成
 */
public class WorldBackupManager {
    private final PvPGame plugin;
    private final File masterFolder;  // 永続マスターバックアップ

    private static final String GAME_WORLD = "world";
    private static final String BACKUP_WORLD = "world_backup";

    private boolean backupReady = false;
    private boolean preparingBackup = false;

    public WorldBackupManager(PvPGame plugin) {
        this.plugin = plugin;
        this.masterFolder = new File(plugin.getDataFolder(), "world_master");
    }

    /**
     * プラグイン起動時の初期化
     * マスターバックアップとworld_backupを準備
     */
    public void initialize() {
        plugin.getLogger().info("===== ワールドスワップシステム初期化 =====");

        // マスターバックアップが存在しない場合は作成
        if (!masterFolder.exists()) {
            plugin.getLogger().info("マスターバックアップが存在しません。初回セットアップを行います。");
            plugin.getLogger().info("※ /save コマンドでマスターバックアップを作成してください。");
            return;
        }

        // world_backupを準備
        prepareBackupWorldSync();

        plugin.getLogger().info("===== ワールドスワップシステム初期化完了 =====");
    }

    /**
     * 現在のワールドをマスターバックアップとして保存
     */
    public boolean saveMasterBackup(World world) {
        plugin.getLogger().info("マスターバックアップを作成中: " + world.getName());

        // ワールドを保存
        world.save();

        File worldFolder = world.getWorldFolder();

        // 古いマスターバックアップを削除
        if (masterFolder.exists()) {
            deleteDirectorySync(masterFolder);
        }
        masterFolder.mkdirs();

        try {
            copyDirectorySync(worldFolder.toPath(), masterFolder.toPath());
            plugin.getLogger().info("マスターバックアップ作成完了");

            // バックアップワールドも準備
            prepareBackupWorldSync();

            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("マスターバックアップ作成失敗: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * マスターバックアップが存在するか
     */
    public boolean hasBackup(String worldName) {
        return masterFolder.exists() && masterFolder.isDirectory();
    }

    /**
     * バックアップワールドが準備完了しているか
     */
    public boolean isBackupReady() {
        return backupReady;
    }

    /**
     * ワールドスワップを実行（ゲーム終了時）
     * これがメインの復元メソッド
     */
    public CompletableFuture<Boolean> swapWorlds() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        plugin.getLogger().info("===== ワールドスワップ開始 =====");

        // バックアップの準備確認
        if (!backupReady) {
            plugin.getLogger().severe("バックアップワールドが準備されていません！");
            future.complete(false);
            return future;
        }

        World gameWorld = Bukkit.getWorld(GAME_WORLD);
        World backupWorld = Bukkit.getWorld(BACKUP_WORLD);

        if (backupWorld == null) {
            plugin.getLogger().severe("バックアップワールドがロードされていません！");
            future.complete(false);
            return future;
        }

        // Step 1: プレイヤーをlobbyへ移動
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            plugin.getLogger().severe("Lobbyワールドが見つかりません");
            future.complete(false);
            return future;
        }

        org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);

        if (gameWorld != null) {
            for (Player player : gameWorld.getPlayers()) {
                player.teleport(lobbySpawn);
            }
        }

        // backupWorldからもプレイヤーを退避（念のため）
        for (Player player : backupWorld.getPlayers()) {
            player.teleport(lobbySpawn);
        }

        plugin.getLogger().info("プレイヤー退避完了");

        // Step 2: 少し待ってからスワップ実行
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                performSwap(future);
            } catch (Exception e) {
                plugin.getLogger().severe("ワールドスワップ中にエラー: " + e.getMessage());
                e.printStackTrace();
                future.complete(false);
            }
        }, 10L); // 0.5秒待機

        return future;
    }

    private void performSwap(CompletableFuture<Boolean> future) {
        World gameWorld = Bukkit.getWorld(GAME_WORLD);
        World backupWorld = Bukkit.getWorld(BACKUP_WORLD);

        // Step 3: ゲームワールドをアンロード
        if (gameWorld != null) {
            plugin.getLogger().info("ゲームワールドをアンロード中...");
            boolean unloaded = Bukkit.unloadWorld(gameWorld, false);
            if (!unloaded) {
                plugin.getLogger().severe("ゲームワールドのアンロードに失敗");
                future.complete(false);
                return;
            }
        }

        // Step 4: バックアップワールドをアンロード
        plugin.getLogger().info("バックアップワールドをアンロード中...");
        boolean backupUnloaded = Bukkit.unloadWorld(backupWorld, false);
        if (!backupUnloaded) {
            plugin.getLogger().severe("バックアップワールドのアンロードに失敗");
            // ゲームワールドを再ロードして復旧
            Bukkit.createWorld(new WorldCreator(GAME_WORLD));
            future.complete(false);
            return;
        }

        backupReady = false;

        // Step 5: フォルダ操作（同期で高速実行）
        File worldFolder = new File(Bukkit.getWorldContainer(), GAME_WORLD);
        File backupFolder = new File(Bukkit.getWorldContainer(), BACKUP_WORLD);

        // 古いゲームワールドフォルダを削除
        plugin.getLogger().info("古いワールドフォルダを削除中...");
        deleteDirectorySync(worldFolder);

        // バックアップフォルダをゲームワールドにリネーム
        plugin.getLogger().info("バックアップをゲームワールドにリネーム中...");
        boolean renamed = backupFolder.renameTo(worldFolder);

        if (!renamed) {
            plugin.getLogger().severe("フォルダリネームに失敗！コピーを試行...");
            try {
                copyDirectorySync(backupFolder.toPath(), worldFolder.toPath());
                deleteDirectorySync(backupFolder);
            } catch (IOException e) {
                plugin.getLogger().severe("コピーにも失敗: " + e.getMessage());
                future.complete(false);
                return;
            }
        }

        // Step 6: 新しいゲームワールドをロード
        plugin.getLogger().info("新しいゲームワールドをロード中...");
        World newGameWorld = Bukkit.createWorld(new WorldCreator(GAME_WORLD));

        if (newGameWorld == null) {
            plugin.getLogger().severe("ワールドのロードに失敗");
            future.complete(false);
            return;
        }

        plugin.getLogger().info("===== ワールドスワップ完了！ =====");

        // Step 7: 非同期で次のバックアップを準備
        prepareBackupWorldAsync();

        future.complete(true);
    }

    /**
     * バックアップワールドを同期で準備（起動時用）
     */
    private void prepareBackupWorldSync() {
        if (!masterFolder.exists()) {
            plugin.getLogger().warning("マスターバックアップが存在しません");
            return;
        }

        plugin.getLogger().info("バックアップワールドを準備中...");

        File backupFolder = new File(Bukkit.getWorldContainer(), BACKUP_WORLD);

        // 既存のバックアップワールドをアンロード
        World existingBackup = Bukkit.getWorld(BACKUP_WORLD);
        if (existingBackup != null) {
            // プレイヤーを退避
            World lobbyWorld = Bukkit.getWorld("lobby");
            if (lobbyWorld != null) {
                org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);
                for (Player player : existingBackup.getPlayers()) {
                    player.teleport(lobbySpawn);
                }
            }
            Bukkit.unloadWorld(existingBackup, false);
        }

        // フォルダを削除して再作成
        if (backupFolder.exists()) {
            deleteDirectorySync(backupFolder);
        }

        try {
            copyDirectorySync(masterFolder.toPath(), backupFolder.toPath());

            // バックアップワールドをロード
            World backup = Bukkit.createWorld(new WorldCreator(BACKUP_WORLD));
            if (backup != null) {
                // バックアップワールドに入れないようにする
                backup.setAutoSave(false);
                backupReady = true;
                plugin.getLogger().info("バックアップワールド準備完了");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("バックアップワールドの準備に失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * バックアップワールドを非同期で準備（ゲーム終了後用）
     */
    public void prepareBackupWorldAsync() {
        if (preparingBackup) {
            plugin.getLogger().info("バックアップ準備が既に進行中です");
            return;
        }

        if (!masterFolder.exists()) {
            plugin.getLogger().warning("マスターバックアップが存在しません");
            return;
        }

        preparingBackup = true;
        plugin.getLogger().info("次のバックアップを非同期で準備開始...");

        File backupFolder = new File(Bukkit.getWorldContainer(), BACKUP_WORLD);

        // 非同期でファイルコピー
        CompletableFuture.runAsync(() -> {
            try {
                // フォルダを削除して再作成
                if (backupFolder.exists()) {
                    deleteDirectorySync(backupFolder);
                }

                copyDirectorySync(masterFolder.toPath(), backupFolder.toPath());

                plugin.getLogger().info("バックアップファイルコピー完了");

                // メインスレッドでワールドをロード
                Bukkit.getScheduler().runTask(plugin, () -> {
                    World backup = Bukkit.createWorld(new WorldCreator(BACKUP_WORLD));
                    if (backup != null) {
                        backup.setAutoSave(false);
                        backupReady = true;
                        preparingBackup = false;
                        plugin.getLogger().info("バックアップワールド準備完了（非同期）");
                    } else {
                        preparingBackup = false;
                        plugin.getLogger().severe("バックアップワールドのロードに失敗");
                    }
                });
            } catch (IOException e) {
                preparingBackup = false;
                plugin.getLogger().severe("バックアップ準備中にエラー: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 旧APIとの互換性のため（restoreWorldRealtimeの代替）
     */
    public boolean restoreWorldRealtime(String worldName) {
        try {
            return swapWorlds().get();
        } catch (Exception e) {
            plugin.getLogger().severe("ワールド復元エラー: " + e.getMessage());
            return false;
        }
    }

    /**
     * 旧APIとの互換性のため（saveWorldの代替）
     */
    public boolean saveWorld(World world) {
        return saveMasterBackup(world);
    }

    /**
     * 旧APIとの互換性のため（restoreWorldの代替）
     */
    public boolean restoreWorld(String worldName) {
        return restoreWorldRealtime(worldName);
    }

    // ===== ユーティリティメソッド =====

    private void copyDirectorySync(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String fileName = dir.getFileName().toString();
                // session.lockとuid.datはスキップ
                if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = destination.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                // session.lockとuid.datはスキップ
                if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = destination.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectorySync(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // session.lockはスキップ（削除できない場合があるため）
                if (file.getName().equals("session.lock")) {
                    continue;
                }
                if (file.isDirectory()) {
                    deleteDirectorySync(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
