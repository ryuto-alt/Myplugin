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
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Controls Iron Golem targeting to only attack enemies
 * Limits detection to within 10 blocks of spawn location
 */
public class GolemTargetListener implements Listener {
    private final PvPGame plugin;
    private static final double MAX_DETECTION_RANGE = 10.0; // Maximum detection range from spawn point
    private static final double MAX_DETECTION_RANGE_SQUARED = MAX_DETECTION_RANGE * MAX_DETECTION_RANGE;

    public GolemTargetListener(PvPGame plugin) {
        this.plugin = plugin;
        // Start task to actively search for enemies
        startAggressiveDefenseTask();
    }

    /**
     * Task that runs every second to check for enemies near golem's spawn location
     */
    private void startAggressiveDefenseTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            // Check all Iron Golems in game world only - 負荷軽減
            org.bukkit.World gameWorld = Bukkit.getWorld("world");
            if (gameWorld == null) return;
            
            for (IronGolem golem : gameWorld.getEntitiesByClass(IronGolem.class)) {
                // Check if golem has owner team metadata
                List<MetadataValue> metadata = golem.getMetadata("ownerTeam");
                if (metadata == null || metadata.isEmpty()) {
                    continue;
                }

                String ownerTeam = metadata.get(0).asString();
                if (ownerTeam == null) {
                    continue;
                }

                // If golem doesn't have a target, search for enemies
                if (golem.getTarget() == null) {
                    findEnemyNearSpawn(golem, ownerTeam);
                }
            }
        }, 0L, 40L); // 2秒毎に変更 - 負荷軽減 // Run every second
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

            // Cancel targeting if player has invisibility effect
            if (targetPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                event.setCancelled(true);
                findEnemyNearSpawn(golem, ownerTeam);
                return;
            }

            String targetTeam = plugin.getGameManager().getPlayerTeam(targetPlayer.getUniqueId());

            // Cancel targeting if player is on same team or no team
            if (targetTeam != null && targetTeam.equals(ownerTeam)) {
                event.setCancelled(true);
                // Try to find an enemy target instead
                findEnemyNearSpawn(golem, ownerTeam);
                return;
            }

            // Check if target is within 10 blocks of spawn point (squared for performance)
            Location spawnLoc = getSpawnLocation(golem);
            if (spawnLoc != null && targetPlayer.getLocation().distanceSquared(spawnLoc) > MAX_DETECTION_RANGE_SQUARED) {
                event.setCancelled(true);
                // Try to find a closer enemy
                findEnemyNearSpawn(golem, ownerTeam);
            }
        }
    }
    
    /**
     * Get the golem's spawn location from metadata
     */
    private Location getSpawnLocation(IronGolem golem) {
        List<MetadataValue> xMeta = golem.getMetadata("spawnX");
        List<MetadataValue> yMeta = golem.getMetadata("spawnY");
        List<MetadataValue> zMeta = golem.getMetadata("spawnZ");
        
        if (xMeta.isEmpty() || yMeta.isEmpty() || zMeta.isEmpty()) {
            return null;
        }
        
        return new Location(
            golem.getWorld(),
            xMeta.get(0).asDouble(),
            yMeta.get(0).asDouble(),
            zMeta.get(0).asDouble()
        );
    }

    /**
     * Search for enemies within 10 blocks of golem's spawn location
     */
    private void findEnemyNearSpawn(IronGolem golem, String ownerTeam) {
        Location spawnLoc = getSpawnLocation(golem);
        if (spawnLoc == null) {
            // Fallback: use golem's current location if no spawn recorded
            spawnLoc = golem.getLocation();
        }

        // Find enemy players within 10 blocks of spawn location
        Player closestEnemy = null;
        double closestDistance = Double.MAX_VALUE; // Using squared distance

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip if player has invisibility effect
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                continue;
            }

            String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

            // Skip if same team or no team
            if (playerTeam == null || playerTeam.equals(ownerTeam)) {
                continue;
            }

            // Check if player is in same world
            if (!player.getWorld().equals(spawnLoc.getWorld())) {
                continue;
            }

            // Check distance to spawn location (squared for performance)
            double distanceSquared = player.getLocation().distanceSquared(spawnLoc);
            if (distanceSquared <= MAX_DETECTION_RANGE_SQUARED && distanceSquared < closestDistance) {
                closestEnemy = player;
                closestDistance = distanceSquared;
            }
        }

        // Target the closest enemy near spawn
        if (closestEnemy != null) {
            golem.setTarget(closestEnemy);
        }
    }
}
