package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameWorldCommand implements CommandExecutor {
    private final PvPGame plugin;

    public GameWorldCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cこのコマンドはOP権限が必要です。");
            return true;
        }

        // Get or load world
        World gameWorld = Bukkit.getWorld("world");
        if (gameWorld == null) {
            plugin.getLogger().info("ワールド「world」が見つかりません。ロード中...");
            try {
                gameWorld = Bukkit.createWorld(new org.bukkit.WorldCreator("world"));
                if (gameWorld == null) {
                    sender.sendMessage(Component.text("ゲームワールド「world」のロードに失敗しました。", NamedTextColor.RED));
                    return true;
                }
                plugin.getLogger().info("ワールド「world」のロードに成功しました。");
            } catch (Exception e) {
                sender.sendMessage(Component.text("ゲームワールド「world」のロード中にエラーが発生しました。", NamedTextColor.RED));
                plugin.getLogger().severe("ワールドロードエラー: " + e.getMessage());
                e.printStackTrace();
                return true;
            }
        }

        // Get spawn location from world spawn
        Location spawnLocation = gameWorld.getSpawnLocation();

        // Teleport all players to game world and set to adventure mode
        int teleportedCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawnLocation);

            // Set to adventure mode if game is not running
            if (!plugin.getGameManager().isGameRunning()) {
                player.setGameMode(GameMode.ADVENTURE);
            }

            teleportedCount++;
        }

        // Broadcast success message
        Bukkit.broadcast(Component.text("==================", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("全プレイヤーをゲームワールドに移動しました！", NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text("移動人数: " + teleportedCount + "人", NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("[ゲームの準備ができました]", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("==================", NamedTextColor.GOLD));

        plugin.getLogger().info("Teleported " + teleportedCount + " players to game world by " + sender.getName());

        return true;
    }
}
