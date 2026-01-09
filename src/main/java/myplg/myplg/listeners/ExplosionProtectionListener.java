package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
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
import org.bukkit.util.Vector;

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
        // Also protect glass blocks from explosions
        event.blockList().removeIf(block -> !BlockPlaceListener.isPlayerPlaced(block) || isGlassBlock(block));

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
        // Also protect glass blocks from explosions
        event.blockList().removeIf(block -> !BlockPlaceListener.isPlayerPlaced(block) || isGlassBlock(block));

        // Remove destroyed blocks from tracking
        for (Block block : event.blockList()) {
            BlockPlaceListener.removePlayerPlacedBlock(block);
        }
    }

    /**
     * Check if the block is a glass type (glass, glass pane, stained glass, etc.)
     */
    private boolean isGlassBlock(Block block) {
        Material type = block.getType();
        return type == Material.GLASS ||
               type == Material.GLASS_PANE ||
               type.name().contains("STAINED_GLASS") ||
               type.name().contains("TINTED_GLASS");
    }

    /**
     * Reduce TNT damage to players to maximum 3 hearts (6.0 damage)
     * while increasing knockback effect significantly
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Check if damage is from TNT explosion
        if (event.getDamager().getType() == EntityType.TNT && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            TNTPrimed tnt = (TNTPrimed) event.getDamager();
            double damage = event.getDamage();

            // Cap damage at 6.0 (3 hearts)
            if (damage > 6.0) {
                event.setDamage(6.0);
                plugin.getLogger().info("TNT damage reduced from " + damage + " to 6.0 for player " + event.getEntity().getName());
            }

            // Apply knockback (reduced by 20% from previous 1.5 multiplier)
            Vector knockbackDirection = player.getLocation().toVector().subtract(tnt.getLocation().toVector()).normalize();
            // Knockback multiplier: 1.5 * 0.8 = 1.2
            double knockbackMultiplier = 1.2;
            Vector knockback = knockbackDirection.multiply(knockbackMultiplier);
            // Add upward velocity for better air effect
            knockback.setY(knockback.getY() + 0.5);

            // Schedule the velocity change to apply after damage calculation
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.setVelocity(player.getVelocity().add(knockback));
            });
        }
    }
}
