package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GeneCommand implements CommandExecutor {
    private final PvPGame plugin;

    public GeneCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cこのコマンドはOP権限が必要です。");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        Player player = (Player) sender;

        // Open generator type selection GUI
        plugin.getGUIClickListener().getManagementGUI().openGeneratorTypeSelection(player);

        return true;
    }
}
