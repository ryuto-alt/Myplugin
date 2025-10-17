package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ShopGUI {
    private final PvPGame plugin;

    // Team name to wool color mapping
    private static final Map<String, Material> TEAM_WOOL_MAP = new HashMap<>();
    private static final Map<String, Material> TEAM_GLASS_MAP = new HashMap<>();

    static {
        TEAM_WOOL_MAP.put("アクア", Material.CYAN_WOOL);
        TEAM_WOOL_MAP.put("イエロー", Material.YELLOW_WOOL);
        TEAM_WOOL_MAP.put("ブルー", Material.BLUE_WOOL);
        TEAM_WOOL_MAP.put("ホワイト", Material.WHITE_WOOL);
        TEAM_WOOL_MAP.put("グレー", Material.GRAY_WOOL);
        TEAM_WOOL_MAP.put("ピンク", Material.PINK_WOOL);
        TEAM_WOOL_MAP.put("グリーン", Material.GREEN_WOOL);
        TEAM_WOOL_MAP.put("レッド", Material.RED_WOOL);

        TEAM_GLASS_MAP.put("アクア", Material.CYAN_STAINED_GLASS);
        TEAM_GLASS_MAP.put("イエロー", Material.YELLOW_STAINED_GLASS);
        TEAM_GLASS_MAP.put("ブルー", Material.BLUE_STAINED_GLASS);
        TEAM_GLASS_MAP.put("ホワイト", Material.WHITE_STAINED_GLASS);
        TEAM_GLASS_MAP.put("グレー", Material.GRAY_STAINED_GLASS);
        TEAM_GLASS_MAP.put("ピンク", Material.PINK_STAINED_GLASS);
        TEAM_GLASS_MAP.put("グリーン", Material.GREEN_STAINED_GLASS);
        TEAM_GLASS_MAP.put("レッド", Material.RED_STAINED_GLASS);
    }

    public ShopGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    public void openMainShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "§6§lショップ - メイン");

        // Get player's team
        String playerTeamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        Material woolType = Material.WHITE_WOOL;
        String woolColorName = "白";

        if (playerTeamName != null) {
            woolType = TEAM_WOOL_MAP.getOrDefault(playerTeamName, Material.WHITE_WOOL);
            woolColorName = getWoolColorName(playerTeamName);
        }

        // Row 1: Category buttons
        // Blocks category
        ItemStack blocks = new ItemStack(Material.WHITE_WOOL);
        ItemMeta blocksMeta = blocks.getItemMeta();
        if (blocksMeta != null) {
            blocksMeta.setDisplayName("§e§lブロック");
            blocksMeta.setLore(Arrays.asList(
                "§7羊毛、エンドストーン、その他",
                "§aクリックして詳細を表示"
            ));
            blocks.setItemMeta(blocksMeta);
        }
        inv.setItem(10, blocks);

        // Equipment category
        ItemStack equipment = new ItemStack(Material.IRON_SWORD);
        ItemMeta equipmentMeta = equipment.getItemMeta();
        if (equipmentMeta != null) {
            equipmentMeta.setDisplayName("§c§l装備");
            equipmentMeta.setLore(Arrays.asList(
                "§7剣、防具、弓など",
                "§aクリックして詳細を表示"
            ));
            equipment.setItemMeta(equipmentMeta);
        }
        inv.setItem(12, equipment);

        // Enhancement category
        ItemStack enhancement = new ItemStack(Material.POTION);
        ItemMeta enhancementMeta = enhancement.getItemMeta();
        if (enhancementMeta != null) {
            enhancementMeta.setDisplayName("§d§l強化");
            enhancementMeta.setLore(Arrays.asList(
                "§7透明化、跳躍力上昇など",
                "§aクリックして詳細を表示"
            ));
            enhancement.setItemMeta(enhancementMeta);
        }
        inv.setItem(14, enhancement);

        // Tools category
        ItemStack tools = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta toolsMeta = tools.getItemMeta();
        if (toolsMeta != null) {
            toolsMeta.setDisplayName("§6§l道具");
            toolsMeta.setLore(Arrays.asList(
                "§7金リンゴ、TNT、道具など",
                "§aクリックして詳細を表示"
            ));
            tools.setItemMeta(toolsMeta);
        }
        inv.setItem(16, tools);

        // Row 3 (skip Row 2 for spacing): Quick buy items
        // Quick buy: Wool x16 (below blocks)
        ItemStack quickWool = new ItemStack(woolType, 16);
        ItemMeta quickWoolMeta = quickWool.getItemMeta();
        if (quickWoolMeta != null) {
            quickWoolMeta.setDisplayName("§f" + woolColorName + "の羊毛");
            quickWoolMeta.setLore(Arrays.asList(
                "§7コスト: §f鉄 4個",
                "",
                "§eクリックして購入！"
            ));
            quickWool.setItemMeta(quickWoolMeta);
        }
        inv.setItem(28, quickWool);

        // Row 4: Quick buy: Oak Planks x10 (below wool)
        ItemStack quickPlanks = new ItemStack(Material.OAK_PLANKS, 10);
        ItemMeta quickPlanksMeta = quickPlanks.getItemMeta();
        if (quickPlanksMeta != null) {
            quickPlanksMeta.setDisplayName("§fオークの木材");
            quickPlanksMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 6個",
                "",
                "§eクリックして購入！"
            ));
            quickPlanks.setItemMeta(quickPlanksMeta);
        }
        inv.setItem(37, quickPlanks);

        // Row 3: Quick buy: Stone Sword (below equipment)
        ItemStack stoneSword = new ItemStack(Material.STONE_SWORD);
        ItemMeta stoneSwordMeta = stoneSword.getItemMeta();
        if (stoneSwordMeta != null) {
            stoneSwordMeta.setDisplayName("§f石の剣");
            stoneSwordMeta.setLore(Arrays.asList(
                "§7コスト: §f鉄 10個",
                "",
                "§eクリックして購入！"
            ));
            stoneSword.setItemMeta(stoneSwordMeta);
        }
        inv.setItem(30, stoneSword);

        // Row 4: Quick buy: Iron Sword (below stone sword)
        ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
        ItemMeta ironSwordMeta = ironSword.getItemMeta();
        if (ironSwordMeta != null) {
            ironSwordMeta.setDisplayName("§f鉄の剣");
            ironSwordMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 7個",
                "",
                "§eクリックして購入！"
            ));
            ironSword.setItemMeta(ironSwordMeta);
        }
        inv.setItem(39, ironSword);

        // Row 3: Quick buy: Invisibility Potion (below enhancement)
        ItemStack invisPotion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta invisMeta = (org.bukkit.inventory.meta.PotionMeta) invisPotion.getItemMeta();
        if (invisMeta != null) {
            invisMeta.setDisplayName("§f透明化のポーション");
            invisMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 2個",
                "§7効果時間: 30秒",
                "",
                "§eクリックして購入！"
            ));
            invisMeta.setColor(org.bukkit.Color.fromRGB(127, 127, 127));
            invisPotion.setItemMeta(invisMeta);
        }
        inv.setItem(32, invisPotion);

        // Row 4: Quick buy: Jump Boost Potion (below invisibility)
        ItemStack jumpPotion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta jumpMeta = (org.bukkit.inventory.meta.PotionMeta) jumpPotion.getItemMeta();
        if (jumpMeta != null) {
            jumpMeta.setDisplayName("§f跳躍力上昇のポーション");
            jumpMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 1個",
                "§7効果時間: 1分間",
                "",
                "§eクリックして購入！"
            ));
            jumpMeta.setColor(org.bukkit.Color.fromRGB(34, 255, 76));
            jumpPotion.setItemMeta(jumpMeta);
        }
        inv.setItem(41, jumpPotion);

        // Row 3: Quick buy: Golden Apple (below tools)
        ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta goldenAppleMeta = goldenApple.getItemMeta();
        if (goldenAppleMeta != null) {
            goldenAppleMeta.setDisplayName("§6金リンゴ");
            goldenAppleMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 3個",
                "",
                "§eクリックして購入！"
            ));
            goldenApple.setItemMeta(goldenAppleMeta);
        }
        inv.setItem(34, goldenApple);

        // Row 4: Quick buy: TNT (below golden apple)
        ItemStack tntItem = new ItemStack(Material.TNT);
        ItemMeta tntItemMeta = tntItem.getItemMeta();
        if (tntItemMeta != null) {
            tntItemMeta.setDisplayName("§cTNT");
            tntItemMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 5個",
                "",
                "§eクリックして購入！"
            ));
            tntItem.setItemMeta(tntItemMeta);
        }
        inv.setItem(43, tntItem);

        player.openInventory(inv);
    }

    public void openBlocksShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§e§lブロック");

        // Get player's team (購入者のチーム)
        String playerTeamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        Material woolType = Material.WHITE_WOOL;
        Material glassType = Material.WHITE_STAINED_GLASS;
        String woolColorName = "白";

        if (playerTeamName != null) {
            woolType = TEAM_WOOL_MAP.getOrDefault(playerTeamName, Material.WHITE_WOOL);
            glassType = TEAM_GLASS_MAP.getOrDefault(playerTeamName, Material.WHITE_STAINED_GLASS);
            woolColorName = getWoolColorName(playerTeamName);
        }

        // Fill with light gray glass panes for decoration
        ItemStack grayPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayPane.getItemMeta();
        if (grayMeta != null) {
            grayMeta.setDisplayName(" ");
            grayPane.setItemMeta(grayMeta);
        }

        // Fill all empty slots with glass panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Row 2: First row of items (evenly spaced)
        // Wool 16 pieces for 4 iron
        ItemStack wool16 = new ItemStack(woolType, 16);
        ItemMeta wool16Meta = wool16.getItemMeta();
        if (wool16Meta != null) {
            wool16Meta.setDisplayName("§f" + woolColorName + "の羊毛");
            wool16Meta.setLore(Arrays.asList(
                "§7コスト: §f鉄 4個",
                "",
                "§eクリックして購入！"
            ));
            wool16.setItemMeta(wool16Meta);
        }
        inv.setItem(11, wool16);

        // Oak Planks x10 for gold 6
        ItemStack planks = new ItemStack(Material.OAK_PLANKS, 10);
        ItemMeta planksMeta = planks.getItemMeta();
        if (planksMeta != null) {
            planksMeta.setDisplayName("§fオークの木材");
            planksMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 6個",
                "",
                "§eクリックして購入！"
            ));
            planks.setItemMeta(planksMeta);
        }
        inv.setItem(13, planks);

        // End Stone x12 for iron 24
        ItemStack endStone = new ItemStack(Material.END_STONE, 12);
        ItemMeta endStoneMeta = endStone.getItemMeta();
        if (endStoneMeta != null) {
            endStoneMeta.setDisplayName("§fエンドストーン");
            endStoneMeta.setLore(Arrays.asList(
                "§7コスト: §f鉄 24個",
                "",
                "§eクリックして購入！"
            ));
            endStone.setItemMeta(endStoneMeta);
        }
        inv.setItem(15, endStone);

        // Row 4: Second row of items (evenly spaced)
        // Team colored Glass x4 for gold 10
        ItemStack glass = new ItemStack(glassType, 4);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName("§f" + woolColorName + "のガラス");
            glassMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 10個",
                "",
                "§eクリックして購入！"
            ));
            glass.setItemMeta(glassMeta);
        }
        inv.setItem(29, glass);

        // Obsidian x4 for emerald 6
        ItemStack obsidian = new ItemStack(Material.OBSIDIAN, 4);
        ItemMeta obsidianMeta = obsidian.getItemMeta();
        if (obsidianMeta != null) {
            obsidianMeta.setDisplayName("§f黒曜石");
            obsidianMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 6個",
                "",
                "§eクリックして購入！"
            ));
            obsidian.setItemMeta(obsidianMeta);
        }
        inv.setItem(31, obsidian);

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList(
                "§7メインショップに戻る"
            ));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(49, backButton);

        player.openInventory(inv);
    }

    public void openEquipmentShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§c§l装備ショップ");

        // Fill with light gray glass panes for decoration
        ItemStack grayPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayPane.getItemMeta();
        if (grayMeta != null) {
            grayMeta.setDisplayName(" ");
            grayPane.setItemMeta(grayMeta);
        }

        // Fill all empty slots with glass panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Row 2: Swords (evenly spaced)
        // Stone Sword
        ItemStack stoneSword = new ItemStack(Material.STONE_SWORD);
        ItemMeta stoneSwordMeta = stoneSword.getItemMeta();
        if (stoneSwordMeta != null) {
            stoneSwordMeta.setDisplayName("§f石の剣");
            stoneSwordMeta.setLore(Arrays.asList(
                "§7コスト: §f鉄 10個",
                "",
                "§eクリックして購入！"
            ));
            stoneSword.setItemMeta(stoneSwordMeta);
        }
        inv.setItem(10, stoneSword);

        // Iron Sword
        ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
        ItemMeta ironSwordMeta = ironSword.getItemMeta();
        if (ironSwordMeta != null) {
            ironSwordMeta.setDisplayName("§f鉄の剣");
            ironSwordMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 7個",
                "",
                "§eクリックして購入！"
            ));
            ironSword.setItemMeta(ironSwordMeta);
        }
        inv.setItem(12, ironSword);

        // Diamond Sword
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta diamondSwordMeta = diamondSword.getItemMeta();
        if (diamondSwordMeta != null) {
            diamondSwordMeta.setDisplayName("§fダイヤの剣");
            diamondSwordMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 3個",
                "",
                "§eクリックして購入！"
            ));
            diamondSword.setItemMeta(diamondSwordMeta);
        }
        inv.setItem(14, diamondSword);

        // Netherite Sword
        ItemStack netheriteSword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta netheriteSwordMeta = netheriteSword.getItemMeta();
        if (netheriteSwordMeta != null) {
            netheriteSwordMeta.setDisplayName("§fネザライトの剣");
            netheriteSwordMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 7個",
                "",
                "§eクリックして購入！"
            ));
            netheriteSword.setItemMeta(netheriteSwordMeta);
        }
        inv.setItem(16, netheriteSword);

        // Row 3: Armor (evenly spaced)
        // Chainmail Armor (boots icon) - Below iron armor
        ItemStack chainArmor = new ItemStack(Material.CHAINMAIL_BOOTS);
        ItemMeta chainArmorMeta = chainArmor.getItemMeta();
        if (chainArmorMeta != null) {
            chainArmorMeta.setDisplayName("§fチェーンの装備");
            chainArmorMeta.setLore(Arrays.asList(
                "§7コスト: §f鉄 40個",
                "§7レギンスとブーツを装備",
                "",
                "§eクリックして購入！"
            ));
            chainArmor.setItemMeta(chainArmorMeta);
        }
        inv.setItem(19, chainArmor);

        // Iron Armor (boots icon)
        ItemStack ironArmor = new ItemStack(Material.IRON_BOOTS);
        ItemMeta ironArmorMeta = ironArmor.getItemMeta();
        if (ironArmorMeta != null) {
            ironArmorMeta.setDisplayName("§f鉄の装備");
            ironArmorMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 12個",
                "§7レギンスとブーツを装備",
                "",
                "§eクリックして購入！"
            ));
            ironArmor.setItemMeta(ironArmorMeta);
        }
        inv.setItem(21, ironArmor);

        // Diamond Armor (boots icon)
        ItemStack diamondArmor = new ItemStack(Material.DIAMOND_BOOTS);
        ItemMeta diamondArmorMeta = diamondArmor.getItemMeta();
        if (diamondArmorMeta != null) {
            diamondArmorMeta.setDisplayName("§fダイヤの装備");
            diamondArmorMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 6個",
                "§7レギンスとブーツを装備",
                "",
                "§eクリックして購入！"
            ));
            diamondArmor.setItemMeta(diamondArmorMeta);
        }
        inv.setItem(23, diamondArmor);

        // Netherite Armor (boots icon)
        ItemStack netheriteArmor = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta netheriteArmorMeta = netheriteArmor.getItemMeta();
        if (netheriteArmorMeta != null) {
            netheriteArmorMeta.setDisplayName("§fネザライトの装備");
            netheriteArmorMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 24個",
                "§7レギンスとブーツを装備",
                "",
                "§eクリックして購入！"
            ));
            netheriteArmor.setItemMeta(netheriteArmorMeta);
        }
        inv.setItem(25, netheriteArmor);

        // Row 4: Bows and Arrows (evenly spaced)
        // Bow
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        if (bowMeta != null) {
            bowMeta.setDisplayName("§f弓");
            bowMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 10個",
                "",
                "§eクリックして購入！"
            ));
            bow.setItemMeta(bowMeta);
        }
        inv.setItem(29, bow);

        // Bow with Knockback
        ItemStack bowKnockback = new ItemStack(Material.BOW);
        ItemMeta bowKnockbackMeta = bowKnockback.getItemMeta();
        if (bowKnockbackMeta != null) {
            bowKnockbackMeta.setDisplayName("§f弓 §7(ノックバック)");
            bowKnockbackMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 24個",
                "§9ノックバック I",
                "",
                "§eクリックして購入！"
            ));
            bowKnockbackMeta.addEnchant(org.bukkit.enchantments.Enchantment.PUNCH, 1, true);
            bowKnockback.setItemMeta(bowKnockbackMeta);
        }
        inv.setItem(31, bowKnockback);

        // Arrows
        ItemStack arrows = new ItemStack(Material.ARROW, 8);
        ItemMeta arrowsMeta = arrows.getItemMeta();
        if (arrowsMeta != null) {
            arrowsMeta.setDisplayName("§f矢 x8");
            arrowsMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 2個",
                "",
                "§eクリックして購入！"
            ));
            arrows.setItemMeta(arrowsMeta);
        }
        inv.setItem(33, arrows);

        // Row 5: Special items (evenly spaced)
        // Infinity Bow
        ItemStack infinityBow = new ItemStack(Material.BOW);
        ItemMeta infinityBowMeta = infinityBow.getItemMeta();
        if (infinityBowMeta != null) {
            infinityBowMeta.setDisplayName("§f矢 §7(無限)");
            infinityBowMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 1個",
                "§9無限 - 矢が1本あれば無限に使用可能",
                "",
                "§eクリックして購入！"
            ));
            infinityBowMeta.addEnchant(org.bukkit.enchantments.Enchantment.INFINITY, 1, true);
            infinityBow.setItemMeta(infinityBowMeta);
        }
        inv.setItem(38, infinityBow);

        // Knockback Stick
        ItemStack knockbackStick = new ItemStack(Material.STICK);
        ItemMeta knockbackStickMeta = knockbackStick.getItemMeta();
        if (knockbackStickMeta != null) {
            knockbackStickMeta.setDisplayName("§fノックバック棒");
            knockbackStickMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 8個",
                "§9ノックバック III",
                "",
                "§eクリックして購入！"
            ));
            knockbackStickMeta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 3, true);
            knockbackStick.setItemMeta(knockbackStickMeta);
        }
        inv.setItem(40, knockbackStick);

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList(
                "§7メインショップに戻る"
            ));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(49, backButton);

        player.openInventory(inv);
    }

    public void openEnhancementShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§d§l強化");

        // Fill with light gray glass panes for decoration
        ItemStack grayPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayPane.getItemMeta();
        if (grayMeta != null) {
            grayMeta.setDisplayName(" ");
            grayPane.setItemMeta(grayMeta);
        }

        // Fill all empty slots with glass panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Row 2: Potions (evenly spaced)
        // Invisibility Potion (30 seconds)
        ItemStack invisPotion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta invisMeta = (org.bukkit.inventory.meta.PotionMeta) invisPotion.getItemMeta();
        if (invisMeta != null) {
            invisMeta.setDisplayName("§f透明化のポーション");
            invisMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 2個",
                "§7効果時間: 30秒",
                "",
                "§eクリックして購入！"
            ));
            invisMeta.setColor(org.bukkit.Color.fromRGB(127, 127, 127));
            invisPotion.setItemMeta(invisMeta);
        }
        inv.setItem(11, invisPotion);

        // Jump Boost Potion (1 minute)
        ItemStack jumpPotion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta jumpMeta = (org.bukkit.inventory.meta.PotionMeta) jumpPotion.getItemMeta();
        if (jumpMeta != null) {
            jumpMeta.setDisplayName("§f跳躍力上昇のポーション");
            jumpMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 1個",
                "§7効果時間: 1分間",
                "",
                "§eクリックして購入！"
            ));
            jumpMeta.setColor(org.bukkit.Color.fromRGB(34, 255, 76));
            jumpPotion.setItemMeta(jumpMeta);
        }
        inv.setItem(13, jumpPotion);

        // Speed Potion
        ItemStack speedPotion = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta speedMeta = (org.bukkit.inventory.meta.PotionMeta) speedPotion.getItemMeta();
        if (speedMeta != null) {
            speedMeta.setDisplayName("§f移動速度上昇のポーション");
            speedMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 1個",
                "§7効果時間: 1分間",
                "",
                "§eクリックして購入！"
            ));
            speedMeta.setColor(org.bukkit.Color.fromRGB(124, 175, 176));
            speedPotion.setItemMeta(speedMeta);
        }
        inv.setItem(15, speedPotion);

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList(
                "§7メインショップに戻る"
            ));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(49, backButton);

        player.openInventory(inv);
    }

    public void openToolsShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l道具");

        // Fill with light gray glass panes for decoration
        ItemStack grayPane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayPane.getItemMeta();
        if (grayMeta != null) {
            grayMeta.setDisplayName(" ");
            grayPane.setItemMeta(grayMeta);
        }

        // Fill all empty slots with glass panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Row 2: First row of items (evenly spaced)
        // Golden Apple (non-enchanted)
        ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta goldenAppleMeta = goldenApple.getItemMeta();
        if (goldenAppleMeta != null) {
            goldenAppleMeta.setDisplayName("§6金リンゴ");
            goldenAppleMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 3個",
                "",
                "§eクリックして購入！"
            ));
            goldenApple.setItemMeta(goldenAppleMeta);
        }
        inv.setItem(11, goldenApple);

        // Shears
        ItemStack shears = new ItemStack(Material.SHEARS);
        ItemMeta shearsMeta = shears.getItemMeta();
        if (shearsMeta != null) {
            shearsMeta.setDisplayName("§fハサミ");
            shearsMeta.setLore(Arrays.asList(
                "§7コスト: §f鉄 20個",
                "",
                "§eクリックして購入！"
            ));
            shears.setItemMeta(shearsMeta);
        }
        inv.setItem(12, shears);

        // Water Bucket
        ItemStack waterBucket = new ItemStack(Material.WATER_BUCKET);
        ItemMeta waterBucketMeta = waterBucket.getItemMeta();
        if (waterBucketMeta != null) {
            waterBucketMeta.setDisplayName("§b水入りバケツ");
            waterBucketMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 4個",
                "§7使用後はバケツごと消えます",
                "",
                "§eクリックして購入！"
            ));
            waterBucket.setItemMeta(waterBucketMeta);
        }
        inv.setItem(13, waterBucket);

        // TNT
        ItemStack tnt = new ItemStack(Material.TNT);
        ItemMeta tntMeta = tnt.getItemMeta();
        if (tntMeta != null) {
            tntMeta.setDisplayName("§cTNT");
            tntMeta.setLore(Arrays.asList(
                "§7コスト: §6ゴールド 5個",
                "",
                "§eクリックして購入！"
            ));
            tnt.setItemMeta(tntMeta);
        }
        inv.setItem(14, tnt);

        // Ender Pearl
        ItemStack enderPearl = new ItemStack(Material.ENDER_PEARL);
        ItemMeta enderPearlMeta = enderPearl.getItemMeta();
        if (enderPearlMeta != null) {
            enderPearlMeta.setDisplayName("§5エンダーパール");
            enderPearlMeta.setLore(Arrays.asList(
                "§7コスト: §aエメラルド 4個",
                "",
                "§eクリックして購入！"
            ));
            enderPearl.setItemMeta(enderPearlMeta);
        }
        inv.setItem(16, enderPearl);

        // Row 3: Tools upgrade items (axes) - Show only next level
        int currentAxeLevel = plugin.getToolUpgradeManager().getAxeLevel(player.getUniqueId());
        int nextAxeLevel = currentAxeLevel + 1;

        if (nextAxeLevel <= 4) {
            Material axeMaterial;
            String axeName;
            Material currency;
            int cost;

            switch (nextAxeLevel) {
                case 1:
                    axeMaterial = Material.WOODEN_AXE;
                    axeName = "§f木の斧";
                    currency = Material.IRON_INGOT;
                    cost = 12;
                    break;
                case 2:
                    axeMaterial = Material.STONE_AXE;
                    axeName = "§f石の斧";
                    currency = Material.IRON_INGOT;
                    cost = 24;
                    break;
                case 3:
                    axeMaterial = Material.IRON_AXE;
                    axeName = "§f鉄の斧";
                    currency = Material.GOLD_INGOT;
                    cost = 8;
                    break;
                case 4:
                default:
                    axeMaterial = Material.DIAMOND_AXE;
                    axeName = "§fダイヤの斧";
                    currency = Material.GOLD_INGOT;
                    cost = 16;
                    break;
            }

            ItemStack axe = new ItemStack(axeMaterial);
            ItemMeta axeMeta = axe.getItemMeta();
            if (axeMeta != null) {
                axeMeta.setDisplayName(axeName);
                String currencyName = currency == Material.IRON_INGOT ? "§f鉄" : "§6ゴールド";
                axeMeta.setLore(Arrays.asList(
                    "§7コスト: " + currencyName + " " + cost + "個",
                    "§7アップグレード: レベル " + nextAxeLevel,
                    "",
                    "§eクリックして購入！"
                ));
                axe.setItemMeta(axeMeta);
            }
            inv.setItem(22, axe);
        }

        // Row 4: Tools upgrade items (pickaxes) - Show only next level
        int currentPickaxeLevel = plugin.getToolUpgradeManager().getPickaxeLevel(player.getUniqueId());
        int nextPickaxeLevel = currentPickaxeLevel + 1;

        if (nextPickaxeLevel <= 4) {
            Material pickaxeMaterial;
            String pickaxeName;
            Material currency;
            int cost;

            switch (nextPickaxeLevel) {
                case 1:
                    pickaxeMaterial = Material.WOODEN_PICKAXE;
                    pickaxeName = "§f木のツルハシ";
                    currency = Material.IRON_INGOT;
                    cost = 12;
                    break;
                case 2:
                    pickaxeMaterial = Material.STONE_PICKAXE;
                    pickaxeName = "§f石のツルハシ";
                    currency = Material.IRON_INGOT;
                    cost = 24;
                    break;
                case 3:
                    pickaxeMaterial = Material.IRON_PICKAXE;
                    pickaxeName = "§f鉄のツルハシ";
                    currency = Material.GOLD_INGOT;
                    cost = 8;
                    break;
                case 4:
                default:
                    pickaxeMaterial = Material.DIAMOND_PICKAXE;
                    pickaxeName = "§fダイヤのツルハシ";
                    currency = Material.GOLD_INGOT;
                    cost = 16;
                    break;
            }

            ItemStack pickaxe = new ItemStack(pickaxeMaterial);
            ItemMeta pickaxeMeta = pickaxe.getItemMeta();
            if (pickaxeMeta != null) {
                pickaxeMeta.setDisplayName(pickaxeName);
                String currencyName = currency == Material.IRON_INGOT ? "§f鉄" : "§6ゴールド";
                pickaxeMeta.setLore(Arrays.asList(
                    "§7コスト: " + currencyName + " " + cost + "個",
                    "§7アップグレード: レベル " + nextPickaxeLevel,
                    "",
                    "§eクリックして購入！"
                ));
                pickaxe.setItemMeta(pickaxeMeta);
            }
            inv.setItem(31, pickaxe);
        }

        // Row 5: Fireball item (centered)
        // Fireball (fire charge that shoots fireballs on right-click)
        ItemStack fireball = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta fireballMeta = fireball.getItemMeta();
        if (fireballMeta != null) {
            fireballMeta.setDisplayName("§c火玉");
            fireballMeta.setLore(Arrays.asList(
                "§7コスト: §f鉄 60個",
                "§7右クリックで火の玉を発射",
                "",
                "§eクリックして購入！"
            ));
            fireball.setItemMeta(fireballMeta);
        }
        inv.setItem(40, fireball);

        // Back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l戻る");
            backMeta.setLore(Arrays.asList(
                "§7メインショップに戻る"
            ));
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(49, backButton);

        player.openInventory(inv);
    }

    public Material getTeamWool(String teamName) {
        return TEAM_WOOL_MAP.getOrDefault(teamName, Material.WHITE_WOOL);
    }

    private String getWoolColorName(String teamName) {
        switch (teamName) {
            case "アクア": return "水色";
            case "イエロー": return "黄色";
            case "ブルー": return "青";
            case "ホワイト": return "白";
            case "グレー": return "灰色";
            case "ピンク": return "ピンク";
            case "グリーン": return "緑";
            case "レッド": return "赤";
            default: return "白";
        }
    }
}
