package myplg.myplg.data;

import myplg.myplg.Generator;
import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class GeneratorDataManager {
    private final PvPGame plugin;
    private final File dataFile;
    private YamlConfiguration config;

    public GeneratorDataManager(PvPGame plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "generators.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create generators.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveGenerators() {
        config = new YamlConfiguration();

        plugin.getLogger().info("Saving " + plugin.getGeneratorManager().getGenerators().size() + " generators...");

        for (Generator generator : plugin.getGeneratorManager().getGenerators().values()) {
            String path = "generators." + generator.getId();
            plugin.getLogger().info("Saving generator: " + generator.getId());

            // Save material
            config.set(path + ".material", generator.getMaterial().name());

            // Save corner1
            Location corner1 = generator.getCorner1();
            config.set(path + ".corner1.world", corner1.getWorld().getName());
            config.set(path + ".corner1.x", corner1.getX());
            config.set(path + ".corner1.y", corner1.getY());
            config.set(path + ".corner1.z", corner1.getZ());

            // Save corner2
            Location corner2 = generator.getCorner2();
            config.set(path + ".corner2.world", corner2.getWorld().getName());
            config.set(path + ".corner2.x", corner2.getX());
            config.set(path + ".corner2.y", corner2.getY());
            config.set(path + ".corner2.z", corner2.getZ());

            // Save spawn interval
            config.set(path + ".spawnInterval", generator.getSpawnInterval());
        }

        try {
            config.save(dataFile);
            plugin.getLogger().info("Generators saved successfully.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save generators.yml: " + e.getMessage());
        }
    }

    public void loadGenerators() {
        ConfigurationSection generatorsSection = config.getConfigurationSection("generators");
        if (generatorsSection == null) {
            plugin.getLogger().info("No generators to load.");
            return;
        }

        plugin.getLogger().info("Loading generators from config...");
        plugin.getLogger().info("Found " + generatorsSection.getKeys(false).size() + " generators in config.");

        for (String generatorId : generatorsSection.getKeys(false)) {
            plugin.getLogger().info("===== Loading generator: " + generatorId + " =====");
            String path = "generators." + generatorId;

            try {
                // Load material
                String materialName = config.getString(path + ".material");
                if (materialName == null) {
                    plugin.getLogger().severe("Material is null for generator " + generatorId);
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().severe("Invalid material for generator " + generatorId + ": " + materialName);
                    continue;
                }

                // Load corner1
                String world1Name = config.getString(path + ".corner1.world");
                if (world1Name == null) {
                    plugin.getLogger().severe("Corner1 world is null for generator " + generatorId);
                    continue;
                }

                World world1 = Bukkit.getWorld(world1Name);
                if (world1 == null) {
                    plugin.getLogger().warning("World " + world1Name + " not found for generator " + generatorId);
                    continue;
                }

                double corner1X = config.getDouble(path + ".corner1.x");
                double corner1Y = config.getDouble(path + ".corner1.y");
                double corner1Z = config.getDouble(path + ".corner1.z");
                Location corner1 = new Location(world1, corner1X, corner1Y, corner1Z);

                // Load corner2
                String world2Name = config.getString(path + ".corner2.world");
                if (world2Name == null) {
                    plugin.getLogger().severe("Corner2 world is null for generator " + generatorId);
                    continue;
                }

                World world2 = Bukkit.getWorld(world2Name);
                if (world2 == null) {
                    plugin.getLogger().warning("World " + world2Name + " not found for generator " + generatorId);
                    continue;
                }

                double corner2X = config.getDouble(path + ".corner2.x");
                double corner2Y = config.getDouble(path + ".corner2.y");
                double corner2Z = config.getDouble(path + ".corner2.z");
                Location corner2 = new Location(world2, corner2X, corner2Y, corner2Z);

                // Load spawn interval
                int spawnInterval = config.getInt(path + ".spawnInterval", 100);

                // Add generator (without saving to avoid loop)
                plugin.getGeneratorManager().addGenerator(generatorId, material, corner1, corner2, spawnInterval, false);
                plugin.getLogger().info("✓ Successfully loaded generator: " + generatorId);

            } catch (Exception e) {
                plugin.getLogger().severe("✗ Failed to load generator " + generatorId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
