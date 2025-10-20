package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
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
        } else {
            // If player left lobby, stop lobby music
            plugin.getMusicManager().stopLobbyMusic(player);
        }
    }
}
