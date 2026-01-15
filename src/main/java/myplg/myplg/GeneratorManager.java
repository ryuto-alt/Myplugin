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
    // Track bonus spawn credits for each generator (when team members are nearby)
    private final Map<String, Double> generatorBonusCredits;
    // Track emerald evolution tasks for each team (level 3 upgrade)
    private final Map<String, BukkitTask> emeraldEvolutionTasks;
    private final Random random = new Random();

    // Emerald evolution spawn interval: 6 minutes = 360 seconds = 7200 ticks
    private static final int EMERALD_EVOLUTION_INTERVAL = 7200;

    public GeneratorManager(PvPGame plugin) {
        this.plugin = plugin;
        this.generators = new HashMap<>();
        this.generatorTasks = new HashMap<>();
        this.generatorBonusCredits = new HashMap<>();
        this.emeraldEvolutionTasks = new HashMap<>();
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
        // Check if team members with territory upgrade are nearby
        boolean hasNearbyUpgradedTeamMember = checkNearbyUpgradedTeamMembers(generator);

        // If upgrade is active, spawn 2 items instead of 1 (100% faster)
        int totalSpawns = hasNearbyUpgradedTeamMember ? 2 : 1;

        // DUO mode bonus: 1.2x for iron and gold (20% chance to spawn extra)
        Material mat = generator.getMaterial();
        if (plugin.getGameManager().getGameMode() == GameMode.DUO) {
            if (mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT) {
                // 20% chance to spawn an extra item
                if (random.nextDouble() < 0.2) {
                    totalSpawns++;
                }
            }
        }

        for (int i = 0; i < totalSpawns; i++) {
            spawnSingleItem(generator);
        }
    }

    private boolean checkNearbyUpgradedTeamMembers(Generator generator) {
        String teamName = generator.getTeamName();

        // Check if this team has purchased the territory upgrade
        if (plugin.getTerritoryUpgradeManager().getUpgradeLevel(teamName) < 2) {
            return false; // No upgrade purchased
        }

        // Check for nearby team members (within 10 blocks of generator center)
        Location center = generator.getRandomLocationInRegion();
        double range = 10.0;

        for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (entity instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) entity;
                String playerTeam = plugin.getGameManager().getPlayerTeam(player.getUniqueId());

                if (playerTeam != null && playerTeam.equals(teamName)) {
                    return true; // Found a team member nearby
                }
            }
        }

        return false;
    }

    private void spawnSingleItem(Generator generator) {
        Location spawnLocation = generator.getRandomLocationInRegion();

        // Get the minimum Y from the selected region (the floor of the selection)
        double minY = Math.min(generator.getCorner1().getY(), generator.getCorner2().getY());

        // For single-block generators (diamond/emerald), spawn directly above the block center
        boolean isSingleBlock = generator.getCorner1().equals(generator.getCorner2());
        Location dropLocation;

        if (isSingleBlock) {
            // Spawn at the center of the block, 1 block above
            dropLocation = new Location(
                spawnLocation.getWorld(),
                generator.getCorner1().getBlockX() + 0.5,
                minY + 1.0,
                generator.getCorner1().getBlockZ() + 0.5
            );
        } else {
            // Spawn randomly within the region for area-based generators
            dropLocation = new Location(
                spawnLocation.getWorld(),
                spawnLocation.getX(),
                minY + 1.0,
                spawnLocation.getZ()
            );
        }

        // Check if iron generator and count existing iron items in the area
        if (generator.getMaterial() == Material.IRON_INGOT) {
            int existingIronCount = 0;

            // Count iron items within 5 block radius
            for (org.bukkit.entity.Entity entity : dropLocation.getWorld().getNearbyEntities(dropLocation, 5, 5, 5)) {
                if (entity instanceof org.bukkit.entity.Item) {
                    org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) entity;
                    if (itemEntity.getItemStack().getType() == Material.IRON_INGOT) {
                        existingIronCount += itemEntity.getItemStack().getAmount();
                    }
                }
            }

            // If already at max (3 stacks = 192), don't spawn more
            if (existingIronCount >= 192) {
                plugin.getLogger().info("Iron generator " + generator.getId() + " reached max capacity (192 items)");
                return;
            }
        }

        // Drop the item and set velocity to zero (no bouncing or movement)
        org.bukkit.entity.Item item = dropLocation.getWorld().dropItem(dropLocation, new ItemStack(generator.getMaterial(), 1));
        item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

        plugin.getLogger().info("Spawned " + generator.getMaterial().name() + " at " +
            String.format("(%.1f, %.1f, %.1f) for generator " + generator.getId(),
            dropLocation.getX(), dropLocation.getY(), dropLocation.getZ()));
    }

    public void updateGeneratorInterval(String id, int newInterval) {
        updateGeneratorInterval(id, newInterval, true);
    }

    /**
     * ジェネレーターの間隔を更新
     * @param id ジェネレーターID
     * @param newInterval 新しい間隔（ticks）
     * @param saveToFile trueならファイルに保存、falseなら一時的な変更（デバッグ用）
     */
    public void updateGeneratorInterval(String id, int newInterval, boolean saveToFile) {
        Generator generator = generators.get(id);
        if (generator != null) {
            generator.setSpawnInterval(newInterval);

            // Restart generator with new interval if game is running
            if (plugin.getGameManager().isGameRunning()) {
                startGenerator(generator);
            }

            plugin.getLogger().info("Updated generator interval: " + id + " -> " + newInterval + " ticks" + (saveToFile ? "" : " (temporary)"));

            if (saveToFile && plugin.getGeneratorDataManager() != null) {
                plugin.getGeneratorDataManager().saveGenerators();
            }
        }
    }

    /**
     * Updates all generators for a specific team to run 2x faster (100% boost)
     * ONLY when team members are nearby (within 10 blocks)
     * @param teamName The team name to upgrade generators for
     */
    public void upgradeTeamGenerators(String teamName) {
        // Note: This method is called when team purchases the upgrade
        // The actual speed upgrade is now applied dynamically in spawnItem()
        // based on nearby players, so we don't permanently change generator speeds here
        plugin.getLogger().info("Territory upgrade purchased for team: " + teamName);
        plugin.getLogger().info("Generators will now run 2x faster (spawn 2 items) when " + teamName + " members are nearby");
    }

    /**
     * Start emerald evolution spawning for a team (Level 3 upgrade)
     * Spawns 1 emerald every 6 minutes at the team's iron generator
     * @param teamName The team name
     */
    public void startEmeraldEvolution(String teamName) {
        // Cancel existing task if any
        stopEmeraldEvolution(teamName);

        // Find the team's iron generator
        Generator ironGenerator = null;
        for (Generator generator : generators.values()) {
            if (generator.getTeamName().equals(teamName) && generator.getMaterial() == Material.IRON_INGOT) {
                ironGenerator = generator;
                break;
            }
        }

        if (ironGenerator == null) {
            plugin.getLogger().warning("No iron generator found for team: " + teamName);
            return;
        }

        final Generator targetGenerator = ironGenerator;

        // Start emerald evolution task (spawn every 6 minutes)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            spawnEvolutionEmerald(targetGenerator);
        }, EMERALD_EVOLUTION_INTERVAL, EMERALD_EVOLUTION_INTERVAL);

        emeraldEvolutionTasks.put(teamName, task);
        plugin.getLogger().info("Started emerald evolution for team: " + teamName + " (every 6 minutes)");
    }

    /**
     * Stop emerald evolution for a team
     * @param teamName The team name
     */
    public void stopEmeraldEvolution(String teamName) {
        BukkitTask task = emeraldEvolutionTasks.remove(teamName);
        if (task != null) {
            task.cancel();
            plugin.getLogger().info("Stopped emerald evolution for team: " + teamName);
        }
    }

    /**
     * Stop all emerald evolution tasks
     */
    public void stopAllEmeraldEvolution() {
        for (String teamName : new ArrayList<>(emeraldEvolutionTasks.keySet())) {
            stopEmeraldEvolution(teamName);
        }
    }

    /**
     * Spawn evolution emerald at a generator location
     */
    private void spawnEvolutionEmerald(Generator generator) {
        double minY = Math.min(generator.getCorner1().getY(), generator.getCorner2().getY());
        Location dropLocation = new Location(
            generator.getCorner1().getWorld(),
            generator.getCorner1().getBlockX() + 0.5,
            minY + 1.0,
            generator.getCorner1().getBlockZ() + 0.5
        );

        org.bukkit.entity.Item item = dropLocation.getWorld().dropItem(dropLocation, new ItemStack(Material.EMERALD, 1));
        item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

        plugin.getLogger().info("Spawned evolution emerald for team " + generator.getTeamName() + " at " +
            String.format("(%.1f, %.1f, %.1f)", dropLocation.getX(), dropLocation.getY(), dropLocation.getZ()));
    }

    public void reset() {
        stopAllGenerators();
        stopAllEmeraldEvolution();
        generators.clear();
    }
}
