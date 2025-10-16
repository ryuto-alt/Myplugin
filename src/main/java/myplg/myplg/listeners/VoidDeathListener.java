package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class VoidDeathListener implements Listener {
    private final PvPGame plugin;

    public VoidDeathListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // Check if player fell below Y = -11
        if (player.getLocation().getY() <= -11) {
            // Kill the player (triggers death event)
            player.setHealth(0);
        }
    }
}
