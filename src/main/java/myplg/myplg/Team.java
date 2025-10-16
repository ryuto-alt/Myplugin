package myplg.myplg;

import org.bukkit.Location;
import org.bukkit.block.Block;

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
