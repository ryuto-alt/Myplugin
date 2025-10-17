package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Automatically ignites TNT when placed
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

            // Remove TNT from player's hand
            if (event.getItemInHand().getAmount() > 1) {
                event.getItemInHand().setAmount(event.getItemInHand().getAmount() - 1);
            } else {
                event.getItemInHand().setType(Material.AIR);
            }
        }
    }
}
