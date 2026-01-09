package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import myplg.myplg.gui.OmegaLastGUI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class OmegaLastListener implements Listener {
    private final PvPGame plugin;
    private final OmegaLastGUI omegaLastGUI;
    private final Map<String, Long> teamCooldowns; // Team name -> cooldown end time
    private static final long COOLDOWN_MS = 10 * 60 * 1000; // 10 minutes
    private static final double BED_RANGE = 10.0; // Must be within 10m of own bed
    private static final double EXPLOSION_RADIUS = 4.0;
    private static final double MAX_DAMAGE = 10.0; // 5 hearts
    private static final double KNOCKBACK_STRENGTH = 1.5;

    public OmegaLastListener(PvPGame plugin) {
        this.plugin = plugin;
        this.omegaLastGUI = new OmegaLastGUI(plugin);
        this.teamCooldowns = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player right-clicked with Ω-LAST
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta() &&
            item.getItemMeta().hasDisplayName() &&
            item.getItemMeta().getDisplayName().equals("§c§lΩ-LAST")) {

            event.setCancelled(true);

            // Check if game is running
            if (!plugin.getGameManager().isGameRunning()) {
                player.sendMessage("§cゲームが開始されていません！");
                return;
            }

            // Get player's team
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            if (teamName == null) {
                player.sendMessage("§cチームに所属していません！");
                return;
            }

            // Check cooldown
            if (isOnCooldown(teamName)) {
                long remainingMs = teamCooldowns.get(teamName) - System.currentTimeMillis();
                long remainingMinutes = remainingMs / 60000;
                long remainingSeconds = (remainingMs % 60000) / 1000;
                player.sendMessage("§cΩ-LASTはクールダウン中です！ 残り: " + remainingMinutes + "分" + remainingSeconds + "秒");
                return;
            }

            // Check if player is within 10m of their own bed
            Team team = plugin.getGameManager().getTeam(teamName);
            if (team == null || team.getBedBlock() == null) {
                player.sendMessage("§cチームのベッドが見つかりません！");
                return;
            }

            Location bedLoc = team.getBedBlock().getLocation();
            if (player.getLocation().distance(bedLoc) > BED_RANGE) {
                player.sendMessage("§c自分のベッドから10m以内でないと使用できません！");
                return;
            }

            // Open enemy team selection GUI
            omegaLastGUI.openTeamSelectionGUI(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals("§c§lΩ-LAST - 敵チーム選択")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Ignore glass panes
        if (clickedItem.getType().toString().contains("GLASS_PANE")) {
            return;
        }

        // Check if it's a team wool
        if (!clickedItem.getType().toString().endsWith("_WOOL")) {
            return;
        }

        if (!clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();
        // Extract team name from display name (format: §e§l<teamName>)
        String targetTeamName = ChatColor.stripColor(displayName);

        // Get target team
        Team targetTeam = plugin.getGameManager().getTeam(targetTeamName);
        if (targetTeam == null || targetTeam.getBedBlock() == null) {
            player.sendMessage("§c選択したチームのベッドが見つかりません！");
            player.closeInventory();
            return;
        }

        // Close GUI
        player.closeInventory();

        // Get player's team name
        String playerTeamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
        if (playerTeamName == null) {
            return;
        }

        // Remove the item from player's inventory
        removeOmegaLastItem(player);

        // Set cooldown for the team
        teamCooldowns.put(playerTeamName, System.currentTimeMillis() + COOLDOWN_MS);

        // Execute Ω-LAST
        executeOmegaLast(player, playerTeamName, targetTeam, targetTeamName);
    }

    private void executeOmegaLast(Player player, String playerTeamName, Team targetTeam, String targetTeamName) {
        Location targetBedLoc = targetTeam.getBedBlock().getLocation().clone().add(0.5, 1, 0.5);

        // Broadcast warning to all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c§l⚠ " + playerTeamName + "チームがΩ-LASTを発動！！警戒せよ！！ ⚠");
            p.sendTitle("§c§l⚠ 警告 ⚠", "§e" + playerTeamName + "チームがΩ-LASTを発動！", 10, 60, 20);
            // Play custom warning sound from texture pack
            p.playSound(p.getLocation(), "gamesound.warn", org.bukkit.SoundCategory.MASTER, 1.0f, 1.0f);
        }

        // Play global alert sound for 5 seconds
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // 5 seconds = 100 ticks
                    this.cancel();
                    // After 5 seconds, launch the player
                    launchPlayer(player, targetBedLoc, targetTeamName);
                    return;
                }

                // Countdown messages
                int secondsLeft = (100 - ticks) / 20;
                if (ticks % 20 == 0 && secondsLeft > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage("§c§l" + secondsLeft + "秒後に" + playerTeamName + "が" + targetTeamName + "のベッドに突撃！");
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Visual effect on player during countdown
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100 || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Particle effect around player
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void launchPlayer(Player player, Location targetLoc, String targetTeamName) {
        if (!player.isOnline()) {
            return;
        }

        Location startLoc = player.getLocation().clone();
        Location finalTarget = targetLoc.clone().add(0, 10, 0); // 10 blocks above bed

        // Play launch sound
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.5f);

        // Broadcast launch message
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c§l" + player.getName() + "が" + targetTeamName + "のベッドに向けて発射！！");
        }

        // Calculate total flight time (60 ticks = 3 seconds)
        final int FLIGHT_DURATION = 60;
        final double totalDistance = startLoc.distance(finalTarget);

        // Flight animation - interpolate between start and target
        new BukkitRunnable() {
            int ticks = 0;
            Location lastLoc = startLoc.clone();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                ticks++;

                // Calculate progress (0.0 to 1.0)
                double progress = (double) ticks / FLIGHT_DURATION;

                if (progress >= 1.0) {
                    // Flight complete - teleport to final position above bed
                    this.cancel();

                    // Find safe landing spot above bed
                    Location landingLoc = targetLoc.clone().add(0, 1, 0);
                    player.teleport(landingLoc);

                    // Small delay before explosion
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                createExplosion(player.getLocation(), targetLoc);
                            }
                        }
                    }.runTaskLater(plugin, 5L);
                    return;
                }

                // Use ease-in-out curve for smoother flight
                double easedProgress = progress < 0.5
                    ? 2 * progress * progress
                    : 1 - Math.pow(-2 * progress + 2, 2) / 2;

                // Calculate arc height (parabolic arc, highest at middle)
                double arcHeight = 30 * (1 - Math.pow(2 * progress - 1, 2)); // Max 30 blocks high

                // Interpolate position
                double x = startLoc.getX() + (finalTarget.getX() - startLoc.getX()) * easedProgress;
                double z = startLoc.getZ() + (finalTarget.getZ() - startLoc.getZ()) * easedProgress;
                double baseY = startLoc.getY() + (finalTarget.getY() - startLoc.getY()) * easedProgress;
                double y = baseY + arcHeight;

                Location newLoc = new Location(player.getWorld(), x, y, z);

                // Look towards target
                Vector direction = targetLoc.toVector().subtract(newLoc.toVector()).normalize();
                newLoc.setDirection(direction);

                // Teleport player
                player.teleport(newLoc);

                // Trail particles at previous location
                player.getWorld().spawnParticle(Particle.FLAME, lastLoc, 8, 0.2, 0.2, 0.2, 0.05);
                player.getWorld().spawnParticle(Particle.SMOKE, lastLoc, 5, 0.1, 0.1, 0.1, 0.02);

                // Rocket particles around player
                if (ticks % 2 == 0) {
                    player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 3, 0.3, 0.3, 0.3, 0.1);
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.3f, 1.5f);
                }

                lastLoc = newLoc.clone();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createExplosion(Location playerLoc, Location bedLoc) {
        // Use bed location as explosion center
        Location explosionCenter = bedLoc.clone();

        // Play explosion sound
        explosionCenter.getWorld().playSound(explosionCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);

        // Spawn explosion particles
        explosionCenter.getWorld().spawnParticle(Particle.EXPLOSION, explosionCenter, 5, 1.5, 1.5, 1.5, 0);
        explosionCenter.getWorld().spawnParticle(Particle.FLAME, explosionCenter, 50, 2, 2, 2, 0.1);
        explosionCenter.getWorld().spawnParticle(Particle.SMOKE, explosionCenter, 30, 2, 2, 2, 0.05);

        // Destroy blocks around the bed (only player-placed blocks)
        destroyBlocksAroundBed(explosionCenter);

        // Damage and knockback nearby players
        for (Player nearbyPlayer : explosionCenter.getWorld().getPlayers()) {
            double distance = nearbyPlayer.getLocation().distance(explosionCenter);

            if (distance <= EXPLOSION_RADIUS) {
                // Calculate damage based on distance (max 5 hearts at center)
                double damageMultiplier = 1.0 - (distance / EXPLOSION_RADIUS);
                double damage = MAX_DAMAGE * damageMultiplier;

                // Apply damage
                nearbyPlayer.damage(damage);

                // Apply knockback (TNT-like)
                Vector knockback = nearbyPlayer.getLocation().toVector()
                    .subtract(explosionCenter.toVector())
                    .normalize()
                    .multiply(KNOCKBACK_STRENGTH * damageMultiplier);
                knockback.setY(knockback.getY() + 0.5);
                nearbyPlayer.setVelocity(nearbyPlayer.getVelocity().add(knockback));

                // Play hurt sound
                nearbyPlayer.playSound(nearbyPlayer.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            }
        }

        // Broadcast landing message
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c§l💥 Ω-LASTが着弾！！ 💥");
        }
    }

    private void destroyBlocksAroundBed(Location center) {
        int radius = 3; // Destroy blocks within 3 block radius

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location blockLoc = center.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();

                    // Only destroy player-placed blocks (not the bed itself or natural terrain)
                    if (BlockPlaceListener.isPlayerPlaced(block)) {
                        // Explosion particle for each destroyed block
                        block.getWorld().spawnParticle(Particle.BLOCK, 
                            blockLoc.add(0.5, 0.5, 0.5), 
                            10, 
                            0.3, 0.3, 0.3, 
                            0.1, 
                            block.getBlockData());

                        // Remove the block
                        BlockPlaceListener.removePlayerPlacedBlock(block);
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void removeOmegaLastItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.NETHER_STAR &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().equals("§c§lΩ-LAST")) {

                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                break;
            }
        }
    }

    public boolean isOnCooldown(String teamName) {
        Long cooldownEnd = teamCooldowns.get(teamName);
        if (cooldownEnd == null) {
            return false;
        }
        return System.currentTimeMillis() < cooldownEnd;
    }

    public void resetCooldowns() {
        teamCooldowns.clear();
    }

    public OmegaLastGUI getOmegaLastGUI() {
        return omegaLastGUI;
    }

    /**
     * Create an Ω-LAST item
     */
    public static ItemStack createOmegaLastItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lΩ-LAST");
            meta.setLore(Arrays.asList(
                "§7敵チームのベッドに突撃する",
                "§7最終兵器",
                "",
                "§c⚠ 使用条件:",
                "§7・自分のベッドから10m以内",
                "§7・チーム毎に10分のクールダウン",
                "",
                "§e右クリックで使用"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
