package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import myplg.myplg.listeners.BlockPlaceListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class StartCommand implements CommandExecutor {
    private final PvPGame plugin;

    public StartCommand(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("ゲームは既に進行中です。");
            return true;
        }

        if (plugin.getGameManager().getTeams().isEmpty()) {
            sender.sendMessage("チームが設定されていません。先に /setbed でチームを設定してください。");
            return true;
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (onlinePlayers.isEmpty()) {
            sender.sendMessage("参加可能なプレイヤーがいません。");
            return true;
        }

        // Start countdown
        startCountdown(onlinePlayers);

        return true;
    }

    private void startCountdown(List<Player> onlinePlayers) {
        // Countdown: 5, 4, 3, 2, 1
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
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startGame(onlinePlayers);
        }, 120L);
    }

    private void startGame(List<Player> onlinePlayers) {
        // Start the game
        plugin.getGameManager().setGameRunning(true);

        // Clear player-placed blocks tracking from previous game
        BlockPlaceListener.clearPlayerPlacedBlocks();

        // Assign players to teams
        plugin.getGameManager().assignPlayersToTeams(onlinePlayers);

        // Initialize scoreboard
        plugin.getScoreboardManager().initializeAllBeds();

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

        // Teleport players to their team spawns and give initial equipment
        for (Player player : onlinePlayers) {
            plugin.getGameManager().teleportPlayerToTeamSpawn(player);
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

            // Give initial equipment
            giveInitialEquipment(player, teamName);

            // Create scoreboard for player
            plugin.getScoreboardManager().createScoreboard(player);

            player.sendMessage("§aゲーム開始！あなたは「" + teamName + "」チームです。");

            // Play start sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Show start title
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

        Bukkit.broadcastMessage("§6§lPvPゲームが開始されました！");
    }

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
}
