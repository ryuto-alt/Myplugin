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

    public void openGeneratorTeamSelection(Player player) {
        plugin.getLogger().info("Opening generator team selection");
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("チーム選択 - ジェネレーター管理", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        // Add "共通" team first
        ItemStack commonTeam = new ItemStack(Material.NETHER_STAR);
        ItemMeta commonMeta = commonTeam.getItemMeta();
        commonMeta.displayName(Component.text("共通", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        List<Component> commonLore = new ArrayList<>();

        int commonCount = 0;
        for (Generator gen : plugin.getGeneratorManager().getGenerators().values()) {
            if (gen.getTeamName().equals("共通")) commonCount++;
        }

        commonLore.add(Component.text("ジェネレーター数: " + commonCount, NamedTextColor.GRAY));
        commonLore.add(Component.text(""));
        commonLore.add(Component.text("クリックで詳細を表示", NamedTextColor.GREEN));
        commonMeta.lore(commonLore);
        commonTeam.setItemMeta(commonMeta);
        gui.setItem(0, commonTeam);
        plugin.getLogger().info("Added 共通 team to slot 0");

        int slot = 1;
        for (Team team : plugin.getGameManager().getTeams().values()) {
            if (slot >= 45) break;

            ItemStack teamItem = new ItemStack(Material.WHITE_BANNER);
            ItemMeta meta = teamItem.getItemMeta();
            meta.displayName(Component.text(team.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();

            // Count generators for this team
            int generatorCount = 0;
            for (Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
                if (generator.getTeamName().equals(team.getName())) {
                    generatorCount++;
                }
            }

            lore.add(Component.text("ジェネレーター数: " + generatorCount, NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("クリックで詳細を表示", NamedTextColor.GREEN));
            meta.lore(lore);

            teamItem.setItemMeta(meta);
            gui.setItem(slot, teamItem);
            plugin.getLogger().info("Added team " + team.getName() + " to slot " + slot);
            slot++;
        }
        plugin.getLogger().info("Total teams added: " + (slot - 1));

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("戻る", NamedTextColor.RED, TextDecoration.BOLD));
        backButton.setItemMeta(backMeta);
        gui.setItem(49, backButton);

        player.openInventory(gui);
    }

    public void openGeneratorListByTeam(Player player, String teamName) {
        plugin.getLogger().info("Opening generator list for team: " + teamName);
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(teamName + " - ジェネレーター", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        int slot = 0;
        for (Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
            if (!generator.getTeamName().equals(teamName)) continue;
            if (slot >= 45) break;
            plugin.getLogger().info("Adding generator to GUI: " + generator.getId());

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

        if (slot == 0) {
            plugin.getLogger().info("No generators found for team: " + teamName);
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

        plugin.getLogger().info("Opening inventory for player: " + player.getName() + ", generators: " + slot);
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

    public void openIntervalSelection(Player player, Material selectedMaterial, String teamName) {
        Inventory gui = Bukkit.createInventory(null, 27,
            Component.text("出現間隔選択", NamedTextColor.DARK_BLUE, TextDecoration.BOLD));

        // 1秒
        ItemStack interval1 = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta meta1 = interval1.getItemMeta();
        meta1.displayName(Component.text("1秒", NamedTextColor.GREEN, TextDecoration.BOLD));
        List<Component> lore1 = new ArrayList<>();
        lore1.add(Component.text("最も速い出現速度", NamedTextColor.GRAY));
        meta1.lore(lore1);
        interval1.setItemMeta(meta1);

        // 3秒
        ItemStack interval3 = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta meta3 = interval3.getItemMeta();
        meta3.displayName(Component.text("3秒", NamedTextColor.GREEN, TextDecoration.BOLD));
        List<Component> lore3 = new ArrayList<>();
        lore3.add(Component.text("速い出現速度", NamedTextColor.GRAY));
        meta3.lore(lore3);
        interval3.setItemMeta(meta3);

        // 5秒
        ItemStack interval5 = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta meta5 = interval5.getItemMeta();
        meta5.displayName(Component.text("5秒", NamedTextColor.YELLOW, TextDecoration.BOLD));
        List<Component> lore5 = new ArrayList<>();
        lore5.add(Component.text("普通の出現速度", NamedTextColor.GRAY));
        meta5.lore(lore5);
        interval5.setItemMeta(meta5);

        // 10秒
        ItemStack interval10 = new ItemStack(Material.ORANGE_CONCRETE);
        ItemMeta meta10 = interval10.getItemMeta();
        meta10.displayName(Component.text("10秒", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Component> lore10 = new ArrayList<>();
        lore10.add(Component.text("遅い出現速度", NamedTextColor.GRAY));
        meta10.lore(lore10);
        interval10.setItemMeta(meta10);

        // 15秒
        ItemStack interval15 = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta15 = interval15.getItemMeta();
        meta15.displayName(Component.text("15秒", NamedTextColor.RED, TextDecoration.BOLD));
        List<Component> lore15 = new ArrayList<>();
        lore15.add(Component.text("最も遅い出現速度", NamedTextColor.GRAY));
        meta15.lore(lore15);
        interval15.setItemMeta(meta15);

        gui.setItem(10, interval1);
        gui.setItem(11, interval3);
        gui.setItem(12, interval5);
        gui.setItem(14, interval10);
        gui.setItem(15, interval15);

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.displayName(Component.text("戻る", NamedTextColor.RED, TextDecoration.BOLD));
        backButton.setItemMeta(backMeta);
        gui.setItem(22, backButton);

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
