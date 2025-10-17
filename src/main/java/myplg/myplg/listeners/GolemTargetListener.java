package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
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
 */
public class GolemTargetListener implements Listener {
    private final PvPGame plugin;

    public GolemTargetListener(PvPGame plugin) {
        this.plugin = plugin;
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
                findEnemyTarget(golem, ownerTeam);
            }
        }
    }

    private void findEnemyTarget(IronGolem golem, String ownerTeam) {
        // Find nearby enemy players within 16 blocks
        double range = 16.0;

        for (org.bukkit.entity.Entity entity : golem.getNearbyEntities(range, range, range)) {
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
