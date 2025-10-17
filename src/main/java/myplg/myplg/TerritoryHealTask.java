package myplg.myplg;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Heals players in their team's territory (within 20 blocks of their bed)
 * if the team has purchased Lv I territory upgrade
 */
public class TerritoryHealTask extends BukkitRunnable {
    private final PvPGame plugin;
    private static final double HEAL_RADIUS = 20.0; // 20 blocks radius
    private static final double HEAL_AMOUNT = 1.0; // 0.5 hearts per tick (slow heal)

    public TerritoryHealTask(PvPGame plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Check all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Get player's team
            String teamName = plugin.getGameManager().getPlayerTeam(player.getUniqueId());
            if (teamName == null) {
                continue;
            }

            // Check if team has Lv I heal upgrade
            if (!plugin.getTerritoryUpgradeManager().hasUpgrade(teamName, 1)) {
                continue;
            }

            // Get team
            Team team = plugin.getGameManager().getTeam(teamName);
            if (team == null || team.getBedBlock() == null) {
                continue;
            }

            // Get bed location
            Location bedLocation = team.getBedBlock().getLocation();
            Location playerLocation = player.getLocation();

            // Check if player is in the same world as the bed
            if (!bedLocation.getWorld().equals(playerLocation.getWorld())) {
                continue;
            }

            // Check if player is within heal radius
            double distance = bedLocation.distance(playerLocation);
            if (distance <= HEAL_RADIUS) {
                // Heal player if not at max health
                double maxHealth = player.getMaxHealth();
                double currentHealth = player.getHealth();

                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(currentHealth + HEAL_AMOUNT, maxHealth);
                    player.setHealth(newHealth);
                }
            }
        }
    }
}
