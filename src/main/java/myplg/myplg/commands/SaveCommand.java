package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SaveCommand implements CommandExecutor {
    private final PvPGame plugin;

    public SaveCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        Player player = (Player) sender;

        // Get world to save
        World world;
        if (args.length > 0) {
            world = Bukkit.getWorld(args[0]);
            if (world == null) {
                player.sendMessage(Component.text("ワールド「" + args[0] + "」が見つかりません。", NamedTextColor.RED));
                return true;
            }
        } else {
            world = player.getWorld();
        }

        player.sendMessage(Component.text("ワールド「" + world.getName() + "」のバックアップを開始します...", NamedTextColor.YELLOW));

        // Save world on main thread (world.save() must run on main thread)
        // But run the file copy operation asynchronously to avoid lag
        final World worldToSave = world;
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = plugin.getWorldBackupManager().saveWorld(worldToSave);

            if (success) {
                player.sendMessage(Component.text("ワールド「" + worldToSave.getName() + "」のバックアップが完了しました。", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("ワールドのバックアップに失敗しました。", NamedTextColor.RED));
            }
        });

        return true;
    }
}
