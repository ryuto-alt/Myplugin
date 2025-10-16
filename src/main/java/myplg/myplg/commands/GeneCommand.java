package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        Player player = (Player) sender;

        // Check if player has a selection in progress
        if (plugin.getGUIClickListener().hasGeneratorSelection(player.getUniqueId())) {
            plugin.getGUIClickListener().completeGeneratorSelection(player);
        } else {
            // Open generator type selection GUI
            plugin.getGUIClickListener().getManagementGUI().openGeneratorTypeSelection(player);
        }

        return true;
    }
}
