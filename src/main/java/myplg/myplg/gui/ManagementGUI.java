package myplg.myplg.gui;

import myplg.myplg.Generator;
import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ManagementGUI {
    private final PvPGame plugin;

    public ManagementGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("ゲーム管理", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        // Team management button
        ItemStack teamManagement = new ItemStack(Material.WHITE_BED);
        ItemMeta teamMeta = teamManagement.getItemMeta();
        teamMeta.displayName(Component.text("チーム管理", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> teamLore = new ArrayList<>();
        teamLore.add(Component.text("クリックでチーム一覧を表示", NamedTextColor.GRAY));
        teamMeta.lore(teamLore);
        teamManagement.setItemMeta(teamMeta);

        // Generator management button
        ItemStack generatorManagement = new ItemStack(Material.DROPPER);
        ItemMeta generatorMeta = generatorManagement.getItemMeta();
        generatorMeta.displayName(Component.text("ジェネレーター管理", NamedTextColor.AQUA, TextDecoration.BOLD));
        List<Component> generatorLore = new ArrayList<>();
        generatorLore.add(Component.text("クリックでジェネレーター設定を表示", NamedTextColor.GRAY));
        generatorMeta.lore(generatorLore);
        generatorManagement.setItemMeta(generatorMeta);

        gui.setItem(11, teamManagement);
        gui.setItem(15, generatorManagement);

        player.openInventory(gui);
    }

    public void openTeamList(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("チーム一覧 - クリックで名前変更", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        int slot = 0;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            if (slot >= 45) break; // Max 45 teams displayed

            ItemStack teamItem = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = teamItem.getItemMeta();
            meta.displayName(Component.text(team.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("メンバー数: " + team.getMembers().size(), NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("クリックで名前を変更", NamedTextColor.GREEN));
            meta.lore(lore);

            teamItem.setItemMeta(meta);
            gui.setItem(slot, teamItem);
            slot++;
        }

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("戻る", NamedTextColor.RED, TextDecoration.BOLD));
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        player.openInventory(gui);
    }

    public void openGeneratorList(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("ジェネレーター設定", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        int slot = 0;
        for (Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
            if (slot >= 45) break;

            ItemStack generatorItem = new ItemStack(generator.getMaterial());
            ItemMeta meta = generatorItem.getItemMeta();
            meta.displayName(Component.text(getMaterialDisplayName(generator.getMaterial()) + " ジェネレーター",
                    NamedTextColor.YELLOW, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("出現間隔: " + (generator.getSpawnInterval() / 20.0) + "秒", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("左クリック: 間隔を0.5秒短縮", NamedTextColor.GREEN));
            lore.add(Component.text("右クリック: 間隔を0.5秒延長", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift+左クリック: 削除", NamedTextColor.RED));
            meta.lore(lore);

            generatorItem.setItemMeta(meta);
            gui.setItem(slot, generatorItem);
            slot++;
        }

        if (plugin.getGeneratorManager().getGenerators().isEmpty()) {
            ItemStack infoItem = new ItemStack(Material.PAPER);
            ItemMeta infoMeta = infoItem.getItemMeta();
            infoMeta.displayName(Component.text("ジェネレーターがありません", NamedTextColor.GRAY));
            List<Component> infoLore = new ArrayList<>();
            infoLore.add(Component.text("/gene コマンドでジェネレーターを作成してください", NamedTextColor.GRAY));
            infoMeta.lore(infoLore);
            infoItem.setItemMeta(infoMeta);
            gui.setItem(22, infoItem);
        }

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("戻る", NamedTextColor.RED, TextDecoration.BOLD));
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        player.openInventory(gui);
    }

    private String getMaterialDisplayName(Material material) {
        switch (material) {
            case DIAMOND:
                return "ダイヤモンド";
            case GOLD_INGOT:
                return "金インゴット";
            case IRON_INGOT:
                return "鉄インゴット";
            case EMERALD:
                return "エメラルド";
            default:
                return material.name();
        }
    }
}
