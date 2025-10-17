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
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage(Component.text("ゲームは開始されていません。", NamedTextColor.RED));
            return true;
        }

        // Get world to restore
        World world;
        if (args.length > 0) {
            world = Bukkit.getWorld(args[0]);
            if (world == null) {
                sender.sendMessage(Component.text("ワールド「" + args[0] + "」が見つかりません。", NamedTextColor.RED));
                return true;
            }
        } else {
            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                world = Bukkit.getWorlds().get(0);
            }
        }

        // Check if backup exists
        if (!plugin.getWorldBackupManager().hasBackup(world.getName())) {
            sender.sendMessage(Component.text("ワールド「" + world.getName() + "」のバックアップが見つかりません。", NamedTextColor.RED));
            sender.sendMessage(Component.text("先に /save でワールドを保存してください。", NamedTextColor.YELLOW));
            return true;
        }

        // End game
        plugin.getGameManager().setGameRunning(false);

        // Stop all generators
        plugin.getGeneratorManager().stopAllGenerators();

        // Stop nametag visibility task
        plugin.getNametagVisibilityListener().stopVisibilityTask();

        // Clear player-placed blocks tracking
        BlockPlaceListener.clearPlayerPlacedBlocks();

        // Broadcast game end
        Bukkit.broadcast(Component.text("==================", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("ゲームが終了しました！", NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text("ロビーに移動しています...", NamedTextColor.YELLOW));
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

        final String worldName = world.getName();

        // Notify OPs that restoration is starting
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(Component.text("ワールドを復元しています...", NamedTextColor.YELLOW));
            }
        }

        // Restore world in real-time after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean success = plugin.getWorldBackupManager().restoreWorldRealtime(worldName);

            if (success) {
                // Notify OPs that plugin will reload
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOp()) {
                        player.sendMessage(Component.text("ワールド復元完了！プラグインを再読み込みしています...", NamedTextColor.YELLOW));
                    }
                }

                // Wait 2 seconds then reload plugin
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getLogger().info("===== プラグイン再読み込み開始 =====");

                    // Reload plugin using Bukkit's plugin manager
                    org.bukkit.plugin.PluginManager pluginManager = Bukkit.getPluginManager();
                    pluginManager.disablePlugin(plugin);
                    pluginManager.enablePlugin(plugin);

                    plugin.getLogger().info("===== プラグイン再読み込み完了 =====");

                    // Notify OPs
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.isOp()) {
                                player.sendMessage(Component.text("ワールド「" + worldName + "」の復元が完了しました！", NamedTextColor.GREEN));
                                player.sendMessage(Component.text("[ゲームの準備ができました]", NamedTextColor.GOLD));
                                player.sendMessage(Component.text("§7/gameworld でゲームワールドに移動してください", NamedTextColor.GRAY));
                            }
                        }
                    }, 20L); // 1 second after plugin reload
                }, 40L); // 2 seconds wait before reload
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOp()) {
                        player.sendMessage(Component.text("ワールドの復元に失敗しました。", NamedTextColor.RED));
                    }
                }
            }
        }, 60L); // 3 seconds delay

        return true;
    }
}
