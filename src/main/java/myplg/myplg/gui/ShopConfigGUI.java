package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopConfigGUI {
    private final PvPGame plugin;
    private final Map<UUID, UUID> openConfigs; // Player UUID -> Villager UUID

    public ShopConfigGUI(PvPGame plugin) {
        this.plugin = plugin;
        this.openConfigs = new HashMap<>();
    }

    public void openConfigGUI(Player player, Villager villager, String teamName) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lショップ設定");

        // Delete button
        ItemStack deleteBtn = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = deleteBtn.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§c§lショップを削除");
            deleteMeta.setLore(Arrays.asList(
                "§7このショップ村人を削除します",
                "§7チーム: §e" + (teamName != null ? teamName : "未設定"),
                "",
                "§c§l警告: この操作は取り消せません！",
                "",
                "§aクリックして削除"
            ));
            deleteBtn.setItemMeta(deleteMeta);
        }
        inv.setItem(13, deleteBtn);

        // Info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lショップ情報");
            infoMeta.setLore(Arrays.asList(
                "§7チーム: §e" + (teamName != null ? teamName : "未設定"),
                "§7UUID: §7" + villager.getUniqueId().toString().substring(0, 8) + "..."
            ));
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(11, infoItem);

        // Close button
        ItemStack closeBtn = new ItemStack(Material.ARROW);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§7閉じる");
            closeBtn.setItemMeta(closeMeta);
        }
        inv.setItem(15, closeBtn);

        // Store the villager being configured
        openConfigs.put(player.getUniqueId(), villager.getUniqueId());

        player.openInventory(inv);
    }

    public UUID getConfiguredVillager(UUID playerUUID) {
        return openConfigs.get(playerUUID);
    }

    public void removeConfiguredVillager(UUID playerUUID) {
        openConfigs.remove(playerUUID);
    }

    public boolean hasOpenConfig(UUID playerUUID) {
        return openConfigs.containsKey(playerUUID);
    }
}
