package myplg.myplg;

import myplg.myplg.commands.EditCommand;
import myplg.myplg.commands.EndCommand;
import myplg.myplg.commands.GameWorldCommand;
import myplg.myplg.commands.GeneCommand;
import myplg.myplg.commands.GeneReloadCommand;
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
import myplg.myplg.listeners.HungerControlListener;
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
    private WeaponUpgradeManager weaponUpgradeManager;
    private ArmorUpgradeManager armorUpgradeManager;
    private ScoreboardManager scoreboardManager;
    private SetBedCommand setBedCommand;
    private BedClickListener bedClickListener;
    private GUIClickListener guiClickListener;
    private PlayerDeathListener playerDeathListener;
    private myplg.myplg.listeners.HealthRegenListener healthRegenListener;
    private myplg.myplg.listeners.NametagVisibilityListener nametagVisibilityListener;
    private boolean teamsLoaded = false;

    @Override
    public void onEnable() {
        // Load lobby world first
        loadLobbyWorld();

        // Load game world (world)
        loadGameWorld();

        // Initialize managers
        gameManager = new GameManager(this);
        generatorManager = new GeneratorManager(this);
        teamDataManager = new TeamDataManager(this);
        generatorDataManager = new GeneratorDataManager(this);
        shopDataManager = new ShopDataManager(this);
        worldBackupManager = new WorldBackupManager(this);
        toolUpgradeManager = new ToolUpgradeManager(this);
        territoryUpgradeManager = new TerritoryUpgradeManager(this);
        weaponUpgradeManager = new WeaponUpgradeManager(this);
        armorUpgradeManager = new ArmorUpgradeManager(this);
        scoreboardManager = new ScoreboardManager(this);

        // Load teams and generators from file after a delay to ensure worlds are loaded
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("===== Starting delayed data loading =====");
            getLogger().info("Available worlds:");
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                getLogger().info("  - " + w.getName());
            }

            teamDataManager.loadTeams();
            teamsLoaded = true;
            getLogger().info("Team data loading completed. Loaded " + gameManager.getTeams().size() + " teams.");

            generatorDataManager.loadGenerators();
            getLogger().info("Generator data loading completed. Loaded " + generatorManager.getGenerators().size() + " generators.");
        }, 40L); // 2 second delay to ensure world is fully loaded

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
        getCommand("genereload").setExecutor(new GeneReloadCommand(this));
        getCommand("shop1").setExecutor(new Shop1Command(this));
        getCommand("shop2").setExecutor(new Shop2Command(this));
        getCommand("sreset").setExecutor(new ShopResetCommand(this));
        getCommand("gameworld").setExecutor(new GameWorldCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(bedClickListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Initialize and register PlayerDeathListener
        playerDeathListener = new PlayerDeathListener(this);
        getServer().getPluginManager().registerEvents(playerDeathListener, this);

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

        // Load shops from config after worlds are loaded
        Bukkit.getScheduler().runTaskLater(this, () -> {
            shopVillagerListener.loadShopsFromConfig();
            shopTwoListener.loadShopsFromConfig();
            getLogger().info("Shop loading completed.");
        }, 40L); // 2 second delay to ensure worlds are fully loaded
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.FireballListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorRemoveListener(this), this);
        getServer().getPluginManager().registerEvents(new BedBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new VoidDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.UpgradeEnchantmentListener(this), this);
        getServer().getPluginManager().registerEvents(new HungerControlListener(this), this);

        // Register new listeners
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.AttackCooldownListener(this), this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.ItemModifierListener(this), this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.TNTAutoIgniteListener(this), this);

        // Initialize and register HealthRegenListener
        healthRegenListener = new myplg.myplg.listeners.HealthRegenListener(this);
        getServer().getPluginManager().registerEvents(healthRegenListener, this);

        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.WeaponDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.WaterBucketListener(this), this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.FallDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.GolemTargetListener(this), this);

        // Initialize and register NametagVisibilityListener
        nametagVisibilityListener = new myplg.myplg.listeners.NametagVisibilityListener(this);
        getServer().getPluginManager().registerEvents(nametagVisibilityListener, this);

        // Register BridgeBuilderListener
        getServer().getPluginManager().registerEvents(new myplg.myplg.listeners.BridgeBuilderListener(this), this);

        // Start time control
        TimeControlListener timeControl = new TimeControlListener(this);
        timeControl.startTimeControl();

        // Start territory heal task (runs every 2 seconds = 40 ticks)
        TerritoryHealTask healTask = new TerritoryHealTask(this);
        healTask.runTaskTimer(this, 0L, 40L);

        getLogger().info("PvPGame has been enabled!");
    }

    @Override
    public void onDisable() {
        // DON'T save teams on disable to prevent overwriting manual changes to teams.yml
        // Teams are only saved when modified through commands like /setbed or during game end
        getLogger().info("Skipping team save on disable to preserve manual edits.");

        // DON'T save generators on disable to prevent overwriting manual changes to generators.yml
        // Use /genereload command to reload generators from generators.yml
        // Generators are only saved when modified through commands like /gene

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

    public WeaponUpgradeManager getWeaponUpgradeManager() {
        return weaponUpgradeManager;
    }

    public ArmorUpgradeManager getArmorUpgradeManager() {
        return armorUpgradeManager;
    }

    public PlayerDeathListener getPlayerDeathListener() {
        return playerDeathListener;
    }

    public myplg.myplg.listeners.HealthRegenListener getHealthRegenListener() {
        return healthRegenListener;
    }

    public myplg.myplg.listeners.NametagVisibilityListener getNametagVisibilityListener() {
        return nametagVisibilityListener;
    }

    private void loadLobbyWorld() {
        // Check if lobby world folder exists
        java.io.File lobbyFolder = new java.io.File(Bukkit.getWorldContainer(), "lobby");

        if (!lobbyFolder.exists()) {
            getLogger().warning("Lobbyワールドフォルダが見つかりません: " + lobbyFolder.getAbsolutePath());
            getLogger().warning("サーバーディレクトリに 'lobby' フォルダを配置してください。");
            return;
        }

        // Load lobby world
        getLogger().info("Lobbyワールドをロード中...");
        org.bukkit.WorldCreator worldCreator = new org.bukkit.WorldCreator("lobby");
        org.bukkit.World lobbyWorld = Bukkit.createWorld(worldCreator);

        if (lobbyWorld != null) {
            getLogger().info("Lobbyワールドのロードに成功しました: " + lobbyWorld.getName());

            // Set spawn location
            lobbyWorld.setSpawnLocation(-210, 7, 15);
            getLogger().info("Lobbyスポーン地点を設定: -210, 7, 15");
        } else {
            getLogger().severe("Lobbyワールドのロードに失敗しました！");
        }
    }

    private void loadGameWorld() {
        // Check if world folder exists
        java.io.File worldFolder = new java.io.File(Bukkit.getWorldContainer(), "world");

        if (!worldFolder.exists()) {
            getLogger().warning("ゲームワールドフォルダが見つかりません: " + worldFolder.getAbsolutePath());
            getLogger().warning("サーバーディレクトリに 'world' フォルダを配置してください。");
            return;
        }

        // Load game world
        getLogger().info("ゲームワールドをロード中...");
        org.bukkit.WorldCreator worldCreator = new org.bukkit.WorldCreator("world");
        org.bukkit.World gameWorld = Bukkit.createWorld(worldCreator);

        if (gameWorld != null) {
            getLogger().info("ゲームワールドのロードに成功しました: " + gameWorld.getName());
        } else {
            getLogger().severe("ゲームワールドのロードに失敗しました！");
        }
    }
}
