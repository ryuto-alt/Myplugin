package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final PvPGame plugin;

    public PlayerJoinListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Get lobby world
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            plugin.getLogger().warning("Lobbyワールドが見つかりません！プレイヤー: " + player.getName());
            return;
        }

        // Teleport to lobby spawn: -210, 7, 15
        Location lobbySpawn = new Location(lobbyWorld, -210.5, 7, 15.5);
        player.teleport(lobbySpawn);

        plugin.getLogger().info(player.getName() + " をLobbyにテレポートしました: -210, 7, 15");

        // Start lobby music for the player
        plugin.getMusicManager().onPlayerJoin(player);
    }
}
