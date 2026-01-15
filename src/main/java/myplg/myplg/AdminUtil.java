package myplg.myplg;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class to check admin status
 * Reads from lobby plugin's admins.yml file
 */
public class AdminUtil {

    private static Set<UUID> cachedAdmins = new HashSet<>();
    private static long lastLoadTime = 0;
    private static final long CACHE_DURATION_MS = 5000; // 5 seconds cache

    // Admin prefix with purple color (name also purple)
    public static final String ADMIN_PREFIX = ChatColor.LIGHT_PURPLE + "[Admin] ";

    /**
     * Check if a player has admin title
     */
    public static boolean isAdmin(UUID playerUUID) {
        refreshCacheIfNeeded();
        return cachedAdmins.contains(playerUUID);
    }

    /**
     * Check if a player has admin title
     */
    public static boolean isAdmin(Player player) {
        return isAdmin(player.getUniqueId());
    }

    /**
     * Force reload admin data from file
     */
    public static void reloadAdmins() {
        loadAdminsFromFile();
    }

    /**
     * Refresh cache if it's stale
     */
    private static void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLoadTime > CACHE_DURATION_MS) {
            loadAdminsFromFile();
        }
    }

    /**
     * Load admins from lobby plugin's admins.yml
     */
    private static void loadAdminsFromFile() {
        cachedAdmins.clear();

        // Path to lobby plugin's admins.yml
        File adminFile = new File(Bukkit.getPluginsFolder(), "lobby/admins.yml");

        if (!adminFile.exists()) {
            Bukkit.getLogger().fine("admins.yml not found at: " + adminFile.getAbsolutePath());
            lastLoadTime = System.currentTimeMillis();
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(adminFile);
            if (config.contains("admins")) {
                for (String uuidString : config.getStringList("admins")) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        cachedAdmins.add(uuid);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("Invalid UUID in admins.yml: " + uuidString);
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to load admins.yml: " + e.getMessage());
        }

        lastLoadTime = System.currentTimeMillis();
    }

    /**
     * Get cached admin set
     */
    public static Set<UUID> getAdmins() {
        refreshCacheIfNeeded();
        return new HashSet<>(cachedAdmins);
    }
}
