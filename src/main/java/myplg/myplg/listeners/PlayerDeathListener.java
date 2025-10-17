package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
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

        // Save all armor BEFORE clearing inventory (leather, iron, diamond, netherite)
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] savedArmorArray = new ItemStack[4];
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && isArmor(armor[i].getType())) {
                savedArmorArray[i] = armor[i].clone();
            }
        }
        savedArmor.put(playerUUID, savedArmorArray);

        // Check if bed is alive
        boolean bedAlive = plugin.getScoreboardManager().isBedAlive(teamName);

        if (!bedAlive) {
            // Player is eliminated
            eliminatedPlayers.add(playerUUID);
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            player.sendTitle("§c§l死んでしまった！", "§cあなたは脱落しました", 10, 70, 20);

            // Update scoreboard to reflect player elimination
            plugin.getScoreboardManager().updateAllScoreboards();

            // Check if team is completely eliminated
            checkTeamElimination(teamName);

            processingDeath.remove(playerUUID);
            return;
        }

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

        // Clear inventory after saving everything
        player.getInventory().clear();

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

        // Restore all saved armor (including leather armor)
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

    private boolean isArmor(Material material) {
        String name = material.toString();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
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

    private void checkTeamElimination(String teamName) {
        myplg.myplg.Team team = plugin.getGameManager().getTeam(teamName);
        if (team == null) return;

        // Count alive players in team
        int aliveCount = 0;
        for (UUID memberUUID : team.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberUUID);
            if (member != null && member.isOnline() && !eliminatedPlayers.contains(memberUUID)) {
                aliveCount++;
            }
        }

        // If no players alive, team is eliminated
        if (aliveCount == 0) {
            plugin.getScoreboardManager().setTeamEliminated(teamName);
            plugin.getServer().broadcastMessage("§c§l" + teamName + " チームが全滅しました！");

            // Check for victory
            checkVictoryCondition();
        }
    }

    private void checkVictoryCondition() {
        // Count remaining teams with alive players
        int remainingTeams = 0;
        String winningTeam = null;

        for (myplg.myplg.Team team : plugin.getGameManager().getTeams().values()) {
            int aliveCount = 0;
            for (UUID memberUUID : team.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberUUID);
                if (member != null && member.isOnline() && !eliminatedPlayers.contains(memberUUID)) {
                    aliveCount++;
                }
            }

            if (aliveCount > 0) {
                remainingTeams++;
                winningTeam = team.getName();
            }
        }

        // If only one team remains, they win
        if (remainingTeams == 1 && winningTeam != null) {
            announceVictory(winningTeam);
        }
    }

    private void announceVictory(String winningTeam) {
        // Get winning team color
        String teamColorCode = getTeamColorCode(winningTeam);

        // Show victory title to all players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

            if (winningTeam.equals(playerTeam)) {
                // Winner's title
                Component title = Component.text("§6§l⭐ VICTORY! ⭐");
                Component subtitle = Component.text(teamColorCode + "§l" + winningTeam + " チームの勝利！");

                Title victoryTitle = Title.title(
                    title,
                    subtitle,
                    Title.Times.times(
                        Duration.ofMillis(1000),  // fade in
                        Duration.ofMillis(5000),  // stay
                        Duration.ofMillis(2000)   // fade out
                    )
                );
                player.showTitle(victoryTitle);

                // Launch fireworks around winner
                launchFireworks(player.getLocation(), teamColorCode);
            } else {
                // Loser's title
                Component title = Component.text("§c§lGAME OVER");
                Component subtitle = Component.text(teamColorCode + "§l" + winningTeam + " チームの勝利！");

                Title defeatTitle = Title.title(
                    title,
                    subtitle,
                    Title.Times.times(
                        Duration.ofMillis(1000),  // fade in
                        Duration.ofMillis(5000),  // stay
                        Duration.ofMillis(2000)   // fade out
                    )
                );
                player.showTitle(defeatTitle);
            }
        }

        // Broadcast message
        plugin.getServer().broadcastMessage("");
        plugin.getServer().broadcastMessage(teamColorCode + "§l═══════════════════════════");
        plugin.getServer().broadcastMessage("§6§l⭐ " + teamColorCode + "§l" + winningTeam + " チームの勝利！ §6§l⭐");
        plugin.getServer().broadcastMessage(teamColorCode + "§l═══════════════════════════");
        plugin.getServer().broadcastMessage("");

        // End game after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getGameManager().setGameRunning(false);
                plugin.getServer().broadcastMessage("§eゲームが終了しました。");
            }
        }.runTaskLater(plugin, 200L); // 10 seconds
    }

    private void launchFireworks(Location location, String teamColorCode) {
        // Launch 5 fireworks with 0.5 second intervals
        for (int i = 0; i < 5; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Random offset around player
                    double offsetX = (Math.random() - 0.5) * 4;
                    double offsetZ = (Math.random() - 0.5) * 4;
                    Location fireworkLoc = location.clone().add(offsetX, 0, offsetZ);

                    Firework firework = (Firework) location.getWorld().spawnEntity(fireworkLoc, EntityType.FIREWORK);
                    FireworkMeta meta = firework.getFireworkMeta();

                    // Get team color
                    Color fireworkColor = getFireworkColor(teamColorCode);

                    // Create firework effect
                    FireworkEffect effect = FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(fireworkColor)
                        .withFade(Color.YELLOW)
                        .withFlicker()
                        .withTrail()
                        .build();

                    meta.addEffect(effect);
                    meta.setPower(1);
                    firework.setFireworkMeta(meta);
                }
            }.runTaskLater(plugin, i * 10L); // 0.5 second intervals
        }
    }

    private String getTeamColorCode(String teamName) {
        switch (teamName) {
            case "レッド": return "§c";
            case "ブルー": return "§9";
            case "グリーン": return "§a";
            case "イエロー": return "§e";
            case "アクア": return "§b";
            case "ホワイト": return "§f";
            case "ピンク": return "§d";
            case "グレー": return "§7";
            default: return "§f";
        }
    }

    private Color getFireworkColor(String colorCode) {
        switch (colorCode) {
            case "§c": return Color.RED;
            case "§9": return Color.BLUE;
            case "§a": return Color.GREEN;
            case "§e": return Color.YELLOW;
            case "§b": return Color.AQUA;
            case "§f": return Color.WHITE;
            case "§d": return Color.FUCHSIA;
            case "§7": return Color.GRAY;
            default: return Color.WHITE;
        }
    }

    public void reset() {
        eliminatedPlayers.clear();
        savedArmor.clear();
        savedAxe.clear();
        savedPickaxe.clear();
    }
}
