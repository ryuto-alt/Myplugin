package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controls health regeneration to match 1.8 behavior with slower regeneration
 */
public class HealthRegenListener implements Listener {
    private final PvPGame plugin;
    private final Map<UUID, Integer> regenTasks = new HashMap<>();
    private static final int REGEN_INTERVAL = 60; // 3 seconds (60 ticks)
    private static final double REGEN_AMOUNT = 2.0; // 1 heart

    public HealthRegenListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Cancel natural regeneration - we'll handle it manually
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Start regen task immediately if game is running
        // During game, hunger is always kept at 20 by HungerControlListener
        if (plugin.getGameManager().isGameRunning()) {
            // Delay slightly to ensure player is fully loaded
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    startRegenTask(player);
                }
            }, 20L); // 1 second delay
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopRegenTask(event.getPlayer());
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = (Player) event.getEntity();

        // During game, hunger is always kept at 20 by HungerControlListener
        // So we only need to start regen task if not already running
        // NEVER stop the regen task during game
        if (!regenTasks.containsKey(player.getUniqueId())) {
            startRegenTask(player);
        }
    }

    private void startRegenTask(Player player) {
        // Don't start if already running
        if (regenTasks.containsKey(player.getUniqueId())) {
            return;
        }

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !plugin.getGameManager().isGameRunning()) {
                stopRegenTask(player);
                return;
            }

            // During game, hunger is always at 20, so no need to check
            // Just heal the player continuously

            // Heal player by 2.0 (1.0 hearts) if not at max health
            try {
                Attribute maxHealthAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
                double maxHealth = player.getAttribute(maxHealthAttr).getValue();
                double currentHealth = player.getHealth();

                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(currentHealth + REGEN_AMOUNT, maxHealth);
                    player.setHealth(newHealth);
                }
            } catch (Exception e) {
                // Fallback if attribute not available
                if (player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(Math.min(player.getHealth() + REGEN_AMOUNT, player.getMaxHealth()));
                }
            }
        }, REGEN_INTERVAL, REGEN_INTERVAL).getTaskId();

        regenTasks.put(player.getUniqueId(), taskId);
    }

    private void stopRegenTask(Player player) {
        Integer taskId = regenTasks.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    // Clean up tasks when plugin disables
    public void cleanup() {
        for (Integer taskId : regenTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        regenTasks.clear();
    }

    // Start regeneration for all online players (called when game starts)
    public void startAllRegenTasks() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // During game, hunger is always kept at 20, so just start regen for everyone
            startRegenTask(player);
        }
    }

    // Stop all regeneration tasks (called when game ends)
    public void stopAllRegenTasks() {
        for (UUID uuid : new HashMap<>(regenTasks).keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                stopRegenTask(player);
            }
        }
    }
}
