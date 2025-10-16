package myplg.myplg;

import myplg.myplg.commands.EditCommand;
import myplg.myplg.commands.EndCommand;
import myplg.myplg.commands.GeneCommand;
import myplg.myplg.commands.SaveCommand;
import myplg.myplg.commands.SetBedCommand;
import myplg.myplg.commands.StartCommand;
import myplg.myplg.data.GeneratorDataManager;
import myplg.myplg.data.TeamDataManager;
import myplg.myplg.data.WorldBackupManager;
import myplg.myplg.listeners.BedClickListener;
import myplg.myplg.listeners.GeneratorSelectionListener;
import myplg.myplg.listeners.GUIClickListener;
import myplg.myplg.listeners.MobSpawnListener;
import myplg.myplg.listeners.PlayerDeathListener;
import myplg.myplg.listeners.PlayerJoinListener;
import myplg.myplg.listeners.TimeControlListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPGame extends JavaPlugin {

    private GameManager gameManager;
    private GeneratorManager generatorManager;
    private TeamDataManager teamDataManager;
    private GeneratorDataManager generatorDataManager;
    private WorldBackupManager worldBackupManager;
    private SetBedCommand setBedCommand;
    private BedClickListener bedClickListener;
    private GUIClickListener guiClickListener;
    private boolean teamsLoaded = false;

    @Override
    public void onEnable() {
        // Initialize managers
        gameManager = new GameManager(this);
        generatorManager = new GeneratorManager(this);
        teamDataManager = new TeamDataManager(this);
        generatorDataManager = new GeneratorDataManager(this);
        worldBackupManager = new WorldBackupManager(this);

        // Load teams and generators from file after a delay to ensure worlds are loaded
        Bukkit.getScheduler().runTaskLater(this, () -> {
            teamDataManager.loadTeams();
            teamsLoaded = true;
            getLogger().info("Team data loading completed. Loaded " + gameManager.getTeams().size() + " teams.");

            generatorDataManager.loadGenerators();
            getLogger().info("Generator data loading completed. Loaded " + generatorManager.getGenerators().size() + " generators.");
        }, 20L); // 1 second delay

        // Initialize commands
        setBedCommand = new SetBedCommand(this);
        bedClickListener = new BedClickListener(this);
        guiClickListener = new GUIClickListener(this);

        // Register commands
        getCommand("setbed").setExecutor(setBedCommand);
        getCommand("start").setExecutor(new StartCommand(this));
        getCommand("edit").setExecutor(new EditCommand(this));
        getCommand("save").setExecutor(new SaveCommand(this));
        getCommand("end").setExecutor(new EndCommand(this));
        getCommand("gene").setExecutor(new GeneCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(bedClickListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(guiClickListener, this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new GeneratorSelectionListener(this), this);

        // Start time control
        TimeControlListener timeControl = new TimeControlListener(this);
        timeControl.startTimeControl();

        getLogger().info("PvPGame has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save teams before disabling, but only if teams were loaded
        if (teamDataManager != null && gameManager != null && teamsLoaded) {
            getLogger().info("Saving teams on disable. Current team count: " + gameManager.getTeams().size());
            teamDataManager.saveTeams();
        } else if (!teamsLoaded) {
            getLogger().warning("Teams were not fully loaded yet, skipping save to prevent data loss.");
        }

        // Save generators
        if (generatorDataManager != null && generatorManager != null) {
            getLogger().info("Saving generators on disable. Current generator count: " + generatorManager.getGenerators().size());
            generatorDataManager.saveGenerators();
        }

        // Stop all generators
        if (generatorManager != null) {
            generatorManager.stopAllGenerators();
        }

        getLogger().info("PvPGame has been disabled!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public SetBedCommand getSetBedCommand() {
        return setBedCommand;
    }

    public BedClickListener getBedClickListener() {
        return bedClickListener;
    }

    public GUIClickListener getGUIClickListener() {
        return guiClickListener;
    }

    public TeamDataManager getTeamDataManager() {
        return teamDataManager;
    }

    public WorldBackupManager getWorldBackupManager() {
        return worldBackupManager;
    }

    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    public GeneratorDataManager getGeneratorDataManager() {
        return generatorDataManager;
    }
}
