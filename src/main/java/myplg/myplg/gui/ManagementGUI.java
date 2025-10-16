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

    public void openGeneratorTypeSelection(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("ジェネレーター作成", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        // Diamond generator
        ItemStack diamondGen = new ItemStack(Material.DIAMOND);
        ItemMeta diamondMeta = diamondGen.getItemMeta();
        diamondMeta.displayName(Component.text("ダイヤモンド ジェネレーター", NamedTextColor.AQUA, TextDecoration.BOLD));
        List<Component> diamondLore = new ArrayList<>();
        diamondLore.add(Component.text("デフォルト間隔: 10秒", NamedTextColor.GRAY));
        diamondLore.add(Component.text(""));
        diamondLore.add(Component.text("クリックして範囲選択を開始", NamedTextColor.GREEN));
        diamondMeta.lore(diamondLore);
        diamondGen.setItemMeta(diamondMeta);

        // Gold generator
        ItemStack goldGen = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldMeta = goldGen.getItemMeta();
        goldMeta.displayName(Component.text("金インゴット ジェネレーター", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> goldLore = new ArrayList<>();
        goldLore.add(Component.text("デフォルト間隔: 5秒", NamedTextColor.GRAY));
        goldLore.add(Component.text(""));
        goldLore.add(Component.text("クリックして範囲選択を開始", NamedTextColor.GREEN));
        goldMeta.lore(goldLore);
        goldGen.setItemMeta(goldMeta);

        // Iron generator
        ItemStack ironGen = new ItemStack(Material.IRON_INGOT);
        ItemMeta ironMeta = ironGen.getItemMeta();
        ironMeta.displayName(Component.text("鉄インゴット ジェネレーター", NamedTextColor.WHITE, TextDecoration.BOLD));
        List<Component> ironLore = new ArrayList<>();
        ironLore.add(Component.text("デフォルト間隔: 3秒", NamedTextColor.GRAY));
        ironLore.add(Component.text(""));
        ironLore.add(Component.text("クリックして範囲選択を開始", NamedTextColor.GREEN));
        ironMeta.lore(ironLore);
        ironGen.setItemMeta(ironMeta);

        // Emerald generator
        ItemStack emeraldGen = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emeraldGen.getItemMeta();
        emeraldMeta.displayName(Component.text("エメラルド ジェネレーター", NamedTextColor.GREEN, TextDecoration.BOLD));
        List<Component> emeraldLore = new ArrayList<>();
        emeraldLore.add(Component.text("デフォルト間隔: 15秒", NamedTextColor.GRAY));
        emeraldLore.add(Component.text(""));
        emeraldLore.add(Component.text("クリックして範囲選択を開始", NamedTextColor.GREEN));
        emeraldMeta.lore(emeraldLore);
        emeraldGen.setItemMeta(emeraldMeta);

        gui.setItem(10, ironGen);
        gui.setItem(12, goldGen);
        gui.setItem(14, diamondGen);
        gui.setItem(16, emeraldGen);

        player.openInventory(gui);
    }

    public void openTeamSelectionForGenerator(Player player, Material selectedMaterial) {
        Inventory gui = Bukkit.createInventory(null, 54,
            Component.text("チーム選択 - " + getMaterialDisplayName(selectedMaterial), NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        int slot = 0;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            if (slot >= 45) break;

            ItemStack teamItem = new ItemStack(Material.WHITE_BANNER);
            ItemMeta meta = teamItem.getItemMeta();
            meta.displayName(Component.text(team.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("このチームのジェネレーターを作成", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("クリックして選択", NamedTextColor.GREEN));
            meta.lore(lore);

            teamItem.setItemMeta(meta);
            gui.setItem(slot, teamItem);
            slot++;
        }

        if (plugin.getGameManager().getTeams().isEmpty()) {
            ItemStack infoItem = new ItemStack(Material.PAPER);
            ItemMeta infoMeta = infoItem.getItemMeta();
            infoMeta.displayName(Component.text("チームがありません", NamedTextColor.GRAY));
            List<Component> infoLore = new ArrayList<>();
            infoLore.add(Component.text("/setbed でチームを作成してください", NamedTextColor.GRAY));
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
