package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

/**
 * Controls Iron Golem targeting to only attack enemies
 * Aggressively defends team bed (20 block radius)
 */
public class GolemTargetListener implements Listener {
    private final PvPGame plugin;
    private static final double BED_DEFENSE_RADIUS = 20.0; // Bed defense radius
    private static final double SEARCH_RANGE = 16.0; // Normal search range

    public GolemTargetListener(PvPGame plugin) {
        this.plugin = plugin;
        // Start task to actively search for enemies near bed
        startAggressiveDefenseTask();
    }

    /**
     * Task that runs every second to check for enemies near team beds
     */
    private void startAggressiveDefenseTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            // Check all Iron Golems
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (IronGolem golem : world.getEntitiesByClass(IronGolem.class)) {
                    // Check if golem has owner team metadata
                    List<MetadataValue> metadata = golem.getMetadata("ownerTeam");
                    if (metadata == null || metadata.isEmpty()) {
                        continue;
                    }

                    String ownerTeam = metadata.get(0).asString();
                    if (ownerTeam == null) {
                        continue;
                    }

                    // If golem doesn't have a target, search for enemies near bed
                    if (golem.getTarget() == null) {
                        findEnemyNearBed(golem, ownerTeam);
                    }
                }
            }
        }, 0L, 20L); // Run every second
    }

    @EventHandler
    public void onGolemTarget(EntityTargetEvent event) {
        // Only handle Iron Golems
        if (!(event.getEntity() instanceof IronGolem)) {
            return;
        }

        IronGolem golem = (IronGolem) event.getEntity();

        // Check if golem has owner team metadata
        List<MetadataValue> metadata = golem.getMetadata("ownerTeam");
        if (metadata == null || metadata.isEmpty()) {
            return; // Not our golem
        }

        String ownerTeam = metadata.get(0).asString();
        if (ownerTeam == null) {
            return;
        }

        // Check if target is a player
        if (event.getTarget() instanceof Player) {
            Player targetPlayer = (Player) event.getTarget();
            String targetTeam = plugin.getGameManager().getPlayerTeam(targetPlayer.getUniqueId());

            // Cancel targeting if player is on same team or no team
            if (targetTeam != null && targetTeam.equals(ownerTeam)) {
                event.setCancelled(true);
                // Try to find an enemy target instead
                findEnemyNearBed(golem, ownerTeam);
            }
        }
    }

    /**
     * Search for enemies near the team's bed
     */
    private void findEnemyNearBed(IronGolem golem, String ownerTeam) {
        // Get team's bed location
        Team team = plugin.getGameManager().getTeam(ownerTeam);
        if (team == null || team.getBedBlock() == null) {
            // Bed destroyed, use normal search
            findEnemyTarget(golem, ownerTeam);
            return;
        }

        Location bedLocation = team.getBedBlock().getLocation();

        // Find enemy players within 20 blocks of the bed
        Player closestEnemy = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

            // Skip if same team or no team
            if (playerTeam == null || playerTeam.equals(ownerTeam)) {
                continue;
            }

            // Check if player is in same world
            if (!player.getWorld().equals(bedLocation.getWorld())) {
                continue;
            }

            // Check distance to bed
            double distance = player.getLocation().distance(bedLocation);
            if (distance <= BED_DEFENSE_RADIUS && distance < closestDistance) {
                closestEnemy = player;
                closestDistance = distance;
            }
        }

        // Target the closest enemy near bed
        if (closestEnemy != null) {
            golem.setTarget(closestEnemy);
            return;
        }

        // No enemies near bed, use normal search around golem
        findEnemyTarget(golem, ownerTeam);
    }

    /**
     * Search for enemies near the golem
     */
    private void findEnemyTarget(IronGolem golem, String ownerTeam) {
        // Find nearby enemy players within 16 blocks
        for (org.bukkit.entity.Entity entity : golem.getNearbyEntities(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

                // Target enemy players only
                if (playerTeam != null && !playerTeam.equals(ownerTeam)) {
                    golem.setTarget(player);
                    return;
                }
            }
        }
    }
}
