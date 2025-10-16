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

        // Teleport all players out of the world
        World lobbyWorld = Bukkit.getWorlds().get(0); // Main world as lobby
        plugin.getLogger().info("プレイヤーを退避中... (" + world.getPlayers().size() + "人)");
        for (Player player : world.getPlayers()) {
            player.teleport(lobbyWorld.getSpawnLocation());
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
