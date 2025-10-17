package myplg.myplg;

import myplg.myplg.commands.EditCommand;
import myplg.myplg.commands.EndCommand;
import myplg.myplg.commands.GeneCommand;
import myplg.myplg.commands.SaveCommand;
import myplg.myplg.commands.SetBedCommand;
import myplg.myplg.commands.Shop1Command;
import myplg.myplg.commands.Shop2Command;
import myplg.myplg.commands.ShopResetCommand;
import myplg.myplg.commands.StartCommand;
import myplg.myplg.data.GeneratorDataManager;
import myplg.myplg.data.ShopDataManager;
import myplg.myplg.data.TeamDataManager;
import myplg.myplg.data.WorldBackupManager;
import myplg.myplg.listeners.ArmorRemoveListener;
import myplg.myplg.listeners.BedBreakListener;
import myplg.myplg.listeners.BedClickListener;
import myplg.myplg.listeners.BlockPlaceListener;
import myplg.myplg.listeners.ExplosionProtectionListener;
import myplg.myplg.listeners.GeneratorSelectionListener;
import myplg.myplg.listeners.GUIClickListener;
import myplg.myplg.listeners.MobSpawnListener;
import myplg.myplg.listeners.PlayerDeathListener;
import myplg.myplg.listeners.PlayerJoinListener;
import myplg.myplg.listeners.ShopClickListener;
import myplg.myplg.listeners.ShopTwoListener;
import myplg.myplg.listeners.ShopVillagerListener;
import myplg.myplg.listeners.TimeControlListener;
import myplg.myplg.listeners.VoidDeathListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPGame extends JavaPlugin {

    private GameManager gameManager;
    private GeneratorManager generatorManager;
    private TeamDataManager teamDataManager;
    private GeneratorDataManager generatorDataManager;
    private ShopDataManager shopDataManager;
    private WorldBackupManager worldBackupManager;
    private ToolUpgradeManager toolUpgradeManager;
    private TerritoryUpgradeManager territoryUpgradeManager;
    private ScoreboardManager scoreboardManager;
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
        shopDataManager = new ShopDataManager(this);
        worldBackupManager = new WorldBackupManager(this);
        toolUpgradeManager = new ToolUpgradeManager(this);
        territoryUpgradeManager = new TerritoryUpgradeManager(this);
        scoreboardManager = new ScoreboardManager(this);

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
        getCommand("shop1").setExecutor(new Shop1Command(this));
        getCommand("shop2").setExecutor(new Shop2Command(this));
        getCommand("sreset").setExecutor(new ShopResetCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(bedClickListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(guiClickListener, this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new GeneratorSelectionListener(this), this);

        // Shop system listeners - need to link them together
        ShopVillagerListener shopVillagerListener = new ShopVillagerListener(this);
        ShopTwoListener shopTwoListener = new ShopTwoListener(this);
        ShopClickListener shopClickListener = new ShopClickListener(this);
        shopClickListener.setVillagerListener(shopVillagerListener);
        shopClickListener.setShopTwoListener(shopTwoListener);

        getServer().getPluginManager().registerEvents(shopVillagerListener, this);
        getServer().getPluginManager().registerEvents(shopTwoListener, this);
        getServer().getPluginManager().registerEvents(shopClickListener, this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.FireballListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorRemoveListener(this), this);
        getServer().getPluginManager().registerEvents(new BedBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new VoidDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionProtectionListener(this), this);

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

    public ShopDataManager getShopDataManager() {
        return shopDataManager;
    }

    public ToolUpgradeManager getToolUpgradeManager() {
        return toolUpgradeManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TerritoryUpgradeManager getTerritoryUpgradeManager() {
        return territoryUpgradeManager;
    }
}
