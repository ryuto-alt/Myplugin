package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VoidDeathListener implements Listener {
    private final PvPGame plugin;
    private final Set<UUID> processingPlayers; // Prevent duplicate processing

    public VoidDeathListener(PvPGame plugin) {
        this.plugin = plugin;
        this.processingPlayers = new HashSet<>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if player fell below Y = -11
        if (player.getLocation().getY() <= -11) {
            // Prevent duplicate processing
            if (processingPlayers.contains(playerUUID)) {
                return;
            }
            processingPlayers.add(playerUUID);

            // Trigger instant death with fake damage event
            EntityDamageEvent fakeEvent = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.VOID, 1000.0);
            plugin.getServer().getPluginManager().callEvent(fakeEvent);

            // Remove from processing
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                processingPlayers.remove(playerUUID);
            }, 120L); // Clear after 6 seconds
        }
    }
}
