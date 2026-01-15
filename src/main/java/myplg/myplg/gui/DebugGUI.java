package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * デバッグモード用GUI
 */
public class DebugGUI {

    private final PvPGame plugin;
    
    // GUIタイトル（識別用）
    public static final String MAIN_GUI_TITLE = "§c§lデバッグメニュー";
    public static final String TEAM_SELECT_BED_TITLE = "§c§lベッド破壊 - チーム選択";
    public static final String TEAM_SELECT_TP_TITLE = "§c§lテレポート - チーム選択";
    public static final String GENERATOR_GUI_TITLE = "§c§lジェネレーター設定";

    public DebugGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    /**
     * メインデバッグGUIを開く
     */
    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, MAIN_GUI_TITLE);

        // ===== ゲーム制御セクション（上段） =====
        // ゲーム終了
        gui.setItem(10, createItem(Material.BARRIER, "§c§lゲーム終了",
                "§7クリックでゲームを即座に終了",
                "§8/end コマンドを実行"));

        // ベッド全破壊
        gui.setItem(11, createItem(Material.RED_BED, "§c§lベッド全破壊",
                "§7クリックで全チームのベッドを破壊",
                "§8ENDモードに移行します"));

        // 特定チームベッド破壊
        gui.setItem(12, createItem(Material.YELLOW_BED, "§e§l特定チームベッド破壊",
                "§7クリックしてチームを選択",
                "§8選択したチームのベッドを破壊"));

        // ゲームタイマー操作
        gui.setItem(13, createItem(Material.CLOCK, "§6§lタイマー操作",
                "§7左クリック: 30秒スキップ",
                "§7右クリック: 1分スキップ",
                "§7シフト+クリック: タイマーリセット"));

        // ===== プレイヤーセクション（中段左） =====
        // 無敵モード
        boolean isInvincible = player.isInvulnerable();
        gui.setItem(19, createItem(
                isInvincible ? Material.TOTEM_OF_UNDYING : Material.SHIELD,
                isInvincible ? "§a§l無敵モード ON" : "§7無敵モード OFF",
                "§7クリックで切り替え",
                isInvincible ? "§a現在: 無敵" : "§c現在: 通常"));

        // 透明化
        boolean isInvisible = player.isInvisible();
        gui.setItem(20, createItem(
                isInvisible ? Material.GLASS : Material.TINTED_GLASS,
                isInvisible ? "§a§l透明化 ON" : "§7透明化 OFF",
                "§7クリックで切り替え",
                isInvisible ? "§a現在: 透明" : "§c現在: 可視"));

        // チーム変更
        gui.setItem(21, createItem(Material.LEATHER_CHESTPLATE, "§d§lチーム変更",
                "§7クリックしてチームを選択",
                "§8自分のチームを変更"));

        // テレポート
        gui.setItem(22, createItem(Material.ENDER_PEARL, "§5§lテレポート",
                "§7クリックしてチームスポーンを選択",
                "§8各チームのスポーン地点にTP"));

        // ===== アイテムセクション（中段右） =====
        // リソース大量取得
        gui.setItem(28, createItem(Material.DIAMOND, "§b§lリソース取得",
                "§7クリックで各種リソースを64個ずつ取得",
                "§8ダイヤ、エメラルド、ゴールド、鉄"));

        // フル装備セット
        gui.setItem(29, createItem(Material.DIAMOND_CHESTPLATE, "§b§lフル装備",
                "§7クリックでダイヤ装備フルセット",
                "§8防具と剣を装備"));

        // 全ツール取得
        gui.setItem(30, createItem(Material.DIAMOND_PICKAXE, "§a§l全ツール取得",
                "§7クリックで全ツールを取得",
                "§8ツルハシ、斧、シャベル"));

        // 特殊アイテム
        gui.setItem(31, createItem(Material.TNT, "§c§l特殊アイテム",
                "§7クリックで特殊アイテムを取得",
                "§8TNT, ファイヤーボール, エンダーパール, ブリッジエッグ"));

        // ===== ジェネレーターセクション（下段） =====
        // ジェネレーター設定
        boolean generatorsRunning = !plugin.getGeneratorManager().getGenerators().isEmpty();
        gui.setItem(37, createItem(Material.BEACON, "§e§lジェネレーター設定",
                "§7クリックでジェネレーター設定を開く",
                "§8速度変更、一時停止/再開"));

        // ジェネレーター一時停止/再開
        gui.setItem(38, createItem(Material.REDSTONE_TORCH, "§c§lジェネ一時停止/再開",
                "§7クリックで全ジェネレーターを停止/再開",
                "§8現在の状態に応じてトグル"));

        // ジェネレーター速度2倍
        gui.setItem(39, createItem(Material.SUGAR, "§a§lジェネ速度 x2",
                "§7クリックでジェネレーター速度を2倍に",
                "§8全てのジェネレーターに適用"));

        // ジェネレーター速度リセット
        gui.setItem(40, createItem(Material.REDSTONE, "§7ジェネ速度リセット",
                "§7クリックでジェネレーター速度を元に戻す",
                "§8初期設定に戻す"));

        // ===== 装飾 =====
        // 枠を灰色ガラスで埋める
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        // セクション区切り（赤ガラス）
        ItemStack redGlass = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        gui.setItem(0, redGlass);
        gui.setItem(8, redGlass);
        gui.setItem(45, redGlass);
        gui.setItem(53, redGlass);

        player.openInventory(gui);
    }

    /**
     * チーム選択GUI（ベッド破壊用）
     */
    public void openTeamSelectForBed(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TEAM_SELECT_BED_TITLE);
        
        int slot = 10;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            Material wool = getTeamWool(team.getName());
            boolean hasBed = plugin.getScoreboardManager().isBedAlive(team.getName());
            
            gui.setItem(slot, createItem(wool,
                    getTeamColor(team.getName()) + "§l" + team.getName(),
                    hasBed ? "§a✓ ベッドあり" : "§c✗ ベッド破壊済み",
                    hasBed ? "§7クリックで破壊" : "§8既に破壊されています"));
            slot++;
            if (slot == 17) break;
        }

        // 戻るボタン
        gui.setItem(22, createItem(Material.ARROW, "§c戻る", "§7メインメニューに戻る"));

        // 枠
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        player.openInventory(gui);
    }

    /**
     * チーム選択GUI（テレポート用）
     */
    public void openTeamSelectForTP(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TEAM_SELECT_TP_TITLE);
        
        int slot = 10;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            Material wool = getTeamWool(team.getName());
            
            gui.setItem(slot, createItem(wool,
                    getTeamColor(team.getName()) + "§l" + team.getName(),
                    "§7クリックでスポーン地点にTP",
                    "§8" + formatLocation(team.getSpawnLocation())));
            slot++;
            if (slot == 17) break;
        }

        // 戻るボタン
        gui.setItem(22, createItem(Material.ARROW, "§c戻る", "§7メインメニューに戻る"));

        // 枠
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        player.openInventory(gui);
    }

    /**
     * チーム選択GUI（チーム変更用）
     */
    public void openTeamSelectForChange(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§c§lチーム変更 - チーム選択");
        
        String currentTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        
        int slot = 10;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            Material wool = getTeamWool(team.getName());
            boolean isCurrent = team.getName().equals(currentTeam);
            
            gui.setItem(slot, createItem(wool,
                    getTeamColor(team.getName()) + "§l" + team.getName(),
                    isCurrent ? "§a★ 現在のチーム" : "§7クリックで参加",
                    "§8メンバー数: " + team.getMembers().size()));
            slot++;
            if (slot == 17) break;
        }

        // 戻るボタン
        gui.setItem(22, createItem(Material.ARROW, "§c戻る", "§7メインメニューに戻る"));

        // 枠
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        player.openInventory(gui);
    }

    /**
     * ジェネレーター設定GUI
     */
    public void openGeneratorGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GENERATOR_GUI_TITLE);

        // 全停止/再開
        gui.setItem(10, createItem(Material.REDSTONE_TORCH, "§c§l全停止/再開",
                "§7クリックで全ジェネレーターを停止/再開"));

        // 速度 x0.5
        gui.setItem(11, createItem(Material.FEATHER, "§7速度 x0.5",
                "§7クリックで速度を半分に"));

        // 速度 x2
        gui.setItem(12, createItem(Material.SUGAR, "§a速度 x2",
                "§7クリックで速度を2倍に"));

        // 速度 x4
        gui.setItem(13, createItem(Material.BLAZE_POWDER, "§e速度 x4",
                "§7クリックで速度を4倍に"));

        // 速度リセット
        gui.setItem(14, createItem(Material.REDSTONE, "§c速度リセット",
                "§7クリックで初期速度に戻す"));

        // 戻るボタン
        gui.setItem(22, createItem(Material.ARROW, "§c戻る", "§7メインメニューに戻る"));

        // 枠
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }

        player.openInventory(gui);
    }

    // ===== ユーティリティメソッド =====

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getTeamWool(String teamName) {
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

    private String getTeamColor(String teamName) {
        switch (teamName) {
            case "レッド": return "§c";
            case "ブルー": return "§9";
            case "グリーン": return "§a";
            case "イエロー": return "§e";
            case "アクア": return "§b";
            case "ホワイト": return "§f";
            case "ピンク": return "§d";
            case "グレー": return "§7";
            default: return "§f";
        }
    }

    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "未設定";
        return String.format("X:%.0f Y:%.0f Z:%.0f", loc.getX(), loc.getY(), loc.getZ());
    }
}
