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
 * ENDモードのボーダーを可視化するコマンド
 * /endmode - ボーダーを表示/非表示切り替え
 */
public class EndModeCommand implements CommandExecutor {
    private final PvPGame plugin;
    private boolean borderVisible = false;
    private double originalSize = 0;
    private double originalCenterX = 0;
    private double originalCenterZ = 0;

    // ENDモード設定（EndModeManagerと同じ値）
    private static final double END_MODE_BORDER_SIZE = 234.0;
    private static final double BORDER_CENTER_X = 0.0;
    private static final double BORDER_CENTER_Z = 0.0;

    public EndModeCommand(PvPGame plugin) {
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

        World world = Bukkit.getWorld("world");
        if (world == null) {
            player.sendMessage("§cゲームワールドが見つかりません。");
            return true;
        }

        WorldBorder border = world.getWorldBorder();

        if (!borderVisible) {
            // ボーダーを表示
            originalSize = border.getSize();
            originalCenterX = border.getCenter().getX();
            originalCenterZ = border.getCenter().getZ();

            border.setCenter(BORDER_CENTER_X, BORDER_CENTER_Z);
            border.setSize(END_MODE_BORDER_SIZE);
            border.setWarningDistance(5);
            border.setDamageAmount(0); // プレビュー中はダメージなし

            borderVisible = true;
            player.sendMessage("§5§l[ENDモード] §aボーダーを表示しました");
            player.sendMessage("§7中心: (0, 0) / サイズ: 234x234");
            player.sendMessage("§7もう一度 /endmode で非表示にできます");
        } else {
            // ボーダーを非表示（元に戻す）
            border.setCenter(originalCenterX, originalCenterZ);
            border.setSize(originalSize > 0 ? originalSize : 60000000);
            border.setWarningDistance(5);

            borderVisible = false;
            player.sendMessage("§5§l[ENDモード] §cボーダーを非表示にしました");
        }

        return true;
    }

    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void reset() {
        if (borderVisible) {
            World world = Bukkit.getWorld("world");
            if (world != null) {
                WorldBorder border = world.getWorldBorder();
                border.setCenter(originalCenterX, originalCenterZ);
                border.setSize(originalSize > 0 ? originalSize : 60000000);
            }
            borderVisible = false;
        }
    }
}
