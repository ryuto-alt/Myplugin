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

public class LobbyCommand implements CommandExecutor {
    private final PvPGame plugin;

    public LobbyCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // プレイヤー以外は実行不可
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行できます。", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        // ゲーム中は無効
        if (plugin.getGameManager().isGameRunning()) {
            player.sendMessage(Component.text("ゲーム中は使用できません。", NamedTextColor.RED));
            return true;
        }

        // ゲームワールドにいない場合は実行不可
        if (!player.getWorld().getName().equalsIgnoreCase("world")) {
            player.sendMessage(Component.text("このコマンドはゲームワールドでのみ使用できます。", NamedTextColor.RED));
            return true;
        }

        // ロビーワールドを取得
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            player.sendMessage(Component.text("ロビーワールドが見つかりません。", NamedTextColor.RED));
            plugin.getLogger().warning("ロビーワールド「lobby」が見つかりません。");
            return true;
        }

        // スポーン地点を取得 (-210, 7, 15)
        Location lobbySpawn = new Location(lobbyWorld, -210, 7, 15);

        // プレイヤーをテレポート
        player.teleport(lobbySpawn);
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(Component.text("ロビーに戻りました。", NamedTextColor.GREEN));

        plugin.getLogger().info(player.getName() + " がロビーに戻りました。");

        return true;
    }
}
