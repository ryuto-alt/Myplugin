package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetBedCommand implements CommandExecutor {
    private final PvPGame plugin;
    private final Map<UUID, Boolean> waitingForBedClick;

    public SetBedCommand(PvPGame plugin) {
        this.plugin = plugin;
        this.waitingForBedClick = new HashMap<>();
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

        if (args.length != 1) {
            player.sendMessage("使用方法: /setbed <チーム名>");
            return true;
        }

        String teamName = args[0];

        // Check if team already exists
        if (plugin.getGameManager().getTeam(teamName) != null) {
            player.sendMessage("チーム「" + teamName + "」は既に存在します。");
            return true;
        }

        waitingForBedClick.put(player.getUniqueId(), true);
        plugin.getBedClickListener().setWaitingPlayer(player.getUniqueId(), teamName);
        player.sendMessage("チーム「" + teamName + "」のベッドを右クリックしてください。");

        return true;
    }

    public boolean isWaitingForBedClick(UUID playerUUID) {
        return waitingForBedClick.getOrDefault(playerUUID, false);
    }

    public void removeWaitingPlayer(UUID playerUUID) {
        waitingForBedClick.remove(playerUUID);
    }
}
