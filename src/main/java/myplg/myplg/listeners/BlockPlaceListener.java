package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Block block = event.getBlock();

        // Skip bed blocks - they are handled by BedBreakListener
        if (isBed(block.getType())) {
            return;
        }

        // Check if this block is part of a generator region
        if (isGeneratorBlock(block)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cジェネレーターのブロックは破壊できません！");
            return;
        }

        // Only allow breaking player-placed blocks
        if (!isPlayerPlaced(block)) {
            event.setCancelled(true);
            return;
        }

        // Remove from tracking when broken
        removePlayerPlacedBlock(block);
    }

    private boolean isGeneratorBlock(Block block) {
        // Check if this block is within any generator region
        for (myplg.myplg.Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
            if (isBlockInRegion(block, generator)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockInRegion(Block block, myplg.myplg.Generator generator) {
        Location loc = block.getLocation();
        Location corner1 = generator.getCorner1();
        Location corner2 = generator.getCorner2();

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ &&
               loc.getWorld().equals(corner1.getWorld());
    }

    private boolean isBed(Material material) {
        return material.name().contains("BED") && !material.name().equals("BEDROCK");
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
