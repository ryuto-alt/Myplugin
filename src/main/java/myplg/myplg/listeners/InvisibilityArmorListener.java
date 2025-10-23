package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hides all armor when player has invisibility potion effect
 * Restores armor when invisibility effect ends
 */
public class InvisibilityArmorListener implements Listener {
    private final PvPGame plugin;
    // Store armor for players with invisibility
    private final Map<UUID, ItemStack[]> storedArmor = new HashMap<>();

    public InvisibilityArmorListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Check if this is invisibility effect
        if (event.getModifiedType() != PotionEffectType.INVISIBILITY) {
            return;
        }

        // Effect added
        if (event.getAction() == EntityPotionEffectEvent.Action.ADDED) {
            hideArmor(player);
        }
        // Effect removed or expired
        else if (event.getAction() == EntityPotionEffectEvent.Action.REMOVED ||
                 event.getAction() == EntityPotionEffectEvent.Action.CLEARED) {
            restoreArmor(player);
        }
    }

    /**
     * Hide player's armor
     */
    private void hideArmor(Player player) {
        UUID playerId = player.getUniqueId();

        // Store current armor
        ItemStack[] armorContents = player.getInventory().getArmorContents();

        // Only store if player has armor
        boolean hasArmor = false;
        for (ItemStack item : armorContents) {
            if (item != null) {
                hasArmor = true;
                break;
            }
        }

        if (hasArmor) {
            storedArmor.put(playerId, armorContents.clone());

            // Clear armor (make invisible)
            player.getInventory().setArmorContents(new ItemStack[4]);

            plugin.getLogger().info("Hid armor for " + player.getName() + " (invisibility active)");
        }
    }

    /**
     * Restore player's armor
     */
    private void restoreArmor(Player player) {
        UUID playerId = player.getUniqueId();

        // Restore armor if it was stored
        if (storedArmor.containsKey(playerId)) {
            ItemStack[] armorToRestore = storedArmor.remove(playerId);

            // Delay restoration by 1 tick to ensure effect is fully removed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Only restore if player is still online
                if (player.isOnline()) {
                    player.getInventory().setArmorContents(armorToRestore);
                    plugin.getLogger().info("Restored armor for " + player.getName() + " (invisibility ended)");
                }
            }, 1L);
        }
    }

    /**
     * Clear stored armor for a player (called on death or logout)
     */
    public void clearStoredArmor(Player player) {
        storedArmor.remove(player.getUniqueId());
    }

    /**
     * Clear all stored armor (called on plugin disable)
     */
    public void clearAllStoredArmor() {
        storedArmor.clear();
    }
}
