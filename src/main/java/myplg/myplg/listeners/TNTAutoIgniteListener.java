package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * Automatically ignites TNT when placed and reduces TNT knockback
 */
public class TNTAutoIgniteListener implements Listener {
    private final PvPGame plugin;

    public TNTAutoIgniteListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTNTPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (block.getType() == Material.TNT) {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            // Cancel the block place event
            event.setCancelled(true);

            // Spawn primed TNT at the block location
            TNTPrimed tnt = block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(60); // 3 seconds (60 ticks)
            tnt.setYield(3.6f); // Default 4.0 * 0.9 = 3.6 (10% weaker explosion)

            // Remove TNT from player's inventory using scheduler to avoid item duplication
            org.bukkit.entity.Player player = event.getPlayer();
            org.bukkit.inventory.ItemStack itemInHand = player.getInventory().getItemInMainHand();

            // Schedule removal on next tick to ensure proper removal
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (itemInHand != null && itemInHand.getType() == Material.TNT) {
                    if (itemInHand.getAmount() > 1) {
                        itemInHand.setAmount(itemInHand.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.AIR));
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTNTDamage(EntityDamageByEntityEvent event) {
        // Check if damage is from TNT explosion
        if (event.getDamager() instanceof TNTPrimed && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // Schedule velocity reduction for next tick (after explosion knockback is applied)
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                Vector velocity = player.getVelocity();
                // Reduce knockback by 20% (multiply by 0.8)
                velocity.multiply(0.8);
                player.setVelocity(velocity);
            });
        }
    }
}
