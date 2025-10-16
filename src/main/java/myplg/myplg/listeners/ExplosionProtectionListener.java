package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
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
}
