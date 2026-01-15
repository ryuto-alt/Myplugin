package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * ENDモードのボーダーを強制的に消去するコマンド
 * /endmodekill - ボーダーを完全にリセット
 */
public class EndModeKillCommand implements CommandExecutor {
    private final PvPGame plugin;

    public EndModeKillCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("pvpgame.admin")) {
            player.sendMessage("§cこのコマンドを使用する権限がありません。");
            return true;
        }

        // 全ワールドのボーダーをリセット
        for (World world : Bukkit.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            border.reset();
            player.sendMessage("§a" + world.getName() + " のボーダーをリセットしました");
        }

        // ENDモードマネージャーもリセット
        if (plugin.getEndModeManager() != null) {
            plugin.getEndModeManager().reset();
        }

        player.sendMessage("§5§l[ENDモード] §a全てのボーダーを強制消去しました！");
        return true;
    }
}
