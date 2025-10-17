package myplg.myplg;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Team {
    private final String name;
    private final Block bedBlock;
    private final Location spawnLocation;
    private final List<UUID> members;

    public Team(String name, Block bedBlock, Location spawnLocation) {
        this.name = name;
        this.bedBlock = bedBlock;
        this.spawnLocation = spawnLocation;
        this.members = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Block getBedBlock() {
        return bedBlock;
    }

    public Location getSpawnLocation() {
        // Calculate spawn location 5 blocks in front of bed head
        return calculateBedSpawnLocation();
    }

    /**
     * Calculates spawn location 5 blocks in front of bed head
     */
    private Location calculateBedSpawnLocation() {
        if (bedBlock == null || bedBlock.getType() == Material.AIR) {
            return spawnLocation; // Fallback to original spawn if bed is destroyed
        }

        // Get bed data
        if (bedBlock.getBlockData() instanceof Bed) {
            Bed bedData = (Bed) bedBlock.getBlockData();
            Block headBlock;

            // Find head block
            if (bedData.getPart() == Bed.Part.HEAD) {
                headBlock = bedBlock;
            } else {
                // Get the head block from foot
                Vector direction = bedData.getFacing().getDirection();
                headBlock = bedBlock.getRelative(
                    (int) direction.getX(),
                    0,
                    (int) direction.getZ()
                );
            }

            // Calculate 5 blocks in front of head
            Vector facing = bedData.getFacing().getDirection();
            Location bedHeadLoc = headBlock.getLocation().add(0.5, 0, 0.5);
            Location spawnLoc = bedHeadLoc.clone().add(
                facing.getX() * 5,
                0,
                facing.getZ() * 5
            );

            // Set to center of block and add small height offset
            spawnLoc.setY(bedHeadLoc.getY() + 0.1);

            return spawnLoc;
        }

        // Fallback to original spawn location
        return spawnLocation;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID playerUUID) {
        members.add(playerUUID);
    }

    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
    }

    public boolean hasMember(UUID playerUUID) {
        return members.contains(playerUUID);
    }
}
