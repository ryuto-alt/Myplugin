package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Controls fall damage to only start from 5 blocks and above
 */
public class FallDamageListener implements Listener {
    private final PvPGame plugin;
    private static final double SAFE_FALL_DISTANCE = 5.0; // Blocks

    public FallDamageListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onFallDamage(EntityDamageEvent event) {
        // Only handle fall damage for players
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL ||
            !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Disable fall damage in lobby world
        if (player.getWorld().getName().equalsIgnoreCase("lobby")) {
            event.setCancelled(true);
            return;
        }

        // Only apply custom fall damage during the game
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        double fallDistance = player.getFallDistance();

        // Cancel fall damage if less than 5 blocks
        if (fallDistance < SAFE_FALL_DISTANCE) {
            event.setCancelled(true);
            return;
        }

        // Calculate damage for falls 5 blocks and above
        // Vanilla formula: damage = fallDistance - 3
        // New formula: damage = fallDistance - 5
        double damage = fallDistance - SAFE_FALL_DISTANCE;
        event.setDamage(Math.max(0, damage));
    }
}
