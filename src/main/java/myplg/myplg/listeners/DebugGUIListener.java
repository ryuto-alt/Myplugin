package myplg.myplg.listeners;

import myplg.myplg.DebugBook;
import myplg.myplg.Generator;
import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import myplg.myplg.gui.DebugGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * デバッグGUIのクリックイベント処理
 */
public class DebugGUIListener implements Listener {

    private final PvPGame plugin;
    private final DebugGUI debugGUI;
    
    // ジェネレーターの元の速度を保存
    private final Map<String, Integer> originalGeneratorSpeeds = new HashMap<>();
    private boolean generatorsPaused = false;

    public DebugGUIListener(PvPGame plugin) {
        this.plugin = plugin;
        this.debugGUI = new DebugGUI(plugin);
    }

    public DebugGUI getDebugGUI() {
        return debugGUI;
    }

    /**
     * デバッグコンパスの右クリックでGUIを開く
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 右クリックかどうか
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // デバッグコンパスかどうか
        if (!DebugBook.isDebugCompass(item)) {
            return;
        }

        // デバッグモードかどうか
        if (!plugin.getGameManager().isDebugMode()) {
            player.sendMessage("§c§l[DEBUG] §cデバッグモードでのみ使用可能です。");
            return;
        }

        event.setCancelled(true);
        debugGUI.openMainGUI(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    /**
     * GUI内のクリックイベント処理
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // デバッグGUIかどうか
        if (!title.startsWith("§c§l")) return;

        // メインGUI
        if (title.equals(DebugGUI.MAIN_GUI_TITLE)) {
            event.setCancelled(true);
            handleMainGUIClick(player, event.getSlot(), event.getClick());
            return;
        }

        // ベッド破壊チーム選択
        if (title.equals(DebugGUI.TEAM_SELECT_BED_TITLE)) {
            event.setCancelled(true);
            handleBedSelectClick(player, event.getSlot());
            return;
        }

        // テレポートチーム選択
        if (title.equals(DebugGUI.TEAM_SELECT_TP_TITLE)) {
            event.setCancelled(true);
            handleTPSelectClick(player, event.getSlot());
            return;
        }

        // チーム変更選択
        if (title.equals("§c§lチーム変更 - チーム選択")) {
            event.setCancelled(true);
            handleTeamChangeClick(player, event.getSlot());
            return;
        }

        // ジェネレーター設定
        if (title.equals(DebugGUI.GENERATOR_GUI_TITLE)) {
            event.setCancelled(true);
            handleGeneratorGUIClick(player, event.getSlot());
            return;
        }
    }

    /**
     * メインGUIのクリック処理
     */
    private void handleMainGUIClick(Player player, int slot, ClickType clickType) {
        switch (slot) {
            // ===== ゲーム制御 =====
            case 10: // ゲーム終了
                player.closeInventory();
                player.performCommand("end");
                break;

            case 11: // ベッド全破壊
                destroyAllBeds(player);
                break;

            case 12: // 特定チームベッド破壊
                debugGUI.openTeamSelectForBed(player);
                break;

            case 13: // タイマー操作
                handleTimerControl(player, clickType);
                break;

            // ===== プレイヤー =====
            case 19: // 無敵モード
                toggleInvincibility(player);
                debugGUI.openMainGUI(player); // GUIを更新
                break;

            case 20: // 透明化
                toggleInvisibility(player);
                debugGUI.openMainGUI(player); // GUIを更新
                break;

            case 21: // チーム変更
                debugGUI.openTeamSelectForChange(player);
                break;

            case 22: // テレポート
                debugGUI.openTeamSelectForTP(player);
                break;

            // ===== アイテム =====
            case 28: // リソース取得
                giveResources(player);
                break;

            case 29: // フル装備
                giveFullArmor(player);
                break;

            case 30: // 全ツール
                giveAllTools(player);
                break;

            case 31: // 特殊アイテム
                giveSpecialItems(player);
                break;

            // ===== ジェネレーター =====
            case 37: // ジェネレーター設定
                debugGUI.openGeneratorGUI(player);
                break;

            case 38: // ジェネレーター一時停止/再開
                toggleGenerators(player);
                break;

            case 39: // ジェネレーター速度2倍
                changeGeneratorSpeed(player, 0.5); // interval半分 = 速度2倍
                break;

            case 40: // ジェネレーター速度リセット
                resetGeneratorSpeed(player);
                break;
        }
    }

    /**
     * ベッド破壊チーム選択のクリック処理
     */
    private void handleBedSelectClick(Player player, int slot) {
        if (slot == 22) { // 戻る
            debugGUI.openMainGUI(player);
            return;
        }

        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            List<Team> teams = new ArrayList<>(plugin.getGameManager().getTeams().values());
            if (index < teams.size()) {
                Team team = teams.get(index);
                destroyTeamBed(player, team);
                debugGUI.openTeamSelectForBed(player); // GUIを更新
            }
        }
    }

    /**
     * テレポートチーム選択のクリック処理
     */
    private void handleTPSelectClick(Player player, int slot) {
        if (slot == 22) { // 戻る
            debugGUI.openMainGUI(player);
            return;
        }

        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            List<Team> teams = new ArrayList<>(plugin.getGameManager().getTeams().values());
            if (index < teams.size()) {
                Team team = teams.get(index);
                teleportToTeamSpawn(player, team);
            }
        }
    }

    /**
     * チーム変更選択のクリック処理
     */
    private void handleTeamChangeClick(Player player, int slot) {
        if (slot == 22) { // 戻る
            debugGUI.openMainGUI(player);
            return;
        }

        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            List<Team> teams = new ArrayList<>(plugin.getGameManager().getTeams().values());
            if (index < teams.size()) {
                Team team = teams.get(index);
                changePlayerTeam(player, team);
            }
        }
    }

    /**
     * ジェネレーターGUIのクリック処理
     */
    private void handleGeneratorGUIClick(Player player, int slot) {
        switch (slot) {
            case 10: // 全停止/再開
                toggleGenerators(player);
                break;
            case 11: // 速度 x0.5
                changeGeneratorSpeed(player, 2.0); // interval2倍 = 速度0.5倍
                break;
            case 12: // 速度 x2
                changeGeneratorSpeed(player, 0.5);
                break;
            case 13: // 速度 x4
                changeGeneratorSpeed(player, 0.25);
                break;
            case 14: // 速度リセット
                resetGeneratorSpeed(player);
                break;
            case 22: // 戻る
                debugGUI.openMainGUI(player);
                break;
        }
    }

    // ===== 機能実装メソッド =====

    private void destroyAllBeds(Player player) {
        int destroyed = 0;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            if (plugin.getScoreboardManager().isBedAlive(team.getName())) {
                plugin.getScoreboardManager().setBedStatus(team.getName(), false);
                destroyed++;
            }
        }

        // ENDモードチェック
        plugin.getEndModeManager().checkAllBedsDestroyed();

        player.sendMessage("§c§l[DEBUG] §e" + destroyed + "チームのベッドを破壊しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.0f);
        
        // 全プレイヤーに通知
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                p.sendMessage("§c§l[DEBUG] §e全てのベッドが破壊されました！");
            }
        }
    }

    private void destroyTeamBed(Player player, Team team) {
        if (!plugin.getScoreboardManager().isBedAlive(team.getName())) {
            player.sendMessage("§c§l[DEBUG] §cそのチームのベッドは既に破壊されています。");
            return;
        }

        plugin.getScoreboardManager().setBedStatus(team.getName(), false);
        plugin.getEndModeManager().checkAllBedsDestroyed();

        player.sendMessage("§c§l[DEBUG] §e" + team.getName() + "チームのベッドを破壊しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
    }

    private void handleTimerControl(Player player, ClickType clickType) {
        // TODO: タイマー機能が実装されていれば連携
        if (clickType == ClickType.LEFT) {
            player.sendMessage("§c§l[DEBUG] §e30秒スキップ（タイマー機能と連携予定）");
        } else if (clickType == ClickType.RIGHT) {
            player.sendMessage("§c§l[DEBUG] §e1分スキップ（タイマー機能と連携予定）");
        } else if (clickType.isShiftClick()) {
            player.sendMessage("§c§l[DEBUG] §eタイマーリセット（タイマー機能と連携予定）");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }

    private void toggleInvincibility(Player player) {
        boolean newState = !player.isInvulnerable();
        player.setInvulnerable(newState);
        player.sendMessage("§c§l[DEBUG] §e無敵モード: " + (newState ? "§aON" : "§cOFF"));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, newState ? 1.5f : 0.5f);
    }

    private void toggleInvisibility(Player player) {
        boolean newState = !player.isInvisible();
        player.setInvisible(newState);
        
        if (newState) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        
        player.sendMessage("§c§l[DEBUG] §e透明化: " + (newState ? "§aON" : "§cOFF"));
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.5f, 1.0f);
    }

    private void changePlayerTeam(Player player, Team newTeam) {
        String oldTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        
        // 古いチームから削除
        if (oldTeam != null) {
            Team old = plugin.getGameManager().getTeam(oldTeam);
            if (old != null) {
                old.removeMember(player.getUniqueId());
            }
        }
        
        // 新しいチームに追加
        newTeam.addMember(player.getUniqueId());
        plugin.getGameManager().setPlayerTeam(player.getUniqueId(), newTeam.getName());
        
        // スコアボード更新（全プレイヤー）
        plugin.getScoreboardManager().updateAllScoreboards();
        
        player.sendMessage("§c§l[DEBUG] §e" + newTeam.getName() + "チームに変更しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        
        debugGUI.openTeamSelectForChange(player);
    }

    private void teleportToTeamSpawn(Player player, Team team) {
        Location spawn = team.getSpawnLocation();
        if (spawn == null) {
            player.sendMessage("§c§l[DEBUG] §cそのチームのスポーン地点が設定されていません。");
            return;
        }

        player.teleport(spawn);
        player.sendMessage("§c§l[DEBUG] §e" + team.getName() + "チームのスポーン地点にテレポートしました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
    }

    private void giveResources(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.IRON_INGOT, 64),
                new ItemStack(Material.GOLD_INGOT, 64),
                new ItemStack(Material.DIAMOND, 64),
                new ItemStack(Material.EMERALD, 64)
        );
        player.sendMessage("§c§l[DEBUG] §eリソースを取得しました（各64個）");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    private void giveFullArmor(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        player.sendMessage("§c§l[DEBUG] §eダイヤフル装備を取得しました。");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);
    }

    private void giveAllTools(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.DIAMOND_PICKAXE),
                new ItemStack(Material.DIAMOND_AXE),
                new ItemStack(Material.DIAMOND_SHOVEL),
                new ItemStack(Material.SHEARS)
        );
        player.sendMessage("§c§l[DEBUG] §e全ツールを取得しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    private void giveSpecialItems(Player player) {
        // TNT
        player.getInventory().addItem(new ItemStack(Material.TNT, 8));
        
        // ファイヤーボール
        ItemStack fireball = new ItemStack(Material.FIRE_CHARGE, 4);
        ItemMeta fireballMeta = fireball.getItemMeta();
        if (fireballMeta != null) {
            fireballMeta.setDisplayName("§c§lファイヤーボール");
            fireball.setItemMeta(fireballMeta);
        }
        player.getInventory().addItem(fireball);
        
        // エンダーパール
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
        
        // ブリッジエッグ
        ItemStack bridgeEgg = new ItemStack(Material.EGG, 8);
        ItemMeta eggMeta = bridgeEgg.getItemMeta();
        if (eggMeta != null) {
            eggMeta.setDisplayName("§a§lブリッジエッグ");
            List<String> lore = new ArrayList<>();
            lore.add("§7投げた方向に橋を作る");
            eggMeta.setLore(lore);
            eggMeta.setCustomModelData(1001); // BridgeBuilderListenerの識別子
            bridgeEgg.setItemMeta(eggMeta);
        }
        player.getInventory().addItem(bridgeEgg);
        
        player.sendMessage("§c§l[DEBUG] §e特殊アイテムを取得しました。");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
    }

    private void toggleGenerators(Player player) {
        if (generatorsPaused) {
            // 再開
            plugin.getGeneratorManager().startAllGenerators();
            generatorsPaused = false;
            player.sendMessage("§c§l[DEBUG] §aジェネレーターを再開しました。");
        } else {
            // 停止
            plugin.getGeneratorManager().stopAllGenerators();
            generatorsPaused = true;
            player.sendMessage("§c§l[DEBUG] §cジェネレーターを停止しました。");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.0f);
    }

    private void changeGeneratorSpeed(Player player, double multiplier) {
        // 元の速度を保存（初回のみ）
        if (originalGeneratorSpeeds.isEmpty()) {
            for (Generator gen : plugin.getGeneratorManager().getGenerators().values()) {
                originalGeneratorSpeeds.put(gen.getId(), gen.getSpawnInterval());
            }
        }

        // 速度変更（ファイルには保存しない = 一時的な変更）
        for (Generator gen : plugin.getGeneratorManager().getGenerators().values()) {
            int originalSpeed = originalGeneratorSpeeds.getOrDefault(gen.getId(), gen.getSpawnInterval());
            int newInterval = (int) (originalSpeed * multiplier);
            if (newInterval < 1) newInterval = 1; // 最小値
            plugin.getGeneratorManager().updateGeneratorInterval(gen.getId(), newInterval, false);
        }

        String speedText = multiplier < 1 ? "x" + (int)(1/multiplier) : "x" + String.format("%.1f", 1/multiplier);
        player.sendMessage("§c§l[DEBUG] §eジェネレーター速度を " + speedText + " に変更しました。");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
    }

    private void resetGeneratorSpeed(Player player) {
        if (originalGeneratorSpeeds.isEmpty()) {
            player.sendMessage("§c§l[DEBUG] §c速度は既に初期状態です。");
            return;
        }

        for (Generator gen : plugin.getGeneratorManager().getGenerators().values()) {
            Integer originalSpeed = originalGeneratorSpeeds.get(gen.getId());
            if (originalSpeed != null) {
                plugin.getGeneratorManager().updateGeneratorInterval(gen.getId(), originalSpeed, false);
            }
        }

        originalGeneratorSpeeds.clear();
        player.sendMessage("§c§l[DEBUG] §eジェネレーター速度を初期状態に戻しました。");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f);
    }

    /**
     * ゲーム終了時にデバッグ状態をリセット
     * resetGameStateから呼び出される
     */
    public void reset() {
        // ジェネレーター速度の記録をクリア（新しいゲームでは新しい値を使う）
        originalGeneratorSpeeds.clear();
        generatorsPaused = false;

        // プレイヤーの無敵・透明状態をリセット
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isInvulnerable()) {
                player.setInvulnerable(false);
            }
            if (player.isInvisible()) {
                player.setInvisible(false);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        plugin.getLogger().info("[DEBUG] デバッグ状態をリセットしました。");
    }
}
