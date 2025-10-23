package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to manually reset game state
 * Backup command in case automatic reset fails
 */
public class GameReloadCommand implements CommandExecutor {
    private final PvPGame plugin;

    public GameReloadCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("このコマンドはOP権限が必要です。", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("========================================", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("[手動初期化] ゲーム状態をリセットします...", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("========================================", NamedTextColor.GOLD));

        plugin.getLogger().info("§6========================================");
        plugin.getLogger().info("§6[手動初期化] /gamereload コマンドが実行されました");
        plugin.getLogger().info("§6実行者: " + sender.getName());
        plugin.getLogger().info("§6========================================");

        // Reset game state
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.resetGameState();

            // Notify all OPs
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isOp()) {
                        player.sendMessage(Component.text("[ゲームの準備ができました]", NamedTextColor.GOLD));
                        player.sendMessage(Component.text("§7/start でゲームを開始できます", NamedTextColor.GRAY));
                    }
                }
                plugin.getLogger().info("§a[初期化完了] ゲームの準備ができました");
            }, 40L); // 2 seconds for initialization
        }, 20L); // 1 second delay

        return true;
    }
}
