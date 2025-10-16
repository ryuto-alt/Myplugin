package myplg.myplg.data;

import myplg.myplg.PvPGame;
import myplg.myplg.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class TeamDataManager {
    private final PvPGame plugin;
    private final File dataFile;
    private YamlConfiguration config;

    public TeamDataManager(PvPGame plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "teams.yml");
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
                plugin.getLogger().severe("Failed to create teams.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveTeams() {
        config = new YamlConfiguration();

        plugin.getLogger().info("Saving " + plugin.getGameManager().getTeams().size() + " teams...");

        for (Team team : plugin.getGameManager().getTeams().values()) {
            String path = "teams." + team.getName();
            plugin.getLogger().info("Saving team: " + team.getName());

            // Save bed location
            Block bed = team.getBedBlock();
            config.set(path + ".bed.world", bed.getWorld().getName());
            config.set(path + ".bed.x", bed.getX());
            config.set(path + ".bed.y", bed.getY());
            config.set(path + ".bed.z", bed.getZ());

            // Save spawn location
            Location spawn = team.getSpawnLocation();
            config.set(path + ".spawn.world", spawn.getWorld().getName());
            config.set(path + ".spawn.x", spawn.getX());
            config.set(path + ".spawn.y", spawn.getY());
            config.set(path + ".spawn.z", spawn.getZ());
            config.set(path + ".spawn.yaw", spawn.getYaw());
            config.set(path + ".spawn.pitch", spawn.getPitch());

            // Save members
            int index = 0;
            for (UUID memberUUID : team.getMembers()) {
                config.set(path + ".members." + index, memberUUID.toString());
                index++;
            }
        }

        try {
            config.save(dataFile);
            plugin.getLogger().info("Teams saved successfully.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save teams.yml: " + e.getMessage());
        }
    }

    public void loadTeams() {
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection == null) {
            plugin.getLogger().info("No teams to load.");
            return;
        }

        plugin.getLogger().info("Loading teams from config...");
        plugin.getLogger().info("Found " + teamsSection.getKeys(false).size() + " teams in config.");

        for (String teamName : teamsSection.getKeys(false)) {
            plugin.getLogger().info("===== Loading team: " + teamName + " =====");
            String path = "teams." + teamName;

            try {
                // Load bed location
                String worldName = config.getString(path + ".bed.world");
                plugin.getLogger().info("  Bed world: " + worldName);
                if (worldName == null) {
                    plugin.getLogger().severe("Bed world name is null for team " + teamName);
                    continue;
                }

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World " + worldName + " not found for team " + teamName);
                    continue;
                }

                int bedX = config.getInt(path + ".bed.x");
                int bedY = config.getInt(path + ".bed.y");
                int bedZ = config.getInt(path + ".bed.z");
                plugin.getLogger().info("  Bed location: " + bedX + ", " + bedY + ", " + bedZ);
                Block bed = world.getBlockAt(bedX, bedY, bedZ);

                // Load spawn location
                String spawnWorldName = config.getString(path + ".spawn.world");
                plugin.getLogger().info("  Spawn world: " + spawnWorldName);
                if (spawnWorldName == null) {
                    plugin.getLogger().severe("Spawn world name is null for team " + teamName);
                    continue;
                }

                World spawnWorld = Bukkit.getWorld(spawnWorldName);
                if (spawnWorld == null) {
                    plugin.getLogger().warning("Spawn world " + spawnWorldName + " not found for team " + teamName);
                    continue;
                }

                double spawnX = config.getDouble(path + ".spawn.x");
                double spawnY = config.getDouble(path + ".spawn.y");
                double spawnZ = config.getDouble(path + ".spawn.z");
                float spawnYaw = (float) config.getDouble(path + ".spawn.yaw");
                float spawnPitch = (float) config.getDouble(path + ".spawn.pitch");
                plugin.getLogger().info("  Spawn location: " + spawnX + ", " + spawnY + ", " + spawnZ);

                plugin.getLogger().info("  Creating Location object...");
                Location spawnLocation = new Location(spawnWorld, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);

                plugin.getLogger().info("  Creating Team object...");
                // Create team
                Team team = new Team(teamName, bed, spawnLocation);

                // Load members
                ConfigurationSection membersSection = config.getConfigurationSection(path + ".members");
                if (membersSection != null) {
                    plugin.getLogger().info("  Loading " + membersSection.getKeys(false).size() + " members...");
                    for (String key : membersSection.getKeys(false)) {
                        String uuidString = config.getString(path + ".members." + key);
                        try {
                            UUID memberUUID = UUID.fromString(uuidString);
                            team.addMember(memberUUID);
                            plugin.getGameManager().setPlayerTeam(memberUUID, teamName);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID for team " + teamName + ": " + uuidString);
                        }
                    }
                } else {
                    plugin.getLogger().info("  No members to load.");
                }

                plugin.getLogger().info("  Adding team to GameManager...");
                plugin.getGameManager().addTeam(team, false);
                plugin.getLogger().info("✓ Successfully loaded team: " + teamName);

            } catch (Exception e) {
                plugin.getLogger().severe("✗ Failed to load team " + teamName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
