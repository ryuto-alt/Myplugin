package myplg.myplg.data;

import myplg.myplg.PvPGame;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopDataManager {
    private final PvPGame plugin;
    private File shopFile;
    private FileConfiguration shopConfig;

    public ShopDataManager(PvPGame plugin) {
        this.plugin = plugin;
        setupShopFile();
    }

    private void setupShopFile() {
        shopFile = new File(plugin.getDataFolder(), "shops.yml");
        if (!shopFile.exists()) {
            try {
                shopFile.getParentFile().mkdirs();
                shopFile.createNewFile();
                plugin.getLogger().info("Created new shops.yml file");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create shops.yml file: " + e.getMessage());
            }
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }

    public void saveShopVillager(String shopType, UUID entityUUID, Location location, String teamName) {
        String path = "villagers." + entityUUID.toString();
        shopConfig.set(path + ".type", shopType);
        shopConfig.set(path + ".team", teamName);
        shopConfig.set(path + ".world", location.getWorld().getName());
        shopConfig.set(path + ".x", location.getX());
        shopConfig.set(path + ".y", location.getY());
        shopConfig.set(path + ".z", location.getZ());
        shopConfig.set(path + ".yaw", location.getYaw());
        shopConfig.set(path + ".pitch", location.getPitch());
        saveConfig();

        plugin.getLogger().info("Saved shop: type=" + shopType + ", team=" + teamName +
            ", location=" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() +
            ", yaw=" + location.getYaw() + ", UUID=" + entityUUID);
    }

    public void removeShopVillager(UUID entityUUID) {
        shopConfig.set("villagers." + entityUUID.toString(), null);
        saveConfig();
    }

    public String getShopType(UUID entityUUID) {
        return shopConfig.getString("villagers." + entityUUID.toString() + ".type");
    }

    public String getShopTeam(UUID entityUUID) {
        return shopConfig.getString("villagers." + entityUUID.toString() + ".team");
    }

    public List<String> getAllShopVillagers() {
        if (shopConfig.getConfigurationSection("villagers") == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(shopConfig.getConfigurationSection("villagers").getKeys(false));
    }

    public Location getShopLocation(UUID entityUUID) {
        String path = "villagers." + entityUUID.toString();
        String worldName = shopConfig.getString(path + ".world");
        if (worldName == null) {
            return null;
        }

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = shopConfig.getDouble(path + ".x");
        double y = shopConfig.getDouble(path + ".y");
        double z = shopConfig.getDouble(path + ".z");
        float yaw = (float) shopConfig.getDouble(path + ".yaw", 0.0);
        float pitch = (float) shopConfig.getDouble(path + ".pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveConfig() {
        try {
            shopConfig.save(shopFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save shops.yml: " + e.getMessage());
        }
    }

    public void reload() {
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }
}
