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

public class Shop2Command implements CommandExecutor {
    private final PvPGame plugin;

    public Shop2Command(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        // Create skeleton spawn egg
        ItemStack spawnEgg = new ItemStack(Material.SKELETON_SPAWN_EGG, 1);
        ItemMeta meta = spawnEgg.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lショップ2 (陣地強化・武器強化・装備強化・トラップ)");
            meta.setLore(Arrays.asList(
                "§7右クリックでスケルトンを召喚",
                "§7スケルトンを右クリックでショップを開く",
                "§e通貨: ダイヤモンドのみ"
            ));
            spawnEgg.setItemMeta(meta);
        }

        player.getInventory().addItem(spawnEgg);
        player.sendMessage("§aショップ2スケルトンのスポーンエッグを入手しました！");
        player.sendMessage("§7地面に右クリックでスケルトンを召喚できます。");

        return true;
    }
}
