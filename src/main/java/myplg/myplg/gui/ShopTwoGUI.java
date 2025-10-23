package myplg.myplg.gui;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ShopTwoGUI {
    private final PvPGame plugin;

    public ShopTwoGUI(PvPGame plugin) {
        this.plugin = plugin;
    }

    public void openMainShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "§6§lアップグレード - メイン");

        // Get player's team to check upgrade levels
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        int territoryLevel = teamName != null ? plugin.getTerritoryUpgradeManager().getUpgradeLevel(teamName) : 0;
        int weaponLevel = teamName != null ? plugin.getWeaponUpgradeManager().getWeaponLevel(teamName) : 0;
        int armorLevel = teamName != null ? plugin.getArmorUpgradeManager().getArmorLevel(teamName) : 0;

        // Row 2: Category buttons (evenly spaced)
        // 陣地強化 (Territory Enhancement) - Beacon icon
        ItemStack territory = new ItemStack(Material.BEACON);
        ItemMeta territoryMeta = territory.getItemMeta();
        if (territoryMeta != null) {
            territoryMeta.setDisplayName("§e§l陣地強化");

            java.util.List<String> lore = new java.util.ArrayList<>();

            // Lv I - Heal
            if (territoryLevel >= 1) {
                lore.add("§9Lv I ヒール §7(ダイヤ x3)");
                lore.add("§9  陣地にいると自動で体力回復");
            } else {
                lore.add("§7Lv I ヒール §7(ダイヤ x3)");
                lore.add("§7  陣地にいると自動で体力回復");
            }

            lore.add("");

            // Lv II - Speed
            if (territoryLevel >= 2) {
                lore.add("§9Lv II 加速 §7(ダイヤ x4)");
                lore.add("§9  ジェネレーターの生成速度が2倍");
            } else {
                lore.add("§7Lv II 加速 §7(ダイヤ x4)");
                lore.add("§7  ジェネレーターの生成速度が2倍");
            }

            lore.add("");

            // Lv III - Evolution
            if (territoryLevel >= 3) {
                lore.add("§9Lv III 進化 §7(ダイヤ x5)");
                lore.add("§9  ジェネレーターが進化");
            } else {
                lore.add("§7Lv III 進化 §7(ダイヤ x5)");
                lore.add("§7  ジェネレーターが進化");
            }

            lore.add("");
            lore.add("§aクリックして購入");

            territoryMeta.setLore(lore);
            territory.setItemMeta(territoryMeta);
        }
        inv.setItem(11, territory);

        // 武器強化 (Weapon Enhancement) - Iron Sword icon
        ItemStack weapon = new ItemStack(Material.IRON_SWORD);
        ItemMeta weaponMeta = weapon.getItemMeta();
        if (weaponMeta != null) {
            weaponMeta.setDisplayName("§c§l武器強化");

            // Hide attribute modifiers (removes "When in Main Hand:" display)
            weaponMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

            java.util.List<String> weaponLore = new java.util.ArrayList<>();

            if (weaponLevel >= 1) {
                weaponLore.add("§9攻撃力上昇 §7(ダイヤ x8)");
            } else {
                weaponLore.add("§7攻撃力上昇 §7(ダイヤ x8)");
            }

            weaponLore.add("");
            weaponLore.add("§aクリックして購入");

            weaponMeta.setLore(weaponLore);
            weapon.setItemMeta(weaponMeta);
        }
        inv.setItem(13, weapon);

        // 装備強化 (Armor Enhancement) - Iron Chestplate icon
        ItemStack armor = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta armorMeta = armor.getItemMeta();
        if (armorMeta != null) {
            armorMeta.setDisplayName("§9§l装備強化");

            // Hide attribute modifiers (removes armor/toughness display)
            armorMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

            java.util.List<String> armorLore = new java.util.ArrayList<>();

            // Lv I - Protection I
            if (armorLevel >= 1) {
                armorLore.add("§9Lv I 防御力アップ §7(ダイヤ x4)");
            } else {
                armorLore.add("§7Lv I 防御力アップ §7(ダイヤ x4)");
            }

            armorLore.add("");

            // Lv II - Protection II
            if (armorLevel >= 2) {
                armorLore.add("§9Lv II 防御力アップ §7(ダイヤ x5)");
            } else {
                armorLore.add("§7Lv II 防御力アップ §7(ダイヤ x5)");
            }

            armorLore.add("");

            // Lv III - Protection III
            if (armorLevel >= 3) {
                armorLore.add("§9Lv III 防御力アップ §7(ダイヤ x6)");
            } else {
                armorLore.add("§7Lv III 防御力アップ §7(ダイヤ x6)");
            }

            armorLore.add("");
            armorLore.add("§aクリックして購入");

            armorMeta.setLore(armorLore);
            armor.setItemMeta(armorMeta);
        }
        inv.setItem(15, armor);

        // トラップ (Traps) - Bell icon
        ItemStack trap = new ItemStack(Material.BELL);
        ItemMeta trapMeta = trap.getItemMeta();
        if (trapMeta != null) {
            trapMeta.setDisplayName("§d§lトラップ");
            trapMeta.setLore(Arrays.asList(
                "§7チームの陣地を守るトラップ",
                "§aクリックして詳細を表示"
            ));
            trap.setItemMeta(trapMeta);
        }
        inv.setItem(29, trap);

        player.openInventory(inv);
    }

    public void openTerritoryShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§e§l陣地強化");

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

        // TODO: Add territory enhancement items here

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

    public void openWeaponShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§c§l武器強化");

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

        // TODO: Add weapon enhancement items here

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

    public void openArmorShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§9§l装備強化");

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

        // TODO: Add armor enhancement items here

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

    public void openTrapShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§d§lトラップ");

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

        // Get player's team to check alarm level
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        int alarmLevel = teamName != null ? plugin.getAlarmTrapManager().getAlarmLevel(teamName) : 0;

        // Alarm Trap - Level 1 (Detection only)
        ItemStack alarm1 = new ItemStack(Material.REDSTONE_LAMP);
        ItemMeta alarm1Meta = alarm1.getItemMeta();
        if (alarm1Meta != null) {
            alarm1Meta.setDisplayName("§e§lアラーム - Lv 1");
            java.util.List<String> alarm1Lore = new java.util.ArrayList<>();
            alarm1Lore.add("§7陣地(ベッド半径20m)に敵が侵入したら");
            alarm1Lore.add("§7チーム全員に通知が届きます");
            alarm1Lore.add("");
            if (alarmLevel >= 1) {
                alarm1Lore.add("§a✓ 購入済み");
            } else {
                alarm1Lore.add("§6価格: §fダイヤ x1");
                alarm1Lore.add("§aクリックして購入");
            }
            alarm1Meta.setLore(alarm1Lore);
            alarm1.setItemMeta(alarm1Meta);
        }
        inv.setItem(20, alarm1);

        // Alarm Trap - Level 2 (Detection + Debuffs)
        ItemStack alarm2 = new ItemStack(Material.REDSTONE_LAMP);
        ItemMeta alarm2Meta = alarm2.getItemMeta();
        if (alarm2Meta != null) {
            alarm2Meta.setDisplayName("§c§lアラーム - Lv 2");
            java.util.List<String> alarm2Lore = new java.util.ArrayList<>();
            alarm2Lore.add("§7陣地に侵入した敵に3秒間:");
            alarm2Lore.add("§7  ・視界が悪くなる(盲目)");
            alarm2Lore.add("§7  ・掘る速度が遅くなる(採掘疲労II)");
            alarm2Lore.add("§7  ・歩く速度が遅くなる(鈍足)");
            alarm2Lore.add("");
            if (alarmLevel >= 2) {
                alarm2Lore.add("§a✓ 購入済み");
            } else if (alarmLevel < 1) {
                alarm2Lore.add("§c✗ Lv 1が必要です");
            } else {
                alarm2Lore.add("§6価格: §fダイヤ x2");
                alarm2Lore.add("§aクリックして購入");
            }
            alarm2Meta.setLore(alarm2Lore);
            alarm2.setItemMeta(alarm2Meta);
        }
        inv.setItem(24, alarm2);

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

    /**
     * Update the GUI for all team members who currently have the upgrade shop open
     * @param teamName The team whose members' GUIs should be updated
     */
    public void updateTeamGUIs(String teamName) {
        if (teamName == null) {
            return;
        }

        myplg.myplg.Team team = plugin.getGameManager().getTeam(teamName);
        if (team == null) {
            return;
        }

        // Update GUI for all online team members
        for (java.util.UUID memberUUID : team.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                // Check if player has the upgrade shop open
                String viewTitle = member.getOpenInventory().getTitle();
                if (viewTitle.equals("§6§lアップグレード - メイン")) {
                    // Reopen the main shop to refresh the GUI
                    openMainShop(member);
                }
            }
        }
    }
}
