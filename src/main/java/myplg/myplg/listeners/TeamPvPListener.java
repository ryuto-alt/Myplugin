package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Prevents PvP between players on the same team
 */
public class TeamPvPListener implements Listener {
    private final PvPGame plugin;

    public TeamPvPListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Only handle player vs player damage
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        // Only during active game
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Get team names
        String victimTeam = plugin.getGameManager().getPlayerTeam(victim.getUniqueId());
        String attackerTeam = plugin.getGameManager().getPlayerTeam(attacker.getUniqueId());

        // If both are on the same team, cancel damage
        if (victimTeam != null && attackerTeam != null && victimTeam.equals(attackerTeam)) {
            event.setCancelled(true);
            // Optional: notify attacker
            // attacker.sendMessage("§c味方を攻撃できません！");
        }
    }
}
