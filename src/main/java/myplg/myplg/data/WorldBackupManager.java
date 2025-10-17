package myplg.myplg.data;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class WorldBackupManager {
    private final PvPGame plugin;
    private final File backupFolder;

    public WorldBackupManager(PvPGame plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "world_backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    public boolean saveWorld(World world) {
        plugin.getLogger().info("ワールドのバックアップを開始: " + world.getName());

        // Save world data first (must be on main thread)
        world.save();

        // Get world folder
        File worldFolder = world.getWorldFolder();
        File backupDestination = new File(backupFolder, world.getName());

        plugin.getLogger().info("ワールドファイルをコピー中...");

        // Delete old backup if exists
        if (backupDestination.exists()) {
            deleteDirectory(backupDestination);
        }

        try {
            // Copy world folder to backup
            copyDirectory(worldFolder.toPath(), backupDestination.toPath());
            plugin.getLogger().info("ワールドのバックアップ完了: " + world.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("ワールドのバックアップに失敗: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean restoreWorld(String worldName) {
        plugin.getLogger().info("===== ワールド復元処理開始 =====");
        plugin.getLogger().info("復元対象ワールド: " + worldName);

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("ワールドが見つかりません: " + worldName);
            return false;
        }

        File backupSource = new File(backupFolder, worldName);
        plugin.getLogger().info("バックアップ元: " + backupSource.getAbsolutePath());
        if (!backupSource.exists()) {
            plugin.getLogger().warning("バックアップが見つかりません: " + worldName);
            return false;
        }

        plugin.getLogger().info("ワールドの復元を開始: " + worldName);

        // Teleport all players out of the world to lobby
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            plugin.getLogger().severe("Lobbyワールドが見つかりません");
            return false;
        }

        org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);
        plugin.getLogger().info("プレイヤーを退避中... (" + world.getPlayers().size() + "人)");
        for (Player player : world.getPlayers()) {
            player.teleport(lobbySpawn);
            plugin.getLogger().info("  - " + player.getName() + " を退避");
        }

        // Unload world
        plugin.getLogger().info("ワールドをアンロード中...");
        boolean unloaded = Bukkit.unloadWorld(world, false);
        plugin.getLogger().info("アンロード結果: " + (unloaded ? "成功" : "失敗"));

        // Delete current world folder
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        plugin.getLogger().info("既存ワールドフォルダを削除: " + worldFolder.getAbsolutePath());
        deleteDirectory(worldFolder);
        plugin.getLogger().info("削除完了");

        try {
            // Copy backup to world folder
            plugin.getLogger().info("バックアップファイルをコピー中...");
            copyDirectory(backupSource.toPath(), worldFolder.toPath());
            plugin.getLogger().info("コピー完了");

            // Reload world
            plugin.getLogger().info("ワールドを再ロード中...");
            World restoredWorld = Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
            plugin.getLogger().info("再ロード完了: " + (restoredWorld != null ? "成功" : "失敗"));

            plugin.getLogger().info("===== ワールドの復元完了 =====");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("ワールドの復元に失敗: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasBackup(String worldName) {
        File backupSource = new File(backupFolder, worldName);
        return backupSource.exists();
    }

    public boolean restoreWorldRealtime(String worldName) {
        try {
            plugin.getLogger().info("===== リアルタイムワールド復元処理開始 =====");
            plugin.getLogger().info("復元対象ワールド: " + worldName);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("ワールドが見つかりません: " + worldName);
                return false;
            }

            File backupSource = new File(backupFolder, worldName);
            plugin.getLogger().info("バックアップ元: " + backupSource.getAbsolutePath());
            if (!backupSource.exists()) {
                plugin.getLogger().warning("バックアップが見つかりません: " + worldName);
                return false;
            }

            plugin.getLogger().info("リアルタイムワールドの復元を開始: " + worldName);

            // Teleport all players out of the world to lobby
            plugin.getLogger().info("Step 1: Lobby取得");
            World lobbyWorld = Bukkit.getWorld("lobby");
            if (lobbyWorld == null) {
                plugin.getLogger().severe("Lobbyワールドが見つかりません");
                return false;
            }
            plugin.getLogger().info("Lobby取得成功");

            org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);
            plugin.getLogger().info("Step 2: プレイヤーを退避中... (" + world.getPlayers().size() + "人)");

            // Copy player list to avoid concurrent modification
            java.util.List<Player> playersToMove = new java.util.ArrayList<>(world.getPlayers());
            for (Player player : playersToMove) {
                plugin.getLogger().info("  - テレポート開始: " + player.getName());
                player.teleport(lobbySpawn);
                plugin.getLogger().info("  - テレポート完了: " + player.getName());
            }
            plugin.getLogger().info("プレイヤー退避完了");

            // Save world before unloading
            plugin.getLogger().info("Step 3: ワールドを保存中...");
            world.save();
            plugin.getLogger().info("ワールド保存完了");

            // Force unload all chunks
            plugin.getLogger().info("Step 4: チャンクをアンロード中...");
            int chunkCount = world.getLoadedChunks().length;
            plugin.getLogger().info("ロード済みチャンク数: " + chunkCount);

            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                chunk.unload(false);
            }
            plugin.getLogger().info("チャンクアンロード完了");

            // Wait a bit for chunks to unload
            plugin.getLogger().info("Step 5: 待機中...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                plugin.getLogger().severe("待機中に例外が発生: " + e.getMessage());
                e.printStackTrace();
            }
            plugin.getLogger().info("待機完了");

            // Unload world
            plugin.getLogger().info("Step 6: ワールドをアンロード中...");
            try {
                plugin.getLogger().info("アンロード前の状態:");
                int playerCount = world.getPlayers().size();
                plugin.getLogger().info("  - プレイヤー数: " + playerCount);
                int remainingChunks = world.getLoadedChunks().length;
                plugin.getLogger().info("  - チャンク数: " + remainingChunks);
            } catch (Exception e) {
                plugin.getLogger().severe("状態取得中にエラー: " + e.getMessage());
                e.printStackTrace();
            }

            plugin.getLogger().info("Bukkit.unloadWorld() 呼び出し中...");
            boolean unloaded = Bukkit.unloadWorld(world, false);
            plugin.getLogger().info("アンロード結果: " + (unloaded ? "成功" : "失敗"));

            if (!unloaded) {
                plugin.getLogger().severe("ワールドのアンロードに失敗しました");
                try {
                    plugin.getLogger().severe("残りのプレイヤー数: " + world.getPlayers().size());
                    plugin.getLogger().severe("ロード済みチャンク数: " + world.getLoadedChunks().length);
                } catch (Exception e) {
                    plugin.getLogger().severe("エラー情報取得失敗: " + e.getMessage());
                }
                return false;
            }
            plugin.getLogger().info("ワールドアンロード成功");

            // Delete current world folder
            plugin.getLogger().info("Step 7: 既存ワールドフォルダを削除");
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            plugin.getLogger().info("削除対象: " + worldFolder.getAbsolutePath());
            deleteDirectory(worldFolder);
            plugin.getLogger().info("削除完了");

            // Copy backup to world folder
            plugin.getLogger().info("Step 8: バックアップファイルをコピー中...");
            copyDirectory(backupSource.toPath(), worldFolder.toPath());
            plugin.getLogger().info("コピー完了");

            // Reload world
            plugin.getLogger().info("Step 9: ワールドを再ロード中...");
            World restoredWorld = Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
            plugin.getLogger().info("再ロード完了: " + (restoredWorld != null ? "成功" : "失敗"));

            plugin.getLogger().info("===== リアルタイムワールドの復元完了 =====");
            return restoredWorld != null;

        } catch (IOException e) {
            plugin.getLogger().severe("IOExceptionが発生: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("予期しない例外が発生: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Skip session.lock files
                if (dir.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = destination.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Skip session.lock and uid.dat files
                String fileName = file.getFileName().toString();
                if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = destination.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
