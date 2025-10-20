package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.listeners.BlockPlaceListener;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Prevent actual death screen from appearing during game
        if (plugin.getGameManager().isGameRunning()) {
            // This should never happen if our damage handler works, but just in case
            event.setCancelled(true);
            Player player = event.getEntity();
            plugin.getLogger().warning("Player " + player.getName() + " triggered actual death event - this shouldn't happen!");

            // Force handle as custom death
            if (!processingDeath.contains(player.getUniqueId())) {
                processingDeath.add(player.getUniqueId());
                player.setHealth(0.1);
                handleInstantDeath(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerUUID = player.getUniqueId();

        // Check if player will die from this damage (health <= 0.5 after damage to prevent actual death)
        double healthAfterDamage = player.getHealth() - event.getFinalDamage();

        if (healthAfterDamage <= 0.5 && !processingDeath.contains(playerUUID)) {
            // Cancel the damage event to prevent actual death
            event.setCancelled(true);

            // Mark as processing
            processingDeath.add(playerUUID);

            // Set health to 0.1 to keep player alive but appear "dead"
            player.setHealth(0.1);

            // Trigger custom death handling (spectator mode)
            handleInstantDeath(player);
        }
    }

    private void handleInstantDeath(Player player) {
        UUID playerUUID = player.getUniqueId();
        String teamName = plugin.getGameManager().getPlayerTeam(playerUUID);

        plugin.getLogger().info("=== Death Handler Started for " + player.getName() + " ===");
        plugin.getLogger().info("Team: " + teamName);

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
        plugin.getLogger().info("Bed alive: " + bedAlive);

        if (!bedAlive) {
            // Player is eliminated
            plugin.getLogger().info("Player eliminated - no respawn");
            eliminatedPlayers.add(playerUUID);
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            player.sendTitle("§c§l死んでしまった！", "§cあなたは脱落しました", 10, 70, 20);

            // Set health to max to prevent issues
            player.setHealth(player.getMaxHealth());

            // Update scoreboard to reflect player elimination
            plugin.getScoreboardManager().updateAllScoreboards();

            // Check if team is completely eliminated
            checkTeamElimination(teamName);

            processingDeath.remove(playerUUID);
            return;
        }

        plugin.getLogger().info("Player will respawn in 5 seconds");

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

        // Set health to max to prevent any death issues
        player.setHealth(player.getMaxHealth());

        // Teleport to (0, 125, 0) in the same world
        Location spectatorLocation = new Location(player.getWorld(), 0.5, 125.0, 0.5);
        player.teleport(spectatorLocation);

        // Get team spawn location
        Location spawnLocation = plugin.getGameManager().getTeam(teamName).getSpawnLocation();

        // Teleport to spawn after 5 seconds and restore equipment
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getGameManager().isGameRunning()) {
                // Set to survival mode FIRST
                player.setGameMode(GameMode.SURVIVAL);

                // Set health to max
                player.setHealth(player.getMaxHealth());

                // Teleport to spawn
                player.teleport(spawnLocation);

                // Restore equipment
                restorePlayerEquipment(player, playerUUID, teamName);

                // Remove from processing
                processingDeath.remove(playerUUID);

                plugin.getLogger().info("Player " + player.getName() + " respawned successfully");
            } else {
                plugin.getLogger().warning("Failed to respawn player " + player.getName() + " - player offline or game not running");
                processingDeath.remove(playerUUID);
            }
        }, 100L); // 5 seconds
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

    /**
     * Public method to check victory condition - can be called from other listeners
     */
    public void checkVictoryCondition() {
        // Count teams that are NOT eliminated (either have bed alive OR have alive players)
        int remainingTeams = 0;
        String winningTeam = null;

        for (myplg.myplg.Team team : plugin.getGameManager().getTeams().values()) {
            String teamName = team.getName();
            boolean bedAlive = plugin.getScoreboardManager().isBedAlive(teamName);

            // Count alive players in team
            int aliveCount = 0;
            for (UUID memberUUID : team.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberUUID);
                if (member != null && member.isOnline() && !eliminatedPlayers.contains(memberUUID)) {
                    aliveCount++;
                }
            }

            // Team is still in the game if bed is alive OR has alive players
            if (bedAlive || aliveCount > 0) {
                remainingTeams++;
                winningTeam = teamName;
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
                endGame();
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

                    Firework firework = (Firework) location.getWorld().spawnEntity(fireworkLoc, EntityType.FIREWORK_ROCKET);
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

    private void endGame() {
        // End game
        plugin.getGameManager().setGameRunning(false);

        // Stop all generators
        plugin.getGeneratorManager().stopAllGenerators();

        // Clear player-placed blocks tracking
        BlockPlaceListener.clearPlayerPlacedBlocks();

        // Get the game world (assuming first player's world or default world)
        World gameWorld = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            gameWorld = player.getWorld();
            break;
        }
        if (gameWorld == null) {
            gameWorld = Bukkit.getWorlds().get(0);
        }

        // Clear all players' inventories and teleport to lobby
        World lobbyWorld = Bukkit.getWorlds().get(0);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(lobbyWorld.getSpawnLocation());
        }

        Bukkit.broadcastMessage("§e§lゲームが終了しました！");
        Bukkit.broadcastMessage("§6ワールドを復元しています...");

        final String worldName = gameWorld.getName();

        // Check if backup exists and restore world
        if (plugin.getWorldBackupManager().hasBackup(worldName)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                boolean success = plugin.getWorldBackupManager().restoreWorld(worldName);

                if (success) {
                    Bukkit.broadcastMessage("§aワールド「" + worldName + "」の復元が完了しました！");
                } else {
                    Bukkit.broadcastMessage("§cワールドの復元に失敗しました。");
                }
            }, 60L); // 3 seconds delay
        } else {
            Bukkit.broadcastMessage("§c警告: ワールドのバックアップが見つかりません。");
        }
    }

    public void reset() {
        eliminatedPlayers.clear();
        savedArmor.clear();
        savedAxe.clear();
        savedPickaxe.clear();
    }

    /**
     * Clear processing death set for clean game restart
     */
    public void clearProcessingDeaths() {
        processingDeath.clear();
    }
}
