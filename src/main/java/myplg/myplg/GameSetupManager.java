package myplg.myplg;

import myplg.myplg.listeners.BlockPlaceListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.time.Duration;
import java.util.*;

/**
 * ゲーム設定マネージャー
 * プレイヤーのチーム選択状態を管理し、カウントダウンとゲーム開始を処理する
 */
public class GameSetupManager {
    private final PvPGame plugin;

    // プレイヤーのチーム選択を保存
    private final Map<UUID, String> playerTeamSelection = new HashMap<>();

    // カスタムモード用：各チームの最大人数
    private final Map<String, Integer> teamMaxPlayers = new HashMap<>();

    // 通常モード用：全チーム共通の最大人数
    private int maxPlayersPerTeam = 1;

    public GameSetupManager(PvPGame plugin) {
        this.plugin = plugin;
        initializeTeamLimits();
    }

    /**
     * チーム人数制限を初期化（デフォルトは各チーム1人）
     */
    private void initializeTeamLimits() {
        for (String teamName : plugin.getGameManager().getTeams().keySet()) {
            teamMaxPlayers.put(teamName, 1);
        }
    }

    /**
     * プレイヤーのチームを設定
     */
    public void setPlayerTeam(Player player, String teamName) {
        playerTeamSelection.put(player.getUniqueId(), teamName);
    }

    /**
     * プレイヤーのチームを取得
     */
    public String getPlayerTeam(UUID playerUUID) {
        return playerTeamSelection.get(playerUUID);
    }

    /**
     * チームのメンバーリストを取得
     */
    public List<Player> getTeamMembers(String teamName) {
        List<Player> members = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerTeamSelection.entrySet()) {
            if (entry.getValue().equals(teamName)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    members.add(player);
                }
            }
        }
        return members;
    }

    /**
     * 全チーム共通の最大人数を設定（ソロ/デュオ/トリプルモード用）
     */
    public void setMaxPlayersPerTeam(int maxPlayers) {
        this.maxPlayersPerTeam = maxPlayers;
        // すべてのチームに同じ上限を設定
        for (String teamName : plugin.getGameManager().getTeams().keySet()) {
            teamMaxPlayers.put(teamName, maxPlayers);
        }
    }

    /**
     * 特定チームの最大人数を設定（カスタムモード用）
     */
    public void setTeamMaxPlayers(String teamName, int maxPlayers) {
        teamMaxPlayers.put(teamName, maxPlayers);
    }

    /**
     * チームの最大人数を取得
     */
    public int getTeamMaxPlayers(String teamName) {
        return teamMaxPlayers.getOrDefault(teamName, 1);
    }

    /**
     * カウントダウンを開始
     */
    public void startCountdown() {
        // Check if all players are assigned to teams
        List<Player> unassignedPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!playerTeamSelection.containsKey(player.getUniqueId())) {
                unassignedPlayers.add(player);
            }
        }

        // If there are unassigned players, cancel game start
        if (!unassignedPlayers.isEmpty()) {
            Bukkit.broadcastMessage("§c§l[エラー] ゲームを開始できません！");
            Bukkit.broadcastMessage("§c以下のプレイヤーがチームに所属していません:");
            for (Player player : unassignedPlayers) {
                Bukkit.broadcastMessage("§c  - " + player.getName());
            }
            Bukkit.broadcastMessage("§e全てのプレイヤーがチームを選択してください。");

            plugin.getLogger().warning("Game start cancelled: " + unassignedPlayers.size() + " players not assigned to teams");
            return; // Cancel game start
        }

        // すべてのプレイヤーのインベントリを閉じる
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
        }

        // カウントダウン: 5, 4, 3, 2, 1
        for (int i = 5; i >= 1; i--) {
            final int count = i;
            final long delay = (5 - i) * 20L; // 0, 20, 40, 60, 80 ticks

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Component title = Component.text("§e§l" + count);
                Component subtitle = Component.text("§7ゲーム開始まで...");

                Title countTitle = Title.title(
                    title,
                    subtitle,
                    Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofMillis(800),
                        Duration.ofMillis(200)
                    )
                );

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.showTitle(countTitle);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, delay);
        }

        // 1の直後にゲーム開始 (80 + 20 = 100 ticks = 5秒後)
        Bukkit.getScheduler().runTaskLater(plugin, this::startGame, 100L);
    }

    /**
     * ゲームを開始
     */
    private void startGame() {
        // Start the game
        plugin.getGameManager().setGameRunning(true);

        // Stop all lobby music and start game BGM
        plugin.getMusicManager().stopAllLobbyMusic();
        plugin.getMusicManager().startGameMusic();

        // Clear player-placed blocks tracking from previous game
        BlockPlaceListener.clearPlayerPlacedBlocks();

        // Clear all chests and ender chests in all worlds
        clearAllChests();

        // プレイヤーを選択したチームに割り当て
        assignPlayersToSelectedTeams();

        // Initialize scoreboard
        plugin.getScoreboardManager().initializeAllBeds();

        // Start scoreboard update task
        plugin.getScoreboardManager().startUpdateTask();

        // Handle teams with 0 members: remove bed and mark as eliminated
        for (Team team : plugin.getGameManager().getTeams().values()) {
            if (team.getMembers().isEmpty()) {
                // Remove bed block
                if (team.getBedBlock() != null) {
                    team.getBedBlock().setType(Material.AIR);
                    plugin.getLogger().info("Removed bed for empty team: " + team.getName());
                }

                // Mark bed as destroyed and team as eliminated in scoreboard
                plugin.getScoreboardManager().setBedStatus(team.getName(), false);
                plugin.getScoreboardManager().setTeamEliminated(team.getName());

                Bukkit.broadcastMessage("§7チーム「" + team.getName() + "」はメンバーがいないため除外されました。");
            }
        }

        // Start all generators
        plugin.getGeneratorManager().startAllGenerators();

        // Start health regeneration for all players
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getHealthRegenListener().startAllRegenTasks();
        }, 40L); // 2 second delay to ensure players are fully loaded

        // Start nametag visibility task (20m distance limit)
        plugin.getNametagVisibilityListener().startVisibilityTask();

        // Start bed destruction timer (35 minutes)
        plugin.getBedDestructionTimer().startTimer();

        // Start alarm trap detection task
        plugin.getAlarmTrapManager().startAlarmTask();

        // Preload all team spawn chunks before teleporting
        preloadTeamSpawnChunks();

        // Teleport players to their team spawns and give initial equipment
        teleportAndEquipPlayers();

        // Apply team colors to player names
        plugin.getTeamColorManager().applyTeamColors();

        // Show start title
        showStartTitle();

        Bukkit.broadcastMessage("§6§lPvPゲームが開始されました！");

        // ゲーム設定をリセット
        reset();
    }

    /**
     * プレイヤーを選択したチームに割り当て
     */
    private void assignPlayersToSelectedTeams() {
        for (Map.Entry<UUID, String> entry : playerTeamSelection.entrySet()) {
            UUID playerUUID = entry.getKey();
            String teamName = entry.getValue();

            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                Team team = plugin.getGameManager().getTeam(teamName);
                if (team != null) {
                    team.addMember(playerUUID);
                    plugin.getGameManager().setPlayerTeam(playerUUID, teamName);
                    plugin.getLogger().info("プレイヤー「" + player.getName() + "」をチーム「" + teamName + "」に割り当てました");
                }
            }
        }
    }

    /**
     * チームスポーンのチャンクをプリロード
     */
    private void preloadTeamSpawnChunks() {
        int loadedTeams = 0;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            org.bukkit.Location spawnLoc = team.getSpawnLocation();
            if (spawnLoc != null && spawnLoc.getWorld() != null) {
                // Load 3x3 chunk area around spawn location
                int centerChunkX = spawnLoc.getBlockX() >> 4;
                int centerChunkZ = spawnLoc.getBlockZ() >> 4;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        org.bukkit.Chunk chunk = spawnLoc.getWorld().getChunkAt(centerChunkX + dx, centerChunkZ + dz);
                        chunk.load(true);
                        chunk.setForceLoaded(true);
                    }
                }
                loadedTeams++;
            }
        }
        plugin.getLogger().info("チャンクプリロード完了: " + loadedTeams + " チーム");
    }

    /**
     * プレイヤーをテレポートして装備を付与
     */
    private void teleportAndEquipPlayers() {
        int playerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

            plugin.getGameManager().teleportPlayerToTeamSpawn(player);

            // Give initial equipment
            giveInitialEquipment(player, teamName);

            // Create scoreboard for player
            plugin.getScoreboardManager().createScoreboard(player);

            player.sendMessage("§aゲーム開始！あなたは「" + teamName + "」チームです。");

            // Play start sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            playerCount++;
        }
        plugin.getLogger().info("プレイヤー初期化完了: " + playerCount + " 人");
    }

    /**
     * 初期装備を付与
     */
    private void giveInitialEquipment(Player player, String teamName) {
        // Clear inventory
        player.getInventory().clear();

        // Get team
        Team team = plugin.getGameManager().getTeam(teamName);
        if (team == null) return;

        // Get team color
        Color armorColor = getTeamColor(teamName);

        // Give wooden sword
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        player.getInventory().addItem(sword);

        // Create and equip colored leather armor
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, armorColor);
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, armorColor);
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, armorColor);
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, armorColor);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        // デバッグモード時はデバッグコンパスを渡す
        if (plugin.getGameManager().isDebugMode()) {
            plugin.getDebugBook().giveCompass(player);
        }
    }

    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            meta.setUnbreakable(true); // Make armor unbreakable
            armor.setItemMeta(meta);
        }
        return armor;
    }

    private Color getTeamColor(String teamName) {
        switch (teamName) {
            case "レッド": return Color.RED;
            case "ブルー": return Color.BLUE;
            case "グリーン": return Color.GREEN;
            case "イエロー": return Color.YELLOW;
            case "アクア": return Color.AQUA;
            case "ホワイト": return Color.WHITE;
            case "ピンク": return Color.FUCHSIA;
            case "グレー": return Color.GRAY;
            default: return Color.WHITE;
        }
    }

    /**
     * ゲームワールドのチェストとプレイヤーのエンダーチェストをクリア
     */
    private void clearAllChests() {
        int chestCount = 0;

        // Clear chests in game world only
        org.bukkit.World gameWorld = Bukkit.getWorld("world");
        if (gameWorld != null) {
            for (org.bukkit.Chunk chunk : gameWorld.getLoadedChunks()) {
                for (org.bukkit.block.BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof org.bukkit.block.Chest) {
                        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) blockState;
                        chest.getInventory().clear();
                        chestCount++;
                    }
                }
            }
        }

        // Clear all player ender chest inventories
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getEnderChest().clear();
        }

        plugin.getLogger().info("Cleared " + chestCount + " chests and " +
            Bukkit.getOnlinePlayers().size() + " player ender chests");
    }

    /**
     * ゲーム開始タイトルを表示
     */
    private void showStartTitle() {
        Component startTitle = Component.text("§a§lゲーム開始！");
        Component startSubtitle = Component.text("§7Good Luck!");

        Title gameStartTitle = Title.title(
            startTitle,
            startSubtitle,
            Title.Times.times(
                Duration.ofMillis(500),  // fade in
                Duration.ofMillis(2000), // stay
                Duration.ofMillis(500)   // fade out
            )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(gameStartTitle);
        }
    }

    /**
     * ゲーム設定をリセット
     */
    public void reset() {
        playerTeamSelection.clear();
        initializeTeamLimits();
    }
}
