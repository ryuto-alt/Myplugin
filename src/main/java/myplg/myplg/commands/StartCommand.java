package myplg.myplg.commands;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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

        // Start the game
        plugin.getGameManager().setGameRunning(true);

        // Assign players to teams
        plugin.getGameManager().assignPlayersToTeams(onlinePlayers);

        // Initialize scoreboard
        plugin.getScoreboardManager().initializeAllBeds();

        // Start all generators
        plugin.getGeneratorManager().startAllGenerators();

        // Teleport players to their team spawns and give initial equipment
        for (Player player : onlinePlayers) {
            plugin.getGameManager().teleportPlayerToTeamSpawn(player);
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

            // Give initial equipment
            giveInitialEquipment(player, teamName);

            // Create scoreboard for player
            plugin.getScoreboardManager().createScoreboard(player);

            player.sendMessage("§aゲーム開始！あなたは「" + teamName + "」チームです。");
        }

        Bukkit.broadcastMessage("§6§lPvPゲームが開始されました！");

        return true;
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
