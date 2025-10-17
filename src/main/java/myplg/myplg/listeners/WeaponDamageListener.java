package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Adjusts weapon damage based on material type
 * Wood sword: base damage
 * Stone sword: 1.1x
 * Iron sword: 1.2x
 * Diamond sword: 1.25x
 * Netherite sword: 1.3x
 */
public class WeaponDamageListener implements Listener {
    private final PvPGame plugin;
    private static final double WOOD_MULTIPLIER = 1.0;
    private static final double STONE_MULTIPLIER = 1.1;
    private static final double IRON_MULTIPLIER = 1.2;
    private static final double DIAMOND_MULTIPLIER = 1.25;
    private static final double NETHERITE_MULTIPLIER = 1.3;
    private static final double BASE_DAMAGE = 5.0; // 2.5 hearts

    public WeaponDamageListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        if (weapon == null || !isSword(weapon.getType())) {
            return;
        }

        double multiplier = getDamageMultiplier(weapon.getType());
        double newDamage = BASE_DAMAGE * multiplier;
        event.setDamage(newDamage);

        // Reduce knockback for all swords
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();

            // Schedule velocity reduction to run after the default knockback is applied
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Vector velocity = victim.getVelocity();
                // Reduce knockback to 30% of original (70% reduction)
                velocity.multiply(0.3);
                victim.setVelocity(velocity);
            }, 1L);
        }
    }

    private boolean isSword(Material material) {
        return material.toString().endsWith("_SWORD");
    }

    private double getDamageMultiplier(Material material) {
        switch (material) {
            case WOODEN_SWORD:
                return WOOD_MULTIPLIER;
            case STONE_SWORD:
                return STONE_MULTIPLIER;
            case IRON_SWORD:
                return IRON_MULTIPLIER;
            case DIAMOND_SWORD:
                return DIAMOND_MULTIPLIER;
            case NETHERITE_SWORD:
                return NETHERITE_MULTIPLIER;
            default:
                return WOOD_MULTIPLIER;
        }
    }
}
