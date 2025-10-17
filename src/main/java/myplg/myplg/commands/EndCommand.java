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

            // Notify only OPs about restoration result
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp()) {
                    if (success) {
                        player.sendMessage(Component.text("ワールド「" + worldName + "」の復元が完了しました！", NamedTextColor.GREEN));
                        player.sendMessage(Component.text("[ゲームの準備ができました]", NamedTextColor.GOLD));
                    } else {
                        player.sendMessage(Component.text("ワールドの復元に失敗しました。", NamedTextColor.RED));
                    }
                }
            }
        }, 60L); // 3 seconds delay

        return true;
    }
}
