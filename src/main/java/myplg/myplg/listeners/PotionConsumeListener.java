package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles potion consumption - removes glass bottle after drinking
 */
public class PotionConsumeListener implements Listener {
    private final PvPGame plugin;

    public PotionConsumeListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        
        // Check if item is a potion
        if (item.getType() == Material.POTION) {
            Player player = event.getPlayer();
            
            // Schedule removal of glass bottle on next tick
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Remove glass bottle from inventory
                player.getInventory().remove(Material.GLASS_BOTTLE);
            }, 1L);
        }
    }
}
