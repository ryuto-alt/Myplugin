package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * ゲームモード選択GUI（ソロ/デュオ/トリプル/カスタム）
 */
public class GameModeSelector {
    private final PvPGame plugin;

    public GameModeSelector(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * ゲームモード選択GUIを開く
     */
    public void openGameModeSelector(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lゲームモード選択");

        // ソロモード (1v1v1v1...)
        ItemStack solo = new ItemStack(Material.IRON_SWORD);
        ItemMeta soloMeta = solo.getItemMeta();
        if (soloMeta != null) {
            soloMeta.setDisplayName("§c§lソロモード");
            soloMeta.setLore(Arrays.asList(
                "§71人チーム（1v1v1...）",
                "§7各プレイヤーが1つのチームに所属",
                "§7",
                "§eクリックして選択"
            ));
            solo.setItemMeta(soloMeta);
        }
        inv.setItem(11, solo);

        // デュオモード (2v2v2v2...)
        ItemStack duo = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta duoMeta = duo.getItemMeta();
        if (duoMeta != null) {
            duoMeta.setDisplayName("§9§lデュオモード");
            duoMeta.setLore(Arrays.asList(
                "§72人チーム（2v2v2...）",
                "§7各チームに2人まで参加可能",
                "§7",
                "§eクリックして選択"
            ));
            duo.setItemMeta(duoMeta);
        }
        inv.setItem(12, duo);

        // トリプルモード (3v3v3v3...)
        ItemStack triple = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta tripleMeta = triple.getItemMeta();
        if (tripleMeta != null) {
            tripleMeta.setDisplayName("§d§lトリプルモード");
            tripleMeta.setLore(Arrays.asList(
                "§73人チーム（3v3v3...）",
                "§7各チームに3人まで参加可能",
                "§7",
                "§eクリックして選択"
            ));
            triple.setItemMeta(tripleMeta);
        }
        inv.setItem(13, triple);

        // カスタムモード (自由なチーム編成)
        ItemStack custom = new ItemStack(Material.NETHER_STAR);
        ItemMeta customMeta = custom.getItemMeta();
        if (customMeta != null) {
            customMeta.setDisplayName("§6§lカスタムモード");
            customMeta.setLore(Arrays.asList(
                "§7自由なチーム編成",
                "§7各チームの人数を自由に設定",
                "§7例: 青3人 vs 赤2人 vs アクア1人",
                "§7",
                "§eクリックして選択"
            ));
            custom.setItemMeta(customMeta);
        }
        inv.setItem(15, custom);

        player.openInventory(inv);
    }

    /**
     * ゲームモード選択を処理
     */
    public void handleGameModeSelection(Player player, String mode) {
        switch (mode) {
            case "SOLO":
                // ソロモードの場合、全プレイヤーにチーム選択GUIを開く（1人制限）
                openTeamSelectorForAll(1);
                break;
            case "DUO":
                // デュオモードの場合、全プレイヤーにチーム選択GUIを開く（2人制限）
                openTeamSelectorForAll(2);
                break;
            case "TRIPLE":
                // トリプルモードの場合、全プレイヤーにチーム選択GUIを開く（3人制限）
                openTeamSelectorForAll(3);
                break;
            case "CUSTOM":
                // カスタムモードの場合、全プレイヤーにカスタムチーム設定GUIを開く
                openCustomSetupForAll();
                break;
        }
    }

    /**
     * 全プレイヤーにチーム選択GUIを開く（ソロ/デュオ/トリプル用）
     */
    private void openTeamSelectorForAll(int maxPlayers) {
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            plugin.getTeamSelectorGUI().openTeamSelector(online, maxPlayers);
        }
    }

    /**
     * 全プレイヤーにカスタムチーム設定GUIを開く
     */
    private void openCustomSetupForAll() {
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            plugin.getCustomTeamSetupGUI().openCustomSetup(online);
        }
    }
}
