package myplg.myplg;

import myplg.myplg.commands.EditCommand;
import myplg.myplg.commands.SetBedCommand;
import myplg.myplg.commands.StartCommand;
import myplg.myplg.data.TeamDataManager;
import myplg.myplg.listeners.BedClickListener;
import myplg.myplg.listeners.GUIClickListener;
import myplg.myplg.listeners.MobSpawnListener;
import myplg.myplg.listeners.PlayerDeathListener;
import myplg.myplg.listeners.PlayerJoinListener;
import myplg.myplg.listeners.TimeControlListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPGame extends JavaPlugin {

    private GameManager gameManager;
    private TeamDataManager teamDataManager;
    private SetBedCommand setBedCommand;
    private BedClickListener bedClickListener;
    private GUIClickListener guiClickListener;

    @Override
    public void onEnable() {
        // Initialize managers
        gameManager = new GameManager(this);
        teamDataManager = new TeamDataManager(this);

        // Load teams from file
        teamDataManager.loadTeams();

        // Initialize commands
        setBedCommand = new SetBedCommand(this);
        bedClickListener = new BedClickListener(this);
        guiClickListener = new GUIClickListener(this);

        // Register commands
        getCommand("setbed").setExecutor(setBedCommand);
        getCommand("start").setExecutor(new StartCommand(this));
        getCommand("edit").setExecutor(new EditCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(bedClickListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(guiClickListener, this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);

        // Start time control
        TimeControlListener timeControl = new TimeControlListener(this);
        timeControl.startTimeControl();

        getLogger().info("PvPGame has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save teams before disabling
        if (teamDataManager != null) {
            teamDataManager.saveTeams();
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
}
