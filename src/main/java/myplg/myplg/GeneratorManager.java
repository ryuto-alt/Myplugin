package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GeneratorManager {
    private final PvPGame plugin;
    private final Map<String, Generator> generators;
    private final Map<String, BukkitTask> generatorTasks;

    public GeneratorManager(PvPGame plugin) {
        this.plugin = plugin;
        this.generators = new HashMap<>();
        this.generatorTasks = new HashMap<>();
    }

    public void addGenerator(String id, String teamName, Material material, Location corner1, Location corner2, int spawnInterval) {
        addGenerator(id, teamName, material, corner1, corner2, spawnInterval, true);
    }

    public void addGenerator(String id, String teamName, Material material, Location corner1, Location corner2, int spawnInterval, boolean save) {
        Generator generator = new Generator(id, teamName, material, corner1, corner2, spawnInterval);
        generators.put(id, generator);
        plugin.getLogger().info("Generator added: " + id + " (Team: " + teamName + ", " + material.name() + ")");

        if (save && plugin.getGeneratorDataManager() != null) {
            plugin.getGeneratorDataManager().saveGenerators();
        }
    }

    public void removeGenerator(String id) {
        generators.remove(id);
        stopGenerator(id);
        plugin.getLogger().info("Generator removed: " + id);

        if (plugin.getGeneratorDataManager() != null) {
            plugin.getGeneratorDataManager().saveGenerators();
        }
    }

    public Generator getGenerator(String id) {
        return generators.get(id);
    }

    public Map<String, Generator> getGenerators() {
        return generators;
    }

    public void startAllGenerators() {
        plugin.getLogger().info("Starting all generators... Total: " + generators.size());
        if (generators.isEmpty()) {
            plugin.getLogger().warning("No generators found! Use /gene to create generators.");
        }
        for (Generator generator : generators.values()) {
            plugin.getLogger().info("Starting generator: " + generator.getId() +
                " (Material: " + generator.getMaterial().name() +
                ", Interval: " + generator.getSpawnInterval() + " ticks)");
            startGenerator(generator);
        }
    }

    public void stopAllGenerators() {
        plugin.getLogger().info("Stopping all generators...");
        for (String id : new ArrayList<>(generatorTasks.keySet())) {
            stopGenerator(id);
        }
    }

    private void startGenerator(Generator generator) {
        // Cancel existing task if running
        stopGenerator(generator.getId());

        // Start new repeating task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            spawnItem(generator);
        }, 0L, generator.getSpawnInterval());

        generatorTasks.put(generator.getId(), task);
        plugin.getLogger().info("Started generator: " + generator.getId() + " (interval: " + generator.getSpawnInterval() + " ticks)");
    }

    private void stopGenerator(String id) {
        BukkitTask task = generatorTasks.remove(id);
        if (task != null) {
            task.cancel();
            plugin.getLogger().info("Stopped generator: " + id);
        }
    }

    private void spawnItem(Generator generator) {
        Location spawnLocation = generator.getRandomLocationInRegion();

        // Get the minimum Y from the selected region (the floor of the selection)
        double minY = Math.min(generator.getCorner1().getY(), generator.getCorner2().getY());

        // Spawn 1 block above the floor of the selected region
        Location dropLocation = new Location(
            spawnLocation.getWorld(),
            spawnLocation.getX(),
            minY + 1,
            spawnLocation.getZ()
        );

        // Drop the item (ingot, diamond, or emerald)
        dropLocation.getWorld().dropItemNaturally(dropLocation, new ItemStack(generator.getMaterial(), 1));

        plugin.getLogger().info("Spawned " + generator.getMaterial().name() + " at " +
            String.format("(%.1f, %.1f, %.1f) for generator " + generator.getId(),
            dropLocation.getX(), dropLocation.getY(), dropLocation.getZ()));
    }

    public void updateGeneratorInterval(String id, int newInterval) {
        Generator generator = generators.get(id);
        if (generator != null) {
            generator.setSpawnInterval(newInterval);

            // Restart generator with new interval if game is running
            if (plugin.getGameManager().isGameRunning()) {
                startGenerator(generator);
            }

            plugin.getLogger().info("Updated generator interval: " + id + " -> " + newInterval + " ticks");

            if (plugin.getGeneratorDataManager() != null) {
                plugin.getGeneratorDataManager().saveGenerators();
            }
        }
    }

    public void reset() {
        stopAllGenerators();
        generators.clear();
    }
}
