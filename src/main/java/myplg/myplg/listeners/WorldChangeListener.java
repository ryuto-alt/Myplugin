package myplg.myplg.listeners;

import myplg.myplg.PermissionUtil;
import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {
    private final PvPGame plugin;

    public WorldChangeListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorldName = event.getFrom().getName();
        String toWorldName = player.getWorld().getName();

        // Notify music manager of world change
        plugin.getMusicManager().onPlayerChangeWorld(player, fromWorldName, toWorldName);

        // If player moved to lobby
        if (toWorldName.equalsIgnoreCase("lobby")) {
            // Additional lobby setup can go here if needed
        }
        // If player moved to game world (not lobby), set to adventure mode
        else if (toWorldName.equalsIgnoreCase("world")) {
            // Set to adventure mode if game is not running
            if (!plugin.getGameManager().isGameRunning()) {
                player.setGameMode(GameMode.ADVENTURE);
                plugin.getLogger().info("Set " + player.getName() + " to Adventure mode in game world");

                // Give game manager book to Admin players
                if (PermissionUtil.isOpLevel4(player)) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Only give book if still in game world and game is not running
                        if (player.getWorld().getName().equalsIgnoreCase("world") && !plugin.getGameManager().isGameRunning()) {
                            plugin.getGameManagerBook().giveBook(player);
                        }
                    }, 10L); // 0.5 second delay
                }
            }
        }
    }
}
