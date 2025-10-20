package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class Shop1Command implements CommandExecutor {
    private final PvPGame plugin;

    public Shop1Command(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cこのコマンドはOP権限が必要です。");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        // Create villager spawn egg
        ItemStack spawnEgg = new ItemStack(Material.VILLAGER_SPAWN_EGG, 1);
        ItemMeta meta = spawnEgg.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lショップ (ブロック・剣・ポーション・TNT)");
            meta.setLore(Arrays.asList(
                "§7右クリックで村人を召喚",
                "§7村人を右クリックでショップを開く",
                "§e通貨: 鉄、ゴールド、エメラルド"
            ));
            spawnEgg.setItemMeta(meta);
        }

        player.getInventory().addItem(spawnEgg);
        player.sendMessage("§aショップ村人のスポーンエッグを入手しました！");
        player.sendMessage("§7地面に右クリックで村人を召喚できます。");

        return true;
    }
}
