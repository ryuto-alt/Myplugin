package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from dropping valuable items while in combat
 * This prevents griefing by throwing items away before dying
 */
public class CombatItemDropListener implements Listener {

    private final PvPGame plugin;

    public CombatItemDropListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        // Only apply during game
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        Player player = event.getPlayer();

        // Check if player is in combat
        if (!plugin.getPlayerDeathListener().isInCombat(player.getUniqueId())) {
            return;
        }

        // Check if dropped item is valuable (resources)
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        Material type = droppedItem.getType();

        if (isValuableItem(type)) {
            // Cancel the drop
            event.setCancelled(true);
            player.sendMessage("§c戦闘中はアイテムを捨てられません！");
        }
    }

    /**
     * Check if an item is valuable (should be protected during combat)
     */
    private boolean isValuableItem(Material type) {
        switch (type) {
            // Resources
            case IRON_INGOT:
            case GOLD_INGOT:
            case DIAMOND:
            case EMERALD:
            // Tools
            case WOODEN_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case DIAMOND_AXE:
            case WOODEN_PICKAXE:
            case STONE_PICKAXE:
            case IRON_PICKAXE:
            case DIAMOND_PICKAXE:
            // Weapons
            case WOODEN_SWORD:
            case STONE_SWORD:
            case IRON_SWORD:
            case DIAMOND_SWORD:
            case BOW:
            case CROSSBOW:
            // Armor
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            // Other valuable items
            case ENDER_PEARL:
            case FIREWORK_ROCKET:
            case TNT:
            case ARROW:
                return true;
            default:
                return false;
        }
    }
}
