package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class BedBreakListener implements Listener {
    private final PvPGame plugin;

    public BedBreakListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBedBreak(BlockBreakEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Block block = event.getBlock();
        Material blockType = block.getType();

        // Check if it's a bed
        if (!isBed(blockType)) {
            return;
        }

        // Mark that this event has been handled by bed listener
        event.setCancelled(false);

        Player player = event.getPlayer();
        String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

        // Find which team's bed this is
        for (Team team : plugin.getGameManager().getTeams().values()) {
            Block teamBedBlock = team.getBedBlock();
            if (teamBedBlock == null) continue;

            // Check if this bed belongs to this team
            if (isSameBed(block, teamBedBlock)) {
                // Check if player is breaking their own team's bed
                if (team.getName().equals(playerTeam)) {
                    // Cancel - player cannot break their own bed
                    event.setCancelled(true);
                    // Restore the bed block immediately (no message)
                    return;
                }

                // Enemy team breaking bed - allow it but prevent drops
                event.setDropItems(false);

                // Mark bed as destroyed
                plugin.getScoreboardManager().setBedStatus(team.getName(), false);

                // Broadcast message
                Bukkit.broadcastMessage("§c§l" + team.getName() + " チームのベッドが破壊されました！");
                Bukkit.broadcastMessage("§c" + team.getName() + " チームはリスポーンできなくなります！");

                return;
            }
        }
    }

    // Protect beds from explosions
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        event.blockList().removeIf(block -> {
            if (isBed(block.getType())) {
                // Check if this is a team bed
                for (Team team : plugin.getGameManager().getTeams().values()) {
                    Block teamBedBlock = team.getBedBlock();
                    if (teamBedBlock != null && isSameBed(block, teamBedBlock)) {
                        return true; // Remove from explosion list (protect bed)
                    }
                }
            }
            return false;
        });
    }

    // Protect beds from block explosions (TNT, etc.)
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        event.blockList().removeIf(block -> {
            if (isBed(block.getType())) {
                // Check if this is a team bed
                for (Team team : plugin.getGameManager().getTeams().values()) {
                    Block teamBedBlock = team.getBedBlock();
                    if (teamBedBlock != null && isSameBed(block, teamBedBlock)) {
                        return true; // Remove from explosion list (protect bed)
                    }
                }
            }
            return false;
        });
    }

    private boolean isBed(Material material) {
        return material.name().contains("BED") && !material.name().equals("BEDROCK");
    }

    private boolean isSameBed(Block block1, Block block2) {
        // Check if blocks are part of the same bed
        // Compare locations (accounting for head/foot parts)
        if (block1.getLocation().equals(block2.getLocation())) {
            return true;
        }

        // Check adjacent blocks for bed parts
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block adjacent = block1.getRelative(x, 0, z);
                if (adjacent.getLocation().equals(block2.getLocation()) && isBed(adjacent.getType())) {
                    return true;
                }
            }
        }

        return false;
    }
}
