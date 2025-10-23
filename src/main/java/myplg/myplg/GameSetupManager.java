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
                // Title display
                Component title = Component.text("§e§l" + count);
                Component subtitle = Component.text("§7ゲーム開始まで...");

                Title countTitle = Title.title(
                    title,
                    subtitle,
                    Title.Times.times(
                        Duration.ofMillis(0),    // fade in
                        Duration.ofMillis(1000), // stay
                        Duration.ofMillis(250)   // fade out
                    )
                );

                // Broadcast and show title to all players
                Bukkit.broadcastMessage("§e§l" + count + "...");

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.showTitle(countTitle);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, delay);
        }

        // Start game after countdown (at 6 seconds = 120 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, this::startGame, 120L);
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

        // Wait a bit to ensure chunks are fully loaded
        try {
            Thread.sleep(1000); // 1 second wait
            plugin.getLogger().info("チャンク安定化待機完了");
        } catch (InterruptedException e) {
            plugin.getLogger().severe("待機中にエラー: " + e.getMessage());
        }

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
        plugin.getLogger().info("===== チームスポーンのチャンクをプリロード開始 =====");
        for (Team team : plugin.getGameManager().getTeams().values()) {
            org.bukkit.Location spawnLoc = team.getSpawnLocation();
            if (spawnLoc != null) {
                plugin.getLogger().info("チーム「" + team.getName() + "」のスポーン座標:");
                plugin.getLogger().info("  World: " + (spawnLoc.getWorld() != null ? spawnLoc.getWorld().getName() : "null"));
                plugin.getLogger().info("  X: " + spawnLoc.getX() + ", Y: " + spawnLoc.getY() + ", Z: " + spawnLoc.getZ());
                plugin.getLogger().info("  Chunk: (" + (spawnLoc.getBlockX() >> 4) + ", " + (spawnLoc.getBlockZ() >> 4) + ")");

                if (spawnLoc.getWorld() != null) {
                    // Load 3x3 chunk area around spawn location for safety
                    int centerChunkX = spawnLoc.getBlockX() >> 4;
                    int centerChunkZ = spawnLoc.getBlockZ() >> 4;

                    plugin.getLogger().info("  ロード開始: 3x3チャンク範囲");
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            org.bukkit.Chunk chunk = spawnLoc.getWorld().getChunkAt(centerChunkX + dx, centerChunkZ + dz);
                            chunk.load(true);
                            chunk.setForceLoaded(true); // Keep chunk loaded
                            plugin.getLogger().info("    チャンク (" + (centerChunkX + dx) + ", " + (centerChunkZ + dz) + ") ロード完了");
                        }
                    }
                    plugin.getLogger().info("  チーム「" + team.getName() + "」のチャンクロード完了");
                } else {
                    plugin.getLogger().severe("  エラー: ワールドがnullです！");
                }
            } else {
                plugin.getLogger().warning("チーム「" + team.getName() + "」のスポーン座標がnullです");
            }
        }
        plugin.getLogger().info("===== チャンクプリロード完了 =====");
    }

    /**
     * プレイヤーをテレポートして装備を付与
     */
    private void teleportAndEquipPlayers() {
        plugin.getLogger().info("===== プレイヤーテレポート開始 =====");
        for (Player player : Bukkit.getOnlinePlayers()) {
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            plugin.getLogger().info("プレイヤー「" + player.getName() + "」をチーム「" + teamName + "」にテレポート中...");

            plugin.getGameManager().teleportPlayerToTeamSpawn(player);

            plugin.getLogger().info("  テレポート完了");

            // Give initial equipment
            giveInitialEquipment(player, teamName);

            // Create scoreboard for player
            plugin.getScoreboardManager().createScoreboard(player);

            player.sendMessage("§aゲーム開始！あなたは「" + teamName + "」チームです。");

            // Play start sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            plugin.getLogger().info("プレイヤー「" + player.getName() + "」の初期化完了");
        }
        plugin.getLogger().info("===== プレイヤーテレポート完了 =====");
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
     * すべてのチェストとエンダーチェストをクリア
     */
    private void clearAllChests() {
        int chestCount = 0;
        int enderChestCount = 0;

        // Clear all chests in all worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.block.BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof org.bukkit.block.Chest) {
                        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) blockState;
                        chest.getInventory().clear();
                        chestCount++;
                    } else if (blockState instanceof org.bukkit.block.EnderChest) {
                        enderChestCount++;
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
