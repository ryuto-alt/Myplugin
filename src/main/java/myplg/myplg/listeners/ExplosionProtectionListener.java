package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplosionProtectionListener implements Listener {
    private final PvPGame plugin;

    public ExplosionProtectionListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Only allow player-placed blocks to be destroyed by explosions
        event.blockList().removeIf(block -> !BlockPlaceListener.isPlayerPlaced(block));

        // Remove destroyed blocks from tracking
        for (Block block : event.blockList()) {
            BlockPlaceListener.removePlayerPlacedBlock(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Only allow player-placed blocks to be destroyed by explosions
        event.blockList().removeIf(block -> !BlockPlaceListener.isPlayerPlaced(block));

        // Remove destroyed blocks from tracking
        for (Block block : event.blockList()) {
            BlockPlaceListener.removePlayerPlacedBlock(block);
        }
    }

    /**
     * Reduce TNT damage to players to maximum 3 hearts (6.0 damage)
     * while keeping the explosion effect for blocks
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Check if damage is from TNT explosion
        if (event.getDamager().getType() == EntityType.TNT && event.getEntity() instanceof Player) {
            double damage = event.getDamage();

            // Cap damage at 6.0 (3 hearts)
            if (damage > 6.0) {
                event.setDamage(6.0);
                plugin.getLogger().info("TNT damage reduced from " + damage + " to 6.0 for player " + event.getEntity().getName());
            }
        }
    }
}
