package myplg.myplg.listeners;

import myplg.myplg.PvPGame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class ArmorRemoveListener implements Listener {
    private final PvPGame plugin;

    public ArmorRemoveListener(PvPGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Check if game is running
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Check if player is clicking armor slots
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && isArmor(clickedItem.getType())) {
                event.setCancelled(true);
            }
        }

        // Check if player is shift-clicking armor
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            ItemStack item = event.getCurrentItem();
            if (isArmor(item.getType())) {
                event.setCancelled(true);
            }
        }

        // Check if player is clicking their own armor slot
        if (event.getClick().isShiftClick() && event.getClickedInventory() != null) {
            if (event.getSlot() >= 36 && event.getSlot() <= 39) { // Armor slots
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onArmorDrop(PlayerDropItemEvent event) {
        // Check if game is running
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // Prevent dropping armor
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isArmor(droppedItem.getType())) {
            event.setCancelled(true);
        }
    }

    private boolean isArmor(Material material) {
        String matName = material.toString();
        return matName.endsWith("_HELMET") ||
               matName.endsWith("_CHESTPLATE") ||
               matName.endsWith("_LEGGINGS") ||
               matName.endsWith("_BOOTS");
    }
}
