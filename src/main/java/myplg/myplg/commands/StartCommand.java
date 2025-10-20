package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StartCommand implements CommandExecutor {
    private final PvPGame plugin;

    public StartCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cこのコマンドはOP権限が必要です。");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("ゲームは既に進行中です。");
            return true;
        }

        if (plugin.getGameManager().getTeams().isEmpty()) {
            sender.sendMessage("チームが設定されていません。先に /setbed でチームを設定してください。");
            return true;
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (onlinePlayers.isEmpty()) {
            sender.sendMessage("参加可能なプレイヤーがいません。");
            return true;
        }

        // ゲーム設定をリセット
        plugin.getGameSetupManager().reset();

        // ゲームモード選択GUIを開く
        plugin.getGameModeSelector().openGameModeSelector(player);

        return true;
    }
}
