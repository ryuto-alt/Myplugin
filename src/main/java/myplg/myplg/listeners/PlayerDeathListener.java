package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerDeathListener implements Listener {
    private final PvPGame plugin;
    private final Set<UUID> respawningPlayers;

    public PlayerDeathListener(PvPGame plugin) {
        this.plugin = plugin;
        this.respawningPlayers = new HashSet<>();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Mark player as respawning
        respawningPlayers.add(playerUUID);

        // Schedule respawn after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isDead()) {
                    // Force respawn
                    player.spigot().respawn();
                }
            }
        }.runTaskLater(plugin, 100L); // 5 seconds = 100 ticks
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!respawningPlayers.contains(playerUUID)) {
            return;
        }

        if (!plugin.getGameManager().isGameRunning()) {
            respawningPlayers.remove(playerUUID);
            return;
        }

        // Set to spectator immediately
        player.setGameMode(GameMode.SPECTATOR);

        // Get team spawn location
        String teamName = plugin.getGameManager().getPlayerTeam(playerUUID);
        if (teamName != null) {
            // Schedule teleport and mode change after 5 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Teleport to team spawn
                        plugin.getGameManager().teleportPlayerToTeamSpawn(player);
                        respawningPlayers.remove(playerUUID);
                    }
                }
            }.runTaskLater(plugin, 100L); // 5 seconds = 100 ticks

            // Override respawn location to team spawn
            event.setRespawnLocation(plugin.getGameManager().getTeam(teamName).getSpawnLocation());
        } else {
            respawningPlayers.remove(playerUUID);
        }
    }
}
