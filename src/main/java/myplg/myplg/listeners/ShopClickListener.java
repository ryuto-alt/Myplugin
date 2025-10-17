package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.gui.ShopGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopClickListener implements Listener {
    private final PvPGame plugin;
    private final ShopGUI shopGUI;
    private ShopVillagerListener villagerListener;
    private ShopTwoListener shopTwoListener;
    private final Map<UUID, Long> purchaseCooldown; // Player UUID -> Last purchase time
    private static final long COOLDOWN_MS = 200; // 0.2秒

    public ShopClickListener(PvPGame plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
        this.purchaseCooldown = new HashMap<>();
    }

    public void setVillagerListener(ShopVillagerListener villagerListener) {
        this.villagerListener = villagerListener;
    }

    public void setShopTwoListener(ShopTwoListener shopTwoListener) {
        this.shopTwoListener = shopTwoListener;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        plugin.getLogger().info("GUI Clicked! Title: '" + title + "'");

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Team selection GUI
        if (title.equals("§6§lショップのチームを選択")) {
            event.setCancelled(true);
            handleTeamSelectionClick(player, clickedItem);
            return;
        }

        // Shop config GUI
        if (title.equals("§c§lショップ設定")) {
            event.setCancelled(true);
            handleConfigClick(player, clickedItem);
            return;
        }

        // Main shop navigation (Shop 1 uses "ショップ", Shop 2 uses "アップグレード")
        if (title.equals("§6§lショップ - メイン") || title.equals("§6§lアップグレード - メイン")) {
            event.setCancelled(true);
            handleMainShopClick(player, clickedItem);
            return;
        }

        // Blocks shop
        if (title.equals("§e§lブロック")) {
            event.setCancelled(true);
            handleBlocksShopClick(player, clickedItem);
            return;
        }

        // Equipment shop
        if (title.equals("§c§l装備ショップ")) {
            event.setCancelled(true);
            handleEquipmentShopClick(player, clickedItem);
            return;
        }

        // Enhancement shop
        if (title.equals("§d§l強化")) {
            event.setCancelled(true);
            handleEnhancementShopClick(player, clickedItem);
            return;
        }

        // Tools shop
        if (title.equals("§6§l道具")) {
            event.setCancelled(true);
            handleToolsShopClick(player, clickedItem);
            return;
        }
    }

    private void handleMainShopClick(Player player, ItemStack clickedItem) {
        Material type = clickedItem.getType();
        int amount = clickedItem.getAmount();

        // Check for category buttons vs quick buy items
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            String displayName = clickedItem.getItemMeta().getDisplayName();

            // Category button: Territory Enhancement (陣地強化) - Shop 2
            if (type == Material.BEACON && displayName.contains("陣地強化")) {
                handleTerritoryUpgradeClick(player);
                return;
            }

            // Category button: Weapon Enhancement (武器強化) - Shop 2
            if (type == Material.IRON_SWORD && displayName.contains("武器強化")) {
                handleWeaponUpgradeClick(player);
                return;
            }

            // Category button: Armor Enhancement (装備強化) - Shop 2
            if (type == Material.IRON_CHESTPLATE && displayName.contains("装備強化")) {
                handleArmorUpgradeClick(player);
                return;
            }

            // Category button: Blocks
            if (displayName.contains("ブロック") && amount == 1) {
                shopGUI.openBlocksShop(player);
                return;
            }

            // Category button: Equipment
            if (displayName.contains("装備") && amount == 1 && type != Material.IRON_SWORD) {
                shopGUI.openEquipmentShop(player);
                return;
            }

            // Category button: Enhancement (強化 but not 陣地強化)
            if (displayName.contains("強化") && amount == 1 && !displayName.contains("陣地")) {
                shopGUI.openEnhancementShop(player);
                return;
            }

            // Category button: Tools
            if (displayName.contains("道具") && amount == 1) {
                shopGUI.openToolsShop(player);
                return;
            }
        }

        // Quick buy items (check by material and amount)
        if (type.toString().endsWith("_WOOL") && amount == 16) {
            // Quick buy: Wool x16 for iron 4
            processPurchase(player, Material.IRON_INGOT, 4, type, 16);
        } else if (type == Material.OAK_PLANKS && amount == 10) {
            // Quick buy: Oak Planks x10 for gold 6
            processPurchase(player, Material.GOLD_INGOT, 6, type, 10);
        } else if (type == Material.STONE_SWORD && amount == 1) {
            // Quick buy: Stone Sword for iron 10
            processPurchase(player, Material.IRON_INGOT, 10, type, 1);
        } else if (type == Material.IRON_SWORD && amount == 1) {
            // Quick buy: Iron Sword for gold 7
            processPurchase(player, Material.GOLD_INGOT, 7, type, 1);
        } else if (type == Material.POTION && amount == 1) {
            // Check potion type by display name
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String potionName = clickedItem.getItemMeta().getDisplayName();
                if (potionName.contains("透明化")) {
                    // Invisibility potion
                    processPotionPurchase(player, Material.EMERALD, 2, org.bukkit.potion.PotionEffectType.INVISIBILITY, 30 * 20, 0);
                } else if (potionName.contains("跳躍力")) {
                    // Jump boost potion
                    processPotionPurchase(player, Material.EMERALD, 1, org.bukkit.potion.PotionEffectType.JUMP_BOOST, 60 * 20, 0);
                }
            }
        } else if (type == Material.ENCHANTED_GOLDEN_APPLE && amount == 1) {
            // Quick buy: Enchanted Golden Apple for gold 3
            processPurchase(player, Material.GOLD_INGOT, 3, type, 1);
        } else if (type == Material.TNT && amount == 1) {
            // Quick buy: TNT for gold 8
            processPurchase(player, Material.GOLD_INGOT, 8, type, 1);
        }
    }

    private void handleBlocksShopClick(Player player, ItemStack clickedItem) {
        Material type = clickedItem.getType();
        int amount = clickedItem.getAmount();

        // Back button
        if (type == Material.ARROW && clickedItem.hasItemMeta() &&
            clickedItem.getItemMeta().hasDisplayName() &&
            clickedItem.getItemMeta().getDisplayName().contains("戻る")) {
            shopGUI.openMainShop(player);
            return;
        }

        // Ignore glass panes (decoration)
        if (type.toString().contains("GLASS_PANE")) {
            return;
        }

        // Wool x16 for iron 4
        if (type.toString().endsWith("_WOOL") && amount == 16) {
            processPurchase(player, Material.IRON_INGOT, 4, type, 16);
        }
        // Oak Planks x10 for gold 6
        else if (type == Material.OAK_PLANKS && amount == 10) {
            processPurchase(player, Material.GOLD_INGOT, 6, type, 10);
        }
        // End Stone x12 for iron 24
        else if (type == Material.END_STONE && amount == 12) {
            processPurchase(player, Material.IRON_INGOT, 24, type, 12);
        }
        // Team colored Glass x4 for gold 10
        else if (type.toString().contains("STAINED_GLASS") && !type.toString().contains("PANE") && amount == 4) {
            processPurchase(player, Material.GOLD_INGOT, 10, type, 4);
        }
        // Obsidian x4 for emerald 6
        else if (type == Material.OBSIDIAN && amount == 4) {
            processPurchase(player, Material.EMERALD, 6, type, 4);
        }
    }

    private void handleEquipmentShopClick(Player player, ItemStack clickedItem) {
        Material type = clickedItem.getType();
        int amount = clickedItem.getAmount();

        // Back button
        if (type == Material.ARROW && clickedItem.hasItemMeta() &&
            clickedItem.getItemMeta().hasDisplayName() &&
            clickedItem.getItemMeta().getDisplayName().contains("戻る")) {
            shopGUI.openMainShop(player);
            return;
        }

        // Ignore glass panes (decoration)
        if (type.toString().contains("GLASS_PANE")) {
            return;
        }

        // Check if item has enchantments to distinguish between different items
        boolean hasEnchant = clickedItem.hasItemMeta() && !clickedItem.getItemMeta().getEnchants().isEmpty();

        // Swords
        if (type == Material.STONE_SWORD) {
            processPurchase(player, Material.IRON_INGOT, 10, type, 1);
        } else if (type == Material.IRON_SWORD) {
            processPurchase(player, Material.GOLD_INGOT, 7, type, 1);
        } else if (type == Material.DIAMOND_SWORD) {
            processPurchase(player, Material.EMERALD, 3, type, 1);
        } else if (type == Material.NETHERITE_SWORD) {
            processPurchase(player, Material.EMERALD, 7, type, 1);
        }
        // Armor (auto-equip leggings and boots)
        else if (type == Material.IRON_BOOTS) {
            processArmorPurchase(player, Material.GOLD_INGOT, 12, Material.IRON_LEGGINGS, Material.IRON_BOOTS);
        } else if (type == Material.DIAMOND_BOOTS) {
            processArmorPurchase(player, Material.EMERALD, 6, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS);
        } else if (type == Material.NETHERITE_BOOTS) {
            processArmorPurchase(player, Material.EMERALD, 24, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);
        }
        // Bows and Arrows
        else if (type == Material.BOW && !hasEnchant) {
            processPurchase(player, Material.GOLD_INGOT, 10, type, 1);
        } else if (type == Material.BOW && hasEnchant) {
            // Check enchantment type
            if (clickedItem.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.PUNCH)) {
                processEnchantedPurchase(player, Material.GOLD_INGOT, 24, Material.BOW,
                    org.bukkit.enchantments.Enchantment.PUNCH, 1);
            } else if (clickedItem.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.INFINITY)) {
                processEnchantedPurchase(player, Material.EMERALD, 1, Material.BOW,
                    org.bukkit.enchantments.Enchantment.INFINITY, 1);
            }
        } else if (type == Material.ARROW && amount == 8) {
            processPurchase(player, Material.GOLD_INGOT, 2, type, 8);
        }
        // Knockback Stick
        else if (type == Material.STICK && hasEnchant) {
            processEnchantedPurchase(player, Material.GOLD_INGOT, 8, Material.STICK,
                org.bukkit.enchantments.Enchantment.KNOCKBACK, 3);
        }
    }

    private void handleEnhancementShopClick(Player player, ItemStack clickedItem) {
        Material type = clickedItem.getType();

        // Back button
        if (type == Material.ARROW && clickedItem.hasItemMeta() &&
            clickedItem.getItemMeta().hasDisplayName() &&
            clickedItem.getItemMeta().getDisplayName().contains("戻る")) {
            shopGUI.openMainShop(player);
            return;
        }

        // Ignore glass panes (decoration)
        if (type.toString().contains("GLASS_PANE")) {
            return;
        }

        // Check potion type by display name
        if (type == Material.POTION && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            String potionName = clickedItem.getItemMeta().getDisplayName();

            if (potionName.contains("透明化")) {
                // Invisibility potion (30 seconds, emerald 2)
                processPotionPurchase(player, Material.EMERALD, 2, org.bukkit.potion.PotionEffectType.INVISIBILITY, 30 * 20, 0);
            } else if (potionName.contains("跳躍力")) {
                // Jump boost potion (1 minute, emerald 1)
                processPotionPurchase(player, Material.EMERALD, 1, org.bukkit.potion.PotionEffectType.JUMP_BOOST, 60 * 20, 0);
            } else if (potionName.contains("移動速度")) {
                // Speed potion (1 minute, emerald 1)
                processPotionPurchase(player, Material.EMERALD, 1, org.bukkit.potion.PotionEffectType.SPEED, 60 * 20, 0);
            }
        }
    }

    private void handleToolsShopClick(Player player, ItemStack clickedItem) {
        Material type = clickedItem.getType();

        // Back button
        if (type == Material.ARROW && clickedItem.hasItemMeta() &&
            clickedItem.getItemMeta().hasDisplayName() &&
            clickedItem.getItemMeta().getDisplayName().contains("戻る")) {
            shopGUI.openMainShop(player);
            return;
        }

        // Ignore glass panes (decoration)
        if (type.toString().contains("GLASS_PANE")) {
            return;
        }

        // Simple items (no upgrades)
        if (type == Material.ENCHANTED_GOLDEN_APPLE) {
            processPurchase(player, Material.GOLD_INGOT, 3, type, 1);
        } else if (type == Material.TNT) {
            processPurchase(player, Material.GOLD_INGOT, 8, type, 1);
        } else if (type == Material.ENDER_PEARL) {
            processPurchase(player, Material.EMERALD, 4, type, 1);
        } else if (type == Material.FIRE_CHARGE && clickedItem.hasItemMeta() &&
                   clickedItem.getItemMeta().hasDisplayName() &&
                   clickedItem.getItemMeta().getDisplayName().contains("火玉")) {
            processFireballPurchase(player);
        }
        // Tool upgrades - Axes
        else if (type == Material.WOODEN_AXE) {
            processToolUpgradePurchase(player, Material.IRON_INGOT, 12, type, 1, true);
        } else if (type == Material.STONE_AXE) {
            processToolUpgradePurchase(player, Material.IRON_INGOT, 24, type, 2, true);
        } else if (type == Material.IRON_AXE) {
            processToolUpgradePurchase(player, Material.GOLD_INGOT, 8, type, 3, true);
        } else if (type == Material.DIAMOND_AXE) {
            processToolUpgradePurchase(player, Material.GOLD_INGOT, 16, type, 4, true);
        }
        // Tool upgrades - Pickaxes
        else if (type == Material.WOODEN_PICKAXE) {
            processToolUpgradePurchase(player, Material.IRON_INGOT, 12, type, 1, false);
        } else if (type == Material.STONE_PICKAXE) {
            processToolUpgradePurchase(player, Material.IRON_INGOT, 24, type, 2, false);
        } else if (type == Material.IRON_PICKAXE) {
            processToolUpgradePurchase(player, Material.GOLD_INGOT, 8, type, 3, false);
        } else if (type == Material.DIAMOND_PICKAXE) {
            processToolUpgradePurchase(player, Material.GOLD_INGOT, 16, type, 4, false);
        }
    }

    private void processFireballPurchase(Player player) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = purchaseCooldown.get(player.getUniqueId());

        if (lastPurchaseTime != null && (currentTime - lastPurchaseTime) < COOLDOWN_MS) {
            return;
        }

        // Check if player has enough currency (iron 40)
        if (!hasEnoughItems(player, Material.IRON_INGOT, 40)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: 鉄 x40");
            return;
        }

        // Remove currency
        removeItems(player, Material.IRON_INGOT, 40);

        // Create fireball item with custom name
        ItemStack fireball = new ItemStack(Material.FIRE_CHARGE, 1);
        org.bukkit.inventory.meta.ItemMeta meta = fireball.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c火玉");
            meta.setLore(java.util.Arrays.asList(
                "§7右クリックで火の玉を発射"
            ));
            fireball.setItemMeta(meta);
        }

        // Give item to player
        java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(fireball);

        if (!leftover.isEmpty()) {
            // Return currency if inventory is full
            player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 40));
            player.sendMessage("§cインベントリに空きがありません！");
            return;
        }

        // Update cooldown
        purchaseCooldown.put(player.getUniqueId(), currentTime);

        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void processToolUpgradePurchase(Player player, Material currency, int cost, Material tool, int level, boolean isAxe) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = purchaseCooldown.get(player.getUniqueId());

        if (lastPurchaseTime != null && (currentTime - lastPurchaseTime) < COOLDOWN_MS) {
            return;
        }

        // Check current level
        int currentLevel = isAxe ?
            plugin.getToolUpgradeManager().getAxeLevel(player.getUniqueId()) :
            plugin.getToolUpgradeManager().getPickaxeLevel(player.getUniqueId());

        if (currentLevel >= level) {
            player.sendMessage("§c既にこのレベル以上のツールを持っています！");
            return;
        }

        // Check if player has enough currency
        if (!hasEnoughItems(player, currency, cost)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: " + getItemDisplayName(currency) + " x" + cost);
            return;
        }

        // Remove currency
        removeItems(player, currency, cost);

        // Upgrade tool level
        boolean upgraded = isAxe ?
            plugin.getToolUpgradeManager().upgradeAxe(player.getUniqueId(), level) :
            plugin.getToolUpgradeManager().upgradePickaxe(player.getUniqueId(), level);

        if (!upgraded) {
            // Refund if upgrade failed
            player.getInventory().addItem(new ItemStack(currency, cost));
            player.sendMessage("§cアップグレードに失敗しました。");
            return;
        }

        // Remove all old tools from inventory
        Inventory inv = player.getInventory();
        if (isAxe) {
            // Remove all axes from inventory
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && (item.getType() == Material.WOODEN_AXE ||
                    item.getType() == Material.STONE_AXE ||
                    item.getType() == Material.IRON_AXE ||
                    item.getType() == Material.DIAMOND_AXE)) {
                    inv.setItem(i, null);
                }
            }
        } else {
            // Remove all pickaxes from inventory
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && (item.getType() == Material.WOODEN_PICKAXE ||
                    item.getType() == Material.STONE_PICKAXE ||
                    item.getType() == Material.IRON_PICKAXE ||
                    item.getType() == Material.DIAMOND_PICKAXE)) {
                    inv.setItem(i, null);
                }
            }
        }

        // Give new tool to player
        ItemStack toolItem = new ItemStack(tool, 1);
        java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toolItem);

        if (!leftover.isEmpty()) {
            // This shouldn't happen since we just removed old tools, but handle it anyway
            player.sendMessage("§cインベントリに空きがありません！");
            return;
        }

        // Update cooldown
        purchaseCooldown.put(player.getUniqueId(), currentTime);

        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        // Success message
        String toolName = isAxe ? "斧" : "ツルハシ";
        player.sendMessage("§a" + toolName + "をレベル " + level + " にアップグレードしました！");

        // Reopen the tools shop to show updated items in real-time
        shopGUI.openToolsShop(player);
    }

    private void processPotionPurchase(Player player, Material currency, int cost,
                                       org.bukkit.potion.PotionEffectType effectType, int duration, int amplifier) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = purchaseCooldown.get(player.getUniqueId());

        if (lastPurchaseTime != null && (currentTime - lastPurchaseTime) < COOLDOWN_MS) {
            return;
        }

        // Check if player has enough currency
        if (!hasEnoughItems(player, currency, cost)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: " + getItemDisplayName(currency) + " x" + cost);
            return;
        }

        // Remove currency
        removeItems(player, currency, cost);

        // Apply potion effect directly to player
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(effectType, duration, amplifier));

        // Update cooldown
        purchaseCooldown.put(player.getUniqueId(), currentTime);

        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void processArmorPurchase(Player player, Material currency, int cost, Material leggings, Material boots) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = purchaseCooldown.get(player.getUniqueId());

        if (lastPurchaseTime != null && (currentTime - lastPurchaseTime) < COOLDOWN_MS) {
            return;
        }

        // Check if player has enough currency
        if (!hasEnoughItems(player, currency, cost)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: " + getItemDisplayName(currency) + " x" + cost);
            return;
        }

        // Remove currency
        removeItems(player, currency, cost);

        // Auto-equip armor
        player.getInventory().setLeggings(new ItemStack(leggings));
        player.getInventory().setBoots(new ItemStack(boots));

        // Update cooldown
        purchaseCooldown.put(player.getUniqueId(), currentTime);

        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void processEnchantedPurchase(Player player, Material currency, int cost, Material item,
                                          org.bukkit.enchantments.Enchantment enchant, int level) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = purchaseCooldown.get(player.getUniqueId());

        if (lastPurchaseTime != null && (currentTime - lastPurchaseTime) < COOLDOWN_MS) {
            return;
        }

        // Check if player has enough currency
        if (!hasEnoughItems(player, currency, cost)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: " + getItemDisplayName(currency) + " x" + cost);
            return;
        }

        // Remove currency
        removeItems(player, currency, cost);

        // Create enchanted item
        ItemStack enchantedItem = new ItemStack(item, 1);
        enchantedItem.addUnsafeEnchantment(enchant, level);

        // Give item to player
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(enchantedItem);

        if (!leftover.isEmpty()) {
            // Return currency if inventory is full
            player.getInventory().addItem(new ItemStack(currency, cost));
            player.sendMessage("§cインベントリに空きがありません！");
            return;
        }

        // Update cooldown
        purchaseCooldown.put(player.getUniqueId(), currentTime);

        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void processPurchase(Player player, Material currency, int cost, Material item, int amount) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastPurchaseTime = purchaseCooldown.get(player.getUniqueId());

        if (lastPurchaseTime != null && (currentTime - lastPurchaseTime) < COOLDOWN_MS) {
            // Still in cooldown, ignore silently
            return;
        }

        plugin.getLogger().info("processPurchase called: currency=" + currency + ", cost=" + cost + ", item=" + item + ", amount=" + amount);

        // Check if player has enough currency
        if (!hasEnoughItems(player, currency, cost)) {
            plugin.getLogger().info("Not enough items! Player has: " + countItems(player, currency) + ", needs: " + cost);
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: " + getItemDisplayName(currency) + " x" + cost);
            return;
        }

        plugin.getLogger().info("Player has enough currency, removing items...");

        // Remove currency from player's inventory
        removeItems(player, currency, cost);

        // Give item to player
        ItemStack purchasedItem = new ItemStack(item, amount);

        plugin.getLogger().info("Adding purchased items to inventory...");

        // Check if player has space
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(purchasedItem);

        if (!leftover.isEmpty()) {
            plugin.getLogger().info("Inventory full! Returning currency...");
            // Return currency if inventory is full
            player.getInventory().addItem(new ItemStack(currency, cost));
            player.sendMessage("§cインベントリに空きがありません！");
            return;
        }

        // Update cooldown
        purchaseCooldown.put(player.getUniqueId(), currentTime);

        plugin.getLogger().info("Purchase successful!");

        // Play sound (no message to avoid spam)
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean hasEnoughItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    inv.setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }

                if (remaining == 0) {
                    break;
                }
            }
        }
    }

    private void handleTeamSelectionClick(Player player, ItemStack clickedItem) {
        if (villagerListener == null && shopTwoListener == null) {
            player.sendMessage("§cエラー: システムエラーが発生しました。");
            player.closeInventory();
            return;
        }

        // Check if clicked item is wool (team selection)
        if (!clickedItem.getType().toString().endsWith("_WOOL")) {
            return;
        }

        // Get team name from item display name
        String displayName = clickedItem.getItemMeta().getDisplayName();
        String teamName = displayName.replace("§e§l", "");

        // Get pending entity UUID - try both listeners
        UUID entityUUID = null;
        if (villagerListener != null) {
            entityUUID = villagerListener.getTeamSelectGUI().getPendingVillager(player.getUniqueId());
        }
        if (entityUUID == null && shopTwoListener != null) {
            entityUUID = shopTwoListener.getTeamSelectGUI().getPendingVillager(player.getUniqueId());
        }

        if (entityUUID == null) {
            player.sendMessage("§cエラー: ショップが見つかりません。");
            player.closeInventory();
            return;
        }

        // Find entity (Villager or Skeleton)
        Entity entity = Bukkit.getEntity(entityUUID);
        if (entity == null) {
            player.sendMessage("§cエラー: ショップが見つかりません。");
            player.closeInventory();
            if (villagerListener != null) {
                villagerListener.getTeamSelectGUI().removePendingVillager(player.getUniqueId());
            }
            if (shopTwoListener != null) {
                shopTwoListener.getTeamSelectGUI().removePendingVillager(player.getUniqueId());
            }
            return;
        }

        // Set team based on entity type
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            villagerListener.setVillagerTeam(villager, teamName);
            villagerListener.getTeamSelectGUI().removePendingVillager(player.getUniqueId());
        } else if (entity instanceof Skeleton) {
            Skeleton skeleton = (Skeleton) entity;
            shopTwoListener.setSkeletonTeam(skeleton, teamName);
            shopTwoListener.getTeamSelectGUI().removePendingVillager(player.getUniqueId());
        } else {
            player.sendMessage("§cエラー: 不明なショップタイプです。");
            player.closeInventory();
            return;
        }

        // Success message
        player.sendMessage("§a§lショップを " + teamName + " チームに設定しました！");
        player.closeInventory();
    }

    private void handleConfigClick(Player player, ItemStack clickedItem) {
        if (villagerListener == null && shopTwoListener == null) {
            player.sendMessage("§cエラー: システムエラーが発生しました。");
            player.closeInventory();
            return;
        }

        Material type = clickedItem.getType();

        if (type == Material.BARRIER) {
            // Delete shop - try both listeners
            UUID entityUUID = null;
            if (villagerListener != null) {
                entityUUID = villagerListener.getConfigGUI().getConfiguredVillager(player.getUniqueId());
            }
            if (entityUUID == null && shopTwoListener != null) {
                entityUUID = shopTwoListener.getConfigGUI().getConfiguredVillager(player.getUniqueId());
            }

            if (entityUUID == null) {
                player.sendMessage("§cエラー: ショップが見つかりません。");
                player.closeInventory();
                return;
            }

            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity == null) {
                player.sendMessage("§cエラー: ショップが見つかりません。");
                player.closeInventory();
                if (villagerListener != null) {
                    villagerListener.getConfigGUI().removeConfiguredVillager(player.getUniqueId());
                }
                if (shopTwoListener != null) {
                    shopTwoListener.getConfigGUI().removeConfiguredVillager(player.getUniqueId());
                }
                return;
            }

            // Remove from config
            plugin.getShopDataManager().removeShopVillager(entityUUID);

            // Remove entity (works for both Villager and Skeleton)
            entity.remove();

            // Remove from tracking
            if (villagerListener != null) {
                villagerListener.getConfigGUI().removeConfiguredVillager(player.getUniqueId());
            }
            if (shopTwoListener != null) {
                shopTwoListener.getConfigGUI().removeConfiguredVillager(player.getUniqueId());
            }

            player.sendMessage("§a§lショップを削除しました。");
            player.closeInventory();
        } else if (type == Material.ARROW) {
            // Close button
            player.closeInventory();
        }
    }

    private void handleTerritoryUpgradeClick(Player player) {
        // Get player's team
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage("§cエラー: チームに所属していません。");
            player.closeInventory();
            return;
        }

        // Get current upgrade level
        int currentLevel = plugin.getTerritoryUpgradeManager().getUpgradeLevel(teamName);
        int nextLevel = currentLevel + 1;

        // Check if max level reached
        if (nextLevel > 3) {
            player.sendMessage("§c陣地強化は既に最大レベルです！");
            return;
        }

        // Determine cost based on level
        int cost;
        String upgradeName;
        switch (nextLevel) {
            case 1:
                cost = 3;
                upgradeName = "Lv I ヒール";
                break;
            case 2:
                cost = 4;
                upgradeName = "Lv II 加速";
                break;
            case 3:
                cost = 5;
                upgradeName = "Lv III 進化";
                break;
            default:
                return;
        }

        // Check if player has enough diamonds
        if (!hasEnoughItems(player, Material.DIAMOND, cost)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: ダイヤ x" + cost);
            return;
        }

        // Remove diamonds
        removeItems(player, Material.DIAMOND, cost);

        // Upgrade territory
        boolean success = plugin.getTerritoryUpgradeManager().upgradeTerritory(teamName, nextLevel);

        if (success) {
            // Broadcast to team
            myplg.myplg.Team team = plugin.getGameManager().getTeam(teamName);
            if (team != null) {
                for (java.util.UUID memberUUID : team.getMembers()) {
                    org.bukkit.entity.Player member = plugin.getServer().getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.sendMessage("§a§l陣地強化: " + upgradeName + " §aが購入されました！");
                    }
                }
            }

            // Play sound
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

            // Reopen shop to show updated status
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                new myplg.myplg.gui.ShopTwoGUI(plugin).openMainShop(player);
            }, 1L);
        } else {
            // Refund if upgrade failed
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, cost));
            player.sendMessage("§cアップグレードに失敗しました。");
        }
    }

    private void handleWeaponUpgradeClick(Player player) {
        // Get player's team
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage("§cエラー: チームに所属していません。");
            player.closeInventory();
            return;
        }

        // Check if already upgraded
        if (plugin.getWeaponUpgradeManager().hasWeaponUpgrade(teamName)) {
            player.sendMessage("§c武器強化は既に購入済みです！");
            return;
        }

        // Check if player has enough diamonds
        int cost = 8;
        if (!hasEnoughItems(player, Material.DIAMOND, cost)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: ダイヤ x" + cost);
            return;
        }

        // Remove diamonds
        removeItems(player, Material.DIAMOND, cost);

        // Upgrade weapon
        boolean success = plugin.getWeaponUpgradeManager().upgradeWeapon(teamName);

        if (success) {
            // Broadcast to team and apply enchantment to all team members' swords
            myplg.myplg.Team team = plugin.getGameManager().getTeam(teamName);
            if (team != null) {
                for (java.util.UUID memberUUID : team.getMembers()) {
                    org.bukkit.entity.Player member = plugin.getServer().getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.sendMessage("§a§l武器強化: 攻撃力上昇 §aが購入されました！");

                        // Apply Sharpness I to all swords in inventory
                        for (ItemStack item : member.getInventory().getContents()) {
                            if (item != null && item.getType().toString().contains("SWORD")) {
                                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 1);
                            }
                        }
                    }
                }
            }

            // Play sound
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

            // Reopen shop to show updated status
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                new myplg.myplg.gui.ShopTwoGUI(plugin).openMainShop(player);
            }, 1L);
        } else {
            // Refund if upgrade failed
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, cost));
            player.sendMessage("§cアップグレードに失敗しました。");
        }
    }

    private void handleArmorUpgradeClick(Player player) {
        // Get player's team
        String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage("§cエラー: チームに所属していません。");
            player.closeInventory();
            return;
        }

        // Get current upgrade level
        int currentLevel = plugin.getArmorUpgradeManager().getArmorLevel(teamName);
        int nextLevel = currentLevel + 1;

        // Check if max level reached
        if (nextLevel > 3) {
            player.sendMessage("§c装備強化は既に最大レベルです！");
            return;
        }

        // Determine cost based on level
        int cost;
        String upgradeName;
        switch (nextLevel) {
            case 1:
                cost = 4;
                upgradeName = "Lv I 防御力アップ";
                break;
            case 2:
                cost = 5;
                upgradeName = "Lv II 防御力アップ";
                break;
            case 3:
                cost = 6;
                upgradeName = "Lv III 防御力アップ";
                break;
            default:
                return;
        }

        // Check if player has enough diamonds
        if (!hasEnoughItems(player, Material.DIAMOND, cost)) {
            player.sendMessage("§c購入に必要な通貨が不足しています！ 必要: ダイヤ x" + cost);
            return;
        }

        // Remove diamonds
        removeItems(player, Material.DIAMOND, cost);

        // Upgrade armor
        boolean success = plugin.getArmorUpgradeManager().upgradeArmor(teamName, nextLevel);

        if (success) {
            // Broadcast to team and apply enchantment to all team members' armor
            myplg.myplg.Team team = plugin.getGameManager().getTeam(teamName);
            if (team != null) {
                for (java.util.UUID memberUUID : team.getMembers()) {
                    org.bukkit.entity.Player member = plugin.getServer().getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.sendMessage("§a§l装備強化: " + upgradeName + " §aが購入されました！");

                        // Apply Protection enchantment to all armor pieces in inventory and equipped
                        for (ItemStack item : member.getInventory().getContents()) {
                            if (item != null && (item.getType().toString().contains("HELMET") ||
                                item.getType().toString().contains("CHESTPLATE") ||
                                item.getType().toString().contains("LEGGINGS") ||
                                item.getType().toString().contains("BOOTS"))) {
                                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, nextLevel);
                            }
                        }

                        // Also apply to equipped armor
                        ItemStack[] armorContents = member.getInventory().getArmorContents();
                        for (ItemStack item : armorContents) {
                            if (item != null) {
                                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, nextLevel);
                            }
                        }
                    }
                }
            }

            // Play sound
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

            // Reopen shop to show updated status
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                new myplg.myplg.gui.ShopTwoGUI(plugin).openMainShop(player);
            }, 1L);
        } else {
            // Refund if upgrade failed
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, cost));
            player.sendMessage("§cアップグレードに失敗しました。");
        }
    }

    private String getItemDisplayName(Material material) {
        String name = material.toString().toLowerCase().replace("_", " ");

        // Japanese translations for common items
        Map<String, String> translations = new HashMap<>();
        translations.put("iron ingot", "鉄");
        translations.put("gold ingot", "ゴールド");
        translations.put("emerald", "エメラルド");
        translations.put("diamond", "ダイヤモンド");
        translations.put("oak planks", "オークの木材");
        translations.put("end stone", "エンドストーン");
        translations.put("obsidian", "黒曜石");

        // Wool colors
        translations.put("cyan wool", "水色の羊毛");
        translations.put("yellow wool", "黄色の羊毛");
        translations.put("blue wool", "青の羊毛");
        translations.put("white wool", "白の羊毛");
        translations.put("gray wool", "灰色の羊毛");
        translations.put("pink wool", "ピンクの羊毛");
        translations.put("green wool", "緑の羊毛");
        translations.put("red wool", "赤の羊毛");

        // Glass colors
        translations.put("cyan stained glass", "水色のガラス");
        translations.put("yellow stained glass", "黄色のガラス");
        translations.put("blue stained glass", "青のガラス");
        translations.put("white stained glass", "白のガラス");
        translations.put("gray stained glass", "灰色のガラス");
        translations.put("pink stained glass", "ピンクのガラス");
        translations.put("green stained glass", "緑のガラス");
        translations.put("red stained glass", "赤のガラス");

        return translations.getOrDefault(name, name);
    }
}
