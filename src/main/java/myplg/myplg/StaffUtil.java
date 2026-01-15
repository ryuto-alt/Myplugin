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
 * Utility class to check staff status
 * Reads from lobby plugin's staff.yml file
 */
public class StaffUtil {

    private static Set<UUID> cachedStaff = new HashSet<>();
    private static long lastLoadTime = 0;
    private static final long CACHE_DURATION_MS = 5000; // 5 seconds cache

    // Staff prefix with yellow color (name also yellow)
    public static final String STAFF_PREFIX = ChatColor.YELLOW + "[Staff] ";

    /**
     * Check if a player has staff title
     */
    public static boolean isStaff(UUID playerUUID) {
        refreshCacheIfNeeded();
        return cachedStaff.contains(playerUUID);
    }

    /**
     * Check if a player has staff title
     */
    public static boolean isStaff(Player player) {
        return isStaff(player.getUniqueId());
    }

    /**
     * Force reload staff data from file
     */
    public static void reloadStaff() {
        loadStaffFromFile();
    }

    /**
     * Refresh cache if it's stale
     */
    private static void refreshCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLoadTime > CACHE_DURATION_MS) {
            loadStaffFromFile();
        }
    }

    /**
     * Load staff from lobby plugin's staff.yml
     */
    private static void loadStaffFromFile() {
        cachedStaff.clear();

        // Path to lobby plugin's staff.yml
        File staffFile = new File(Bukkit.getPluginsFolder(), "lobby/staff.yml");

        if (!staffFile.exists()) {
            Bukkit.getLogger().fine("staff.yml not found at: " + staffFile.getAbsolutePath());
            lastLoadTime = System.currentTimeMillis();
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(staffFile);
            if (config.contains("staff")) {
                for (String uuidString : config.getStringList("staff")) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        cachedStaff.add(uuid);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning("Invalid UUID in staff.yml: " + uuidString);
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to load staff.yml: " + e.getMessage());
        }

        lastLoadTime = System.currentTimeMillis();
    }

    /**
     * Get cached staff set
     */
    public static Set<UUID> getStaffMembers() {
        refreshCacheIfNeeded();
        return new HashSet<>(cachedStaff);
    }
}
