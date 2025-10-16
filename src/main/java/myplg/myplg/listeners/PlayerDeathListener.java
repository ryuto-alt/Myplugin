package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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

    public PlayerDeathListener(PvPGame plugin) {
        this.plugin = plugin;
        this.eliminatedPlayers = new HashSet<>();
        this.savedArmor = new HashMap<>();
        this.savedAxe = new HashMap<>();
        this.savedPickaxe = new HashMap<>();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String teamName = plugin.getGameManager().getPlayerTeam(playerUUID);

        // Check if bed is alive
        boolean bedAlive = plugin.getScoreboardManager().isBedAlive(teamName);

        if (!bedAlive) {
            // Player is eliminated
            eliminatedPlayers.add(playerUUID);
            event.setDeathMessage("§c" + player.getName() + " は脱落しました！");
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

        // Clear all drops except nothing (we handle what they keep)
        event.getDrops().clear();
        event.setKeepInventory(false);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        String teamName = plugin.getGameManager().getPlayerTeam(playerUUID);
        if (teamName == null) {
            return;
        }

        // Check if player is eliminated
        if (eliminatedPlayers.contains(playerUUID)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("§cあなたは脱落しました。観戦モードになります。");
            return;
        }

        // Set respawn location to team spawn
        event.setRespawnLocation(plugin.getGameManager().getTeam(teamName).getSpawnLocation());

        // Restore equipment after a tick
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    restorePlayerEquipment(player, playerUUID, teamName);
                }
            }
        }.runTaskLater(plugin, 1L);
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
