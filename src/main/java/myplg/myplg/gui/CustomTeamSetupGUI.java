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
 * カスタムチーム設定GUI（自由な人数制限）
 */
public class CustomTeamSetupGUI {
    private final PvPGame plugin;

    public CustomTeamSetupGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * カスタムチーム設定GUIを開く
     */
    public void openCustomSetup(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lカスタムチーム設定");

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
                int maxPlayers = plugin.getGameSetupManager().getTeamMaxPlayers(teamName);

                lore.add("§e現在の参加者: §f" + currentMembers.size() + "§7/§f" + maxPlayers + "人");

                if (!currentMembers.isEmpty()) {
                    lore.add("§7");
                    for (Player member : currentMembers) {
                        lore.add("§7  - " + member.getName());
                    }
                }

                lore.add("§7");
                lore.add("§e左クリック: §a参加");
                lore.add("§e右クリック: §c人数上限を変更");

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
                "§7チーム編成が完了したら",
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
     * チーム参加処理（左クリック）
     */
    public void handleTeamJoin(Player player, String teamName) {
        // 現在のチームメンバー数を確認
        List<Player> currentMembers = plugin.getGameSetupManager().getTeamMembers(teamName);
        int maxPlayers = plugin.getGameSetupManager().getTeamMaxPlayers(teamName);

        if (currentMembers.size() >= maxPlayers) {
            player.sendMessage("§c" + teamName + "チームは満員です！人数上限を増やすか、別のチームを選択してください。");
            return;
        }

        // プレイヤーのチームを設定
        plugin.getGameSetupManager().setPlayerTeam(player, teamName);
        player.sendMessage("§a" + teamName + "チームに参加しました！");

        // 全プレイヤーのGUIを更新（リアルタイム更新）
        updateAllCustomGUIs();
    }

    /**
     * チーム人数上限変更処理（右クリック）
     */
    public void handleTeamLimitChange(Player player, String teamName) {
        // 人数上限選択GUIを開く
        openLimitSelector(player, teamName);
    }

    /**
     * 人数上限選択GUIを開く
     */
    private void openLimitSelector(Player player, String teamName) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§l" + teamName + "の人数上限");

        // 1人～8人の選択肢を作成
        for (int i = 1; i <= 8; i++) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD, i);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e§l" + i + "人");
                meta.setLore(Arrays.asList(
                    "§7",
                    "§7このチームの最大人数を",
                    "§e" + i + "人§7に設定します",
                    "§7",
                    "§aクリックして設定"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(9 + i, item);
        }

        // 戻るボタン
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList(
                "§7",
                "§7カスタム設定に戻る"
            ));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(22, backButton);

        player.openInventory(inv);
    }

    /**
     * 人数上限設定を処理
     */
    public void handleLimitSelection(Player player, String teamName, int limit) {
        plugin.getGameSetupManager().setTeamMaxPlayers(teamName, limit);
        player.sendMessage("§a" + teamName + "チームの人数上限を" + limit + "人に設定しました！");

        // カスタム設定GUIに戻る
        openCustomSetup(player);

        // 他のプレイヤーのGUIも更新
        updateAllCustomGUIs();
    }

    /**
     * すべてのプレイヤーのカスタムGUIを更新
     */
    private void updateAllCustomGUIs() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTitle().equals("§6§lカスタムチーム設定")) {
                openCustomSetup(online);
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
