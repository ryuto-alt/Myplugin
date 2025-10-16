package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedClickListener implements Listener {
    private final PvPGame plugin;
    private final Map<UUID, String> waitingPlayers;

    public BedClickListener(PvPGame plugin) {
        this.plugin = plugin;
        this.waitingPlayers = new HashMap<>();
    }

    public void setWaitingPlayer(UUID playerUUID, String teamName) {
        waitingPlayers.put(playerUUID, teamName);
    }

    @EventHandler
    public void onBedClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        Material blockType = clickedBlock.getType();
        if (!isBed(blockType)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!waitingPlayers.containsKey(playerUUID)) {
            return;
        }

        event.setCancelled(true);

        String teamName = waitingPlayers.get(playerUUID);

        // Get the bed's head block
        Block bedHead = getBedHead(clickedBlock);
        if (bedHead == null) {
            player.sendMessage("ベッドの設定に失敗しました。");
            return;
        }

        // Calculate spawn location at the head of the bed
        Location spawnLocation = calculateBedHeadSpawn(bedHead);

        // Create team
        Team team = new Team(teamName, bedHead, spawnLocation);
        plugin.getGameManager().addTeam(team);

        player.sendMessage("チーム「" + teamName + "」のベッドを設定しました。");
        waitingPlayers.remove(playerUUID);
        plugin.getSetBedCommand().removeWaitingPlayer(playerUUID);
    }

    private boolean isBed(Material material) {
        return material.name().contains("BED") && !material.name().equals("BEDROCK");
    }

    private Block getBedHead(Block bedBlock) {
        if (bedBlock.getBlockData() instanceof Bed) {
            Bed bedData = (Bed) bedBlock.getBlockData();
            if (bedData.getPart() == Bed.Part.HEAD) {
                return bedBlock;
            } else {
                // Get the head part
                return bedBlock.getRelative(bedData.getFacing());
            }
        }
        return null;
    }

    private Location calculateBedHeadSpawn(Block bedHead) {
        if (bedHead.getBlockData() instanceof Bed) {
            Bed bedData = (Bed) bedHead.getBlockData();
            Location bedLocation = bedHead.getLocation().add(0.5, 1, 0.5);

            // Set yaw based on bed facing direction
            float yaw = 0;
            switch (bedData.getFacing()) {
                case NORTH:
                    yaw = 180;
                    break;
                case SOUTH:
                    yaw = 0;
                    break;
                case WEST:
                    yaw = 90;
                    break;
                case EAST:
                    yaw = -90;
                    break;
            }

            bedLocation.setYaw(yaw);
            bedLocation.setPitch(0);

            return bedLocation;
        }
        return bedHead.getLocation().add(0.5, 1, 0.5);
    }
}
