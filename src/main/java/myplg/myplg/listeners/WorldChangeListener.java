package myplg.myplg.listeners;

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
        String newWorldName = player.getWorld().getName();

        // If player moved to lobby, start lobby music
        if (newWorldName.equalsIgnoreCase("lobby")) {
            // Stop lobby music first, then restart after delay
            plugin.getMusicManager().stopLobbyMusic(player);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Only start if still in lobby and not in game
                if (plugin.getMusicManager().isInLobby(player) && !plugin.getGameManager().isGameRunning()) {
                    plugin.getMusicManager().startLobbyMusic(player);
                }
            }, 20L); // 1 second delay
        }
        // If player moved to game world (not lobby), set to adventure mode
        else if (newWorldName.equalsIgnoreCase("world")) {
            // Set to adventure mode if game is not running
            if (!plugin.getGameManager().isGameRunning()) {
                player.setGameMode(GameMode.ADVENTURE);
                plugin.getLogger().info("Set " + player.getName() + " to Adventure mode in game world");

                // Open game mode selector GUI if player is OP
                if (player.isOp()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Only open if still in game world and game is not running
                        if (player.getWorld().getName().equalsIgnoreCase("world") && !plugin.getGameManager().isGameRunning()) {
                            plugin.getGameModeSelector().openGameModeSelector(player);
                        }
                    }, 10L); // 0.5 second delay
                }
            }

            // Stop lobby music
            plugin.getMusicManager().stopLobbyMusic(player);
        }
        else {
            // If player left lobby, stop lobby music
            plugin.getMusicManager().stopLobbyMusic(player);
        }
    }
}
