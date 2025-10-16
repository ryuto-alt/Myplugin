package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopResetCommand implements CommandExecutor {
    private final PvPGame plugin;

    public ShopResetCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("§cこのコマンドを実行する権限がありません。");
            return true;
        }

        // Reset all players' tool upgrades
        plugin.getToolUpgradeManager().resetAll();

        player.sendMessage("§a§l全プレイヤーのショップツールアップグレード状態をリセットしました！");
        plugin.getLogger().info(player.getName() + " がショップ状態をリセットしました。");

        return true;
    }
}
