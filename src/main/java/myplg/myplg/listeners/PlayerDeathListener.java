package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerDeathListener implements Listener {
    private final PvPGame plugin;
    private final Set<UUID> eliminatedPlayers; // Players who can't respawn
    private final Map<UUID, ItemStack[]> savedArmor; // Saved armor for respawn
    private final Map<UUID, ItemStack> savedAxe; // Saved axe for respawn
    private final Map<UUID, ItemStack> savedPickaxe; // Saved pickaxe for respawn
    private final Set<UUID> processingDeath; // Prevent duplicate death processing

    public PlayerDeathListener(PvPGame plugin) {
        this.plugin = plugin;
        this.eliminatedPlayers = new HashSet<>();
        this.savedArmor = new HashMap<>();
        this.savedAxe = new HashMap<>();
        this.savedPickaxe = new HashMap<>();
        this.processingDeath = new HashSet<>();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerUUID = player.getUniqueId();

        // Check if player will die from this damage (health <= 0.2 after damage)
        double healthAfterDamage = player.getHealth() - event.getFinalDamage();

        if (healthAfterDamage <= 0.2 && !processingDeath.contains(playerUUID)) {
            // Cancel the damage event
            event.setCancelled(true);

            // Mark as processing
            processingDeath.add(playerUUID);

            // Trigger instant death
            handleInstantDeath(player);
        }
    }

    private void handleInstantDeath(Player player) {
        UUID playerUUID = player.getUniqueId();
        String teamName = plugin.getGameManager().getPlayerTeam(playerUUID);

        // Clear inventory drops
        player.getInventory().clear();

        // Check if bed is alive
        boolean bedAlive = plugin.getScoreboardManager().isBedAlive(teamName);

        if (!bedAlive) {
            // Player is eliminated
            eliminatedPlayers.add(playerUUID);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendTitle("§c§l死んでしまった！", "§cあなたは脱落しました", 10, 70, 20);
            processingDeath.remove(playerUUID);
            return;
        }

        // Save armor (iron, diamond, netherite)
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] savedArmorArray = new ItemStack[4];
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && isUpgradedArmor(armor[i].getType())) {
                savedArmorArray[i] = armor[i].clone();
            }
        }
        savedArmor.put(playerUUID, savedArmorArray);

        // Find and save axe/pickaxe (downgrade level)
        ItemStack currentAxe = null;
        ItemStack currentPickaxe = null;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            Material type = item.getType();
            if (isAxe(type)) {
                currentAxe = item;
            } else if (isPickaxe(type)) {
                currentPickaxe = item;
            }
        }

        // Downgrade tools
        if (currentAxe != null) {
            int currentLevel = getToolLevel(currentAxe.getType(), true);
            if (currentLevel > 0) {
                int newLevel = currentLevel - 1;
                plugin.getToolUpgradeManager().upgradeAxe(playerUUID, newLevel);
                if (newLevel > 0) {
                    Material downgradedAxe = plugin.getToolUpgradeManager().getAxeMaterial(newLevel);
                    savedAxe.put(playerUUID, new ItemStack(downgradedAxe));
                } else {
                    savedAxe.remove(playerUUID);
                }
            }
        }

        if (currentPickaxe != null) {
            int currentLevel = getToolLevel(currentPickaxe.getType(), false);
            if (currentLevel > 0) {
                int newLevel = currentLevel - 1;
                plugin.getToolUpgradeManager().upgradePickaxe(playerUUID, newLevel);
                if (newLevel > 0) {
                    Material downgradedPickaxe = plugin.getToolUpgradeManager().getPickaxeMaterial(newLevel);
                    savedPickaxe.put(playerUUID, new ItemStack(downgradedPickaxe));
                } else {
                    savedPickaxe.remove(playerUUID);
                }
            }
        }

        // Show "死んでしまった！" title to the player
        player.sendTitle("§c§l死んでしまった！", "§e5秒後にリスポーンします...", 10, 70, 20);

        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(20.0); // Reset health

        // Teleport to (0, 125, 0) in the same world
        Location spectatorLocation = new Location(player.getWorld(), 0.5, 125.0, 0.5);
        player.teleport(spectatorLocation);

        // Get team spawn location
        Location spawnLocation = plugin.getGameManager().getTeam(teamName).getSpawnLocation();

        // Teleport to spawn after 5 seconds and restore equipment
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(spawnLocation);
                    restorePlayerEquipment(player, playerUUID, teamName);
                    processingDeath.remove(playerUUID);
                }
            }
        }.runTaskLater(plugin, 100L); // 5 seconds
    }

    private void restorePlayerEquipment(Player player, UUID playerUUID, String teamName) {
        // Clear inventory
        player.getInventory().clear();

        // Give wooden sword
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));

        // Restore saved armor or give leather armor
        ItemStack[] savedArmorArray = savedArmor.get(playerUUID);
        if (savedArmorArray != null) {
            for (int i = 0; i < savedArmorArray.length; i++) {
                if (savedArmorArray[i] != null) {
                    switch (i) {
                        case 0: player.getInventory().setBoots(savedArmorArray[i]); break;
                        case 1: player.getInventory().setLeggings(savedArmorArray[i]); break;
                        case 2: player.getInventory().setChestplate(savedArmorArray[i]); break;
                        case 3: player.getInventory().setHelmet(savedArmorArray[i]); break;
                    }
                }
            }
            savedArmor.remove(playerUUID);
        }

        // Give team-colored leather armor for empty slots
        giveLeatherArmorForEmptySlots(player, teamName);

        // Restore tools
        if (savedAxe.containsKey(playerUUID)) {
            player.getInventory().addItem(savedAxe.get(playerUUID));
            savedAxe.remove(playerUUID);
        }

        if (savedPickaxe.containsKey(playerUUID)) {
            player.getInventory().addItem(savedPickaxe.get(playerUUID));
            savedPickaxe.remove(playerUUID);
        }
    }

    private void giveLeatherArmorForEmptySlots(Player player, String teamName) {
        org.bukkit.Color armorColor = getTeamColor(teamName);

        if (player.getInventory().getHelmet() == null) {
            player.getInventory().setHelmet(createColoredArmor(Material.LEATHER_HELMET, armorColor));
        }
        if (player.getInventory().getChestplate() == null) {
            player.getInventory().setChestplate(createColoredArmor(Material.LEATHER_CHESTPLATE, armorColor));
        }
        if (player.getInventory().getLeggings() == null) {
            player.getInventory().setLeggings(createColoredArmor(Material.LEATHER_LEGGINGS, armorColor));
        }
        if (player.getInventory().getBoots() == null) {
            player.getInventory().setBoots(createColoredArmor(Material.LEATHER_BOOTS, armorColor));
        }
    }

    private ItemStack createColoredArmor(Material material, org.bukkit.Color color) {
        ItemStack armor = new ItemStack(material);
        org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) armor.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            meta.setUnbreakable(true);
            armor.setItemMeta(meta);
        }
        return armor;
    }

    private org.bukkit.Color getTeamColor(String teamName) {
        switch (teamName) {
            case "レッド": return org.bukkit.Color.RED;
            case "ブルー": return org.bukkit.Color.BLUE;
            case "グリーン": return org.bukkit.Color.GREEN;
            case "イエロー": return org.bukkit.Color.YELLOW;
            case "アクア": return org.bukkit.Color.AQUA;
            case "ホワイト": return org.bukkit.Color.WHITE;
            case "ピンク": return org.bukkit.Color.FUCHSIA;
            case "グレー": return org.bukkit.Color.GRAY;
            default: return org.bukkit.Color.WHITE;
        }
    }

    private boolean isUpgradedArmor(Material material) {
        String name = material.toString();
        return (name.startsWith("IRON_") || name.startsWith("DIAMOND_") || name.startsWith("NETHERITE_")) &&
               (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS"));
    }

    private boolean isAxe(Material material) {
        String name = material.toString();
        return name.endsWith("_AXE");
    }

    private boolean isPickaxe(Material material) {
        String name = material.toString();
        return name.endsWith("_PICKAXE");
    }

    private int getToolLevel(Material material, boolean isAxe) {
        String prefix = material.toString().replace("_AXE", "").replace("_PICKAXE", "");
        switch (prefix) {
            case "WOODEN": return 1;
            case "STONE": return 2;
            case "IRON": return 3;
            case "DIAMOND": return 4;
            default: return 0;
        }
    }

    public void reset() {
        eliminatedPlayers.clear();
        savedArmor.clear();
        savedAxe.clear();
        savedPickaxe.clear();
    }
}
