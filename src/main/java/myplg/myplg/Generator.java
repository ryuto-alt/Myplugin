package myplg.myplg;

import org.bukkit.Location;
import org.bukkit.Material;

public class Generator {
    private final String id;
    private final Material material;
    private final Location corner1;
    private final Location corner2;
    private int spawnInterval; // in ticks (20 ticks = 1 second)

    public Generator(String id, Material material, Location corner1, Location corner2, int spawnInterval) {
        this.id = id;
        this.material = material;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.spawnInterval = spawnInterval;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public int getSpawnInterval() {
        return spawnInterval;
    }

    public void setSpawnInterval(int spawnInterval) {
        this.spawnInterval = spawnInterval;
    }

    public Location getRandomLocationInRegion() {
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        double x = minX + Math.random() * (maxX - minX);
        double y = minY + Math.random() * (maxY - minY);
        double z = minZ + Math.random() * (maxZ - minZ);

        return new Location(corner1.getWorld(), x, y, z);
    }
}
