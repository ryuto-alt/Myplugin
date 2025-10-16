package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.Set;

public class BlockPlaceListener implements Listener {
    private final PvPGame plugin;
    private static final Set<Block> playerPlacedBlocks = new HashSet<>();

    public BlockPlaceListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        if (!event.isCancelled()) {
            // Track this block as player-placed
            playerPlacedBlocks.add(event.getBlock());
        }
    }

    public static boolean isPlayerPlaced(Block block) {
        return playerPlacedBlocks.contains(block);
    }

    public static void clearPlayerPlacedBlocks() {
        playerPlacedBlocks.clear();
    }

    public static void removePlayerPlacedBlock(Block block) {
        playerPlacedBlocks.remove(block);
    }
}
