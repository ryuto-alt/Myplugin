package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import myplg.myplg.listeners.BlockPlaceListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EndCommand implements CommandExecutor {
    private final PvPGame plugin;

    public EndCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("このコマンドはOP権限が必要です。", NamedTextColor.RED));
            return true;
        }

        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage(Component.text("ゲームは開始されていません。", NamedTextColor.RED));
            return true;
        }

        // Check if backup exists
        if (!plugin.getWorldBackupManager().hasBackup("world")) {
            sender.sendMessage(Component.text("マスターバックアップが見つかりません。", NamedTextColor.RED));
            sender.sendMessage(Component.text("先に /save でワールドを保存してください。", NamedTextColor.YELLOW));
            return true;
        }

        // Check if backup world is ready
        if (!plugin.getWorldBackupManager().isBackupReady()) {
            sender.sendMessage(Component.text("バックアップワールドが準備中です。少々お待ちください...", NamedTextColor.YELLOW));
            return true;
        }

        // End game
        plugin.getGameManager().setGameRunning(false);

        // Stop all generators
        plugin.getGeneratorManager().stopAllGenerators();

        // Stop nametag visibility task
        plugin.getNametagVisibilityListener().stopVisibilityTask();

        // Stop scoreboard update task
        plugin.getScoreboardManager().stopUpdateTask();

        // Stop bed destruction timer
        plugin.getBedDestructionTimer().stopTimer();

        // Stop END mode manager
        if (plugin.getEndModeManager() != null) {
            plugin.getEndModeManager().stop();
        }

        // Stop alarm trap task
        plugin.getAlarmTrapManager().stopAlarmTask();

        // Remove team colors from player names
        plugin.getTeamColorManager().removeTeamColors();

        // Clear player-placed blocks tracking
        BlockPlaceListener.clearPlayerPlacedBlocks();

        // Broadcast game end
        Bukkit.broadcast(Component.text("==================", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("ゲームが終了しました！", NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text("==================", NamedTextColor.GOLD));

        // Get lobby world
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            sender.sendMessage(Component.text("Lobbyワールドが見つかりません。", NamedTextColor.RED));
            return true;
        }

        // Clear all players' inventories and teleport to lobby
        org.bukkit.Location lobbySpawn = new org.bukkit.Location(lobbyWorld, -210, 7, 15);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(lobbySpawn);
        }

        // Notify OPs
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(Component.text("ワールドスワップを実行中...", NamedTextColor.YELLOW));
            }
        }

        // Execute world swap (this is now very fast!)
        plugin.getWorldBackupManager().swapWorlds().thenAccept(success -> {
            // This runs on main thread after swap completes
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    // Notify success
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.isOp()) {
                            player.sendMessage(Component.text("ワールドスワップ完了！", NamedTextColor.GREEN));
                            player.sendMessage(Component.text("ゲーム状態を初期化中...", NamedTextColor.YELLOW));
                        }
                    }

                    // Reset game state after short delay
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.resetGameState();

                        // Notify completion
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.isOp()) {
                                    player.sendMessage(Component.text("ゲームの準備ができました！", NamedTextColor.GOLD));
                                    player.sendMessage(Component.text("/start でゲームを開始できます", NamedTextColor.GRAY));
                                }
                            }
                            plugin.getLogger().info("[初期化完了] ゲームの準備ができました");
                        }, 40L); // 2 seconds
                    }, 20L); // 1 second
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.isOp()) {
                            player.sendMessage(Component.text("ワールドスワップに失敗しました。", NamedTextColor.RED));
                            player.sendMessage(Component.text("サーバーログを確認してください。", NamedTextColor.RED));
                        }
                    }
                }
            });
        });

        return true;
    }
}
