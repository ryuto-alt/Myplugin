package myplg.myplg;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * デバッグモード用コンパス
 * デバッグモード中にプレイヤーに渡され、右クリックでDebugGUIを開く
 */
public class DebugBook {

    private final PvPGame plugin;

    // アイテムの識別子（カスタムモデルデータ）
    public static final int DEBUG_COMPASS_MODEL_DATA = 8888;

    public DebugBook(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * デバッグコンパスを作成
     */
    public ItemStack createCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§c§lデバッグツール");

            List<String> lore = new ArrayList<>();
            lore.add("§7右クリックでデバッグメニューを開く");
            lore.add("");
            lore.add("§8デバッグモード専用アイテム");
            meta.setLore(lore);

            // カスタムモデルデータで識別
            meta.setCustomModelData(DEBUG_COMPASS_MODEL_DATA);

            compass.setItemMeta(meta);
        }

        return compass;
    }

    /**
     * プレイヤーにデバッグコンパスを渡す
     */
    public void giveCompass(Player player) {
        // 既に持っているか確認
        if (hasCompass(player)) {
            return;
        }

        ItemStack compass = createCompass();
        player.getInventory().setItem(8, compass); // 9番目のスロット（インベントリの右端）
        player.sendMessage("§c§l[DEBUG] §aデバッグツールを受け取りました！右クリックでメニューを開きます。");
    }

    /**
     * プレイヤーがデバッグコンパスを持っているか確認
     */
    public boolean hasCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isDebugCompass(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * アイテムがデバッグコンパスかどうか確認
     */
    public static boolean isDebugCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // カスタムモデルデータで識別
        return meta.hasCustomModelData() && meta.getCustomModelData() == DEBUG_COMPASS_MODEL_DATA;
    }

    /**
     * プレイヤーからデバッグコンパスを削除
     */
    public void removeCompass(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDebugCompass(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    /**
     * 全プレイヤーからデバッグコンパスを削除
     */
    public void removeAllCompasses() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            removeCompass(player);
        }
    }
}
