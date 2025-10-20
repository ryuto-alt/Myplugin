package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * チーム選択GUI（ソロ/デュオ/トリプルモード用）
 */
public class TeamSelectorGUI {
    private final PvPGame plugin;
    private int maxPlayersPerTeam = 1; // デフォルトはソロ

    public TeamSelectorGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * チーム選択GUIを開く
     * @param player プレイヤー
     * @param maxPlayers チームあたりの最大人数
     */
    public void openTeamSelector(Player player, int maxPlayers) {
        this.maxPlayersPerTeam = maxPlayers;
        plugin.getGameSetupManager().setMaxPlayersPerTeam(maxPlayers);

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lチーム選択");

        Map<String, Team> teams = plugin.getGameManager().getTeams();

        int slot = 10;
        for (Map.Entry<String, Team> entry : teams.entrySet()) {
            String teamName = entry.getKey();
            Team team = entry.getValue();

            Material material = getTeamMaterial(teamName);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName("§f§l" + teamName + "チーム");

                List<String> lore = new ArrayList<>();
                lore.add("§7");

                // 現在の選択状況を表示
                List<Player> currentMembers = plugin.getGameSetupManager().getTeamMembers(teamName);
                lore.add("§e現在の参加者: §f" + currentMembers.size() + "§7/§f" + maxPlayers + "人");

                if (!currentMembers.isEmpty()) {
                    lore.add("§7");
                    for (Player member : currentMembers) {
                        lore.add("§7  - " + member.getName());
                    }
                }

                lore.add("§7");
                if (currentMembers.size() >= maxPlayers) {
                    lore.add("§c満員です！");
                } else {
                    lore.add("§aクリックして参加");
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
            slot++;

            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
        }

        // ゲーム開始ボタン
        ItemStack startButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta startMeta = startButton.getItemMeta();
        if (startMeta != null) {
            startMeta.setDisplayName("§a§lゲーム開始");
            startMeta.setLore(Arrays.asList(
                "§7",
                "§7全員がチームを選択したら",
                "§7クリックしてゲームを開始",
                "§7",
                "§eクリックして開始"
            ));
            startButton.setItemMeta(startMeta);
        }
        inv.setItem(49, startButton);

        // 戻るボタン
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList(
                "§7",
                "§7ゲームモード選択に戻る"
            ));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(45, backButton);

        player.openInventory(inv);
    }

    /**
     * チーム選択を処理
     */
    public void handleTeamSelection(Player player, String teamName) {
        // 現在のチームメンバー数を確認
        List<Player> currentMembers = plugin.getGameSetupManager().getTeamMembers(teamName);

        if (currentMembers.size() >= maxPlayersPerTeam) {
            player.sendMessage("§c" + teamName + "チームは満員です！");
            return;
        }

        // プレイヤーのチームを設定
        plugin.getGameSetupManager().setPlayerTeam(player, teamName);
        player.sendMessage("§a" + teamName + "チームに参加しました！");

        // 全プレイヤーのGUIを更新（リアルタイム更新）
        updateAllTeamSelectors();
    }

    /**
     * 全プレイヤーのチーム選択GUIを更新
     */
    public void updateAllTeamSelectors() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTitle().equals("§6§lチーム選択")) {
                openTeamSelector(online, maxPlayersPerTeam);
            }
        }
    }

    /**
     * チーム名に対応するマテリアルを取得
     */
    private Material getTeamMaterial(String teamName) {
        switch (teamName) {
            case "レッド": return Material.RED_WOOL;
            case "ブルー": return Material.BLUE_WOOL;
            case "グリーン": return Material.GREEN_WOOL;
            case "イエロー": return Material.YELLOW_WOOL;
            case "アクア": return Material.CYAN_WOOL;
            case "ホワイト": return Material.WHITE_WOOL;
            case "ピンク": return Material.PINK_WOOL;
            case "グレー": return Material.GRAY_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }

    /**
     * ゲーム開始処理
     */
    public void startGame() {
        // カウントダウンを開始
        plugin.getGameSetupManager().startCountdown();
    }
}
