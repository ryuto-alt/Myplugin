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

        // Get the main world (or you can specify a specific world)
        World world = Bukkit.getWorlds().get(0);

        // Teleport to 0, 143, 0
        Location spawnLocation = new Location(world, 0.5, 143, 0.5);
        player.teleport(spawnLocation);
    }
}
